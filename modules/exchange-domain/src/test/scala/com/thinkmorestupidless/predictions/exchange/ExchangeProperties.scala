package com.thinkmorestupidless.predictions.exchange

import org.scalacheck.{Gen, Prop}

import java.time.Instant

/**
 * Spec 005 SC-002 (research R11): random command sequences, and after every command the invariants
 * of the data model: every outcome's shares held are the sets outstanding; the pool is what the
 * money events put in and took out; no position goes negative and what rests under orders is what
 * the books hold; every buy's reservation, less what it released and was charged, is what still
 * rests; a decision's next value is its events replayed; the whole journal replays to the value;
 * trades and accepted orders are numbered in turn; every money amount is non-negative.
 * `-Dpredictions.sequences=<n>` sets the count (100,000).
 */
class ExchangeProperties extends munit.ScalaCheckSuite:

  private val sequences = sys.props.get("predictions.sequences").fold(100_000)(_.toInt)

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(sequences).withWorkers(1)

  override val munitTimeout = scala.concurrent.duration.Duration(30, "min")

  private val t0     = Instant.parse("2026-10-31T10:00:00Z")
  private val owners = Vector("ada", "bob", "cy").map(OwnerId(_))

  private final case class World(outcomes: Vector[OutcomeId], maxOrderCost: Long, maxPosition: Long)

  private val worlds: Gen[World] = for
    n     <- Gen.oneOf(2, 3)
    tight <- Gen.oneOf(true, false)
  yield World(
    Vector("yes", "no", "maybe").take(n).map(OutcomeId(_)),
    if tight then 2_000 else 100_000,
    if tight then 30 else 10_000
  )

  /** Quantities whose product with a price, or sum with a position, overflows a `Long`. */
  private val huge = Gen.oneOf(Long.MaxValue, Long.MaxValue / 2 + 1, Long.MaxValue / 99 + 1)

  /** A command for the `i`th step, with the time it carries. */
  private def command(w: World, i: Int): Gen[ExchangeCommand] =
    val at = t0.plusSeconds(i.toLong)
    val place = for
      id       <- Gen.choose(0, 14).map(k => OrderId(s"o-$k"))
      owner    <- Gen.oneOf(owners)
      outcome  <- Gen.oneOf(w.outcomes)
      side     <- Gen.oneOf(Side.Buy, Side.Sell)
      price    <- Gen.frequency(20 -> Gen.choose(1L, 99L), 1 -> Gen.oneOf(0L, 100L))
      quantity <- Gen.frequency(20 -> Gen.choose(1L, 20L), 1 -> Gen.const(0L), 1 -> huge)
      duration <- Gen.frequency(
        6 -> Gen.const(OrderDuration.RestUntilClose),
        2 -> Gen.const(OrderDuration.FillAtOnce),
        2 -> Gen.const(OrderDuration.FillWhole)
      )
    yield ExchangeCommand.Place(id, owner, outcome, side, price, quantity, duration, at)
    val cancel = for
      id    <- Gen.choose(0, 14).map(k => OrderId(s"o-$k"))
      owner <- Gen.oneOf(owners)
    yield ExchangeCommand.Cancel(id, owner, at)
    val sets = for
      key   <- Gen.choose(0, 5).map(k => SetKey(s"k-$k"))
      owner <- Gen.oneOf(owners)
      n     <- Gen.frequency(12 -> Gen.choose(0L, 12L), 1 -> huge)
      split <- Gen.frequency(3 -> true, 1 -> false)
    yield
      if split then ExchangeCommand.Split(key, owner, n, at)
      else ExchangeCommand.Merge(key, owner, n, at)
    val finality = Gen.oneOf(w.outcomes.map(Finality.Resolved(_)) :+ Finality.Void)
    Gen.frequency(
      12 -> place,
      3  -> cancel,
      4  -> sets,
      1  -> Gen.oneOf(ExchangeCommand.Suspend(at), ExchangeCommand.Resume(at)),
      1  -> Gen.const(ExchangeCommand.Close(at)),
      1  -> finality.map(ExchangeCommand.Finalise(_, at))
    )

  private val runs: Gen[(World, Vector[ExchangeCommand])] = for
    w      <- worlds
    length <- Gen.choose(1, 40)
    cs <- Gen.sequence[Vector[ExchangeCommand], ExchangeCommand]((1 to length).map(command(w, _)))
  yield (w, cs)

  /** What the money events say: the pool's money, and each buy's reservation still held. */
  private final case class Ledger(pool: Long = 0, reserved: Map[OrderId, Long] = Map.empty):
    def held(o: OrderId): Long = reserved.getOrElse(o, 0L)
    def apply(e: ExchangeEvent): Ledger = e match
      case p: ExchangeEvent.OrderPlaced => copy(reserved = reserved.updated(p.order, p.cost))
      case t: ExchangeEvent.TradeMade =>
        val moved = t.legs.foldLeft(reserved)((r, l) =>
          if l.side == Side.Buy then
            r.updated(l.order, r.getOrElse(l.order, 0L) - l.charge - l.released)
          else r
        )
        val pool = t.through match
          case Through.Direct => 0L
          case Through.Split  => t.legs.map(_.charge).sum
          case Through.Merge  => -t.legs.map(_.paid).sum
        Ledger(this.pool + pool, moved)
      case c: ExchangeEvent.OrderCancelled =>
        copy(reserved = reserved.updated(c.order, held(c.order) - c.released))
      case c: ExchangeEvent.OrderExpired =>
        copy(reserved = reserved.updated(c.order, held(c.order) - c.released))
      case s: ExchangeEvent.SetsSplit    => copy(pool = pool + s.charge)
      case m: ExchangeEvent.SetsMerged   => copy(pool = pool - m.paid)
      case p: ExchangeEvent.PositionPaid => copy(pool = pool - p.paid)
      case _                             => this

  private def check(
      x: Exchange,
      ledger: Ledger,
      journal: Vector[ExchangeEvent],
      clue: => String
  ): Unit =
    x.outcomes.foreach { o =>
      val held = x.positions.values.flatMap(_.get(o)).map(_.quantity).sum
      assertEquals(held, x.setsOutstanding, s"$o held against the sets: $clue")
    }
    assertEquals(x.pool, ledger.pool, s"the pool against the money: $clue")
    val resting = x.orders.values.filter(Exchange.resting)
    for
      (owner, held) <- x.positions
      (outcome, p)  <- held
    do
      def rests(side: Side) =
        resting
          .filter(o => o.owner == owner && o.outcome == outcome && o.side == side)
          .map(_.remaining)
          .sum
      assert(p.quantity >= 0 && p.restingUnderSells <= p.quantity, s"$owner/$outcome $p: $clue")
      assertEquals(p.restingUnderSells, rests(Side.Sell), s"$owner/$outcome under sells: $clue")
      assertEquals(p.restingToBuy, rests(Side.Buy), s"$owner/$outcome to buy: $clue")
    x.orders.values.filter(_.side == Side.Buy).foreach { o =>
      val expected = if Exchange.resting(o) then o.remaining * o.price else 0L
      assertEquals(ledger.held(o.id), expected, s"${o.id}'s reservation: $clue")
    }
    val trades = journal.collect { case t: ExchangeEvent.TradeMade => t.sequence }
    assertEquals(trades, (1L to trades.size.toLong).toVector, s"trade numbers: $clue")
    val accepted = journal.collect { case p: ExchangeEvent.OrderPlaced => p.sequence }
    assertEquals(accepted, (1L to accepted.size.toLong).toVector, s"order sequences: $clue")
    assertEquals(
      journal.foldLeft(Exchange.empty)(Exchange.replay),
      x,
      s"the journal's replay: $clue"
    )

  private def amounts(e: ExchangeEvent): Vector[Long] = e match
    case p: ExchangeEvent.OrderPlaced => Vector(p.cost)
    case t: ExchangeEvent.TradeMade   => t.legs.flatMap(l => Vector(l.charge, l.paid, l.released))
    case c: ExchangeEvent.OrderCancelled => Vector(c.released)
    case c: ExchangeEvent.OrderExpired   => Vector(c.released)
    case s: ExchangeEvent.SetsSplit      => Vector(s.charge)
    case m: ExchangeEvent.SetsMerged     => Vector(m.paid)
    case p: ExchangeEvent.PositionPaid   => Vector(p.paid)
    case r: ExchangeEvent.RemainderKept  => Vector(r.amount)
    case _                               => Vector.empty

  // What the sequences did, so a run that traded nothing is seen to have proved little.
  private val ran     = java.util.concurrent.atomic.AtomicInteger()
  private val counted = scala.collection.concurrent.TrieMap.empty[String, Long]

  private def count(e: ExchangeEvent): Unit =
    val name = e match
      case t: ExchangeEvent.TradeMade => s"trade ${Through.word(t.through)}"
      case other                      => other.getClass.getSimpleName
    counted.updateWith(name)(n => Some(n.getOrElse(0L) + 1)): Unit

  override def afterAll(): Unit =
    val shown = counted.toVector.sorted.map((k, v) => s"$k $v").mkString(", ")
    println(s"ExchangeProperties ran ${ran.get} sequences: $shown")

  property("no sequence of commands breaks the pool, a position, a reservation or replay") {
    Prop.forAllNoShrink(runs) { (w, commands) =>
      ran.incrementAndGet(): Unit
      val open =
        ExchangeCommand.Open(MarketId("m-1"), w.outcomes, 100, w.maxOrderCost, w.maxPosition, t0)
      val first = Exchange(Exchange.empty, open).fold(r => fail(r.toString), identity)
      commands.zipWithIndex.foldLeft((first.next, Ledger(), first.events)) {
        case ((x, ledger, journal), (c, i)) =>
          def clue = s"after command $i of ${(open +: commands).mkString("\n  ", "\n  ", "")}"
          Exchange(x, c) match
            case Left(_) => (x, ledger, journal)
            case Right(d) =>
              assertEquals(
                d.events.foldLeft(x)(Exchange.replay),
                d.next,
                s"the decision's replay: $clue"
              )
              d.events.foreach { e =>
                assert(amounts(e).forall(_ >= 0), s"$e: $clue")
                count(e)
              }
              val next = (d.next, d.events.foldLeft(ledger)(_.apply(_)), journal ++ d.events)
              check(next._1, next._2, next._3, clue)
              next
      }: Unit
    }
  }

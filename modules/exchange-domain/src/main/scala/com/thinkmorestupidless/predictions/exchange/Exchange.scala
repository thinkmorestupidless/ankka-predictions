package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.Instant
import scala.collection.immutable.SortedMap

/**
 * One market's exchange, as a value: every outcome's book, the orders it accepted, the positions,
 * the pool and the time of its last event (spec 005 FR-001). Ordered collections throughout, so
 * `==` is replay equality.
 */
final case class Exchange(
    market: MarketId = MarketId(""),
    outcomes: Vector[OutcomeId] = Vector.empty,
    unit: Long = 0,
    maxOrderCost: Long = 0,
    maxPosition: Long = 0,
    state: ExchangeState = ExchangeState.Empty,
    books: SortedMap[OutcomeId, Book] = SortedMap.empty,
    orders: SortedMap[OrderId, Order] = SortedMap.empty,
    positions: SortedMap[OwnerId, SortedMap[OutcomeId, Position]] = SortedMap.empty,
    setMoves: SortedMap[SetKey, SetMove] = SortedMap.empty,
    setsOutstanding: Long = 0,
    remainder: Long = 0,
    lastPrices: SortedMap[OutcomeId, Long] = SortedMap.empty,
    nextOrder: Long = 1,
    nextTrade: Long = 1,
    lastTime: Option[Instant] = None,
    finality: Option[Finality] = None
):

  def position(owner: OwnerId, outcome: OutcomeId): Position =
    positions.get(owner).flatMap(_.get(outcome)).getOrElse(Position())

  /** The pool: one unit per set outstanding, and the remainder a void kept (spec 005 FR-015). */
  def pool: Long = setsOutstanding * unit + remainder

  /** The price of the outcome's last trade, if it has traded (FR-020). */
  def lastPrice(outcome: OutcomeId): Option[Long] = lastPrices.get(outcome)

  /** The highest resting buy price of the outcome. */
  def bestBid(outcome: OutcomeId): Option[Long] =
    books.get(outcome).flatMap(_.buys.headOption).map(_.price)

  /** The lowest resting sell price of the outcome. */
  def bestAsk(outcome: OutcomeId): Option[Long] =
    books.get(outcome).flatMap(_.sells.headOption).map(_.price)

  /** The quantity resting at each price of the outcome's book: buys, then sells, best first. */
  def depth(outcome: OutcomeId): (Vector[Level], Vector[Level]) =
    val book = books.getOrElse(outcome, Book())
    def levels(resting: Vector[Resting]): Vector[Level] =
      resting.foldLeft(Vector.empty[Level]) { (acc, r) =>
        val left = orders.get(r.order).fold(0L)(_.remaining)
        acc.lastOption match
          case Some(l) if l.price == r.price => acc.init :+ l.copy(quantity = l.quantity + left)
          case _                             => acc :+ Level(r.price, left)
      }
    (levels(book.buys), levels(book.sells))

  private[exchange] def withPosition(owner: OwnerId, outcome: OutcomeId)(
      f: Position => Position
  ): Exchange =
    val byOutcome = positions.getOrElse(owner, SortedMap.empty[OutcomeId, Position])
    copy(positions =
      positions.updated(owner, byOutcome.updated(outcome, f(position(owner, outcome))))
    )

  private[exchange] def withOrder(id: OrderId)(f: Order => Order): Exchange =
    orders.get(id).fold(this)(o => copy(orders = orders.updated(id, f(o))))

  private[exchange] def withoutResting(id: OrderId): Exchange =
    orders.get(id).fold(this) { o =>
      copy(books = books.updatedWith(o.outcome)(_.map(_.remove(id))))
    }

object Exchange:

  val empty: Exchange = Exchange()

  /** What a buy of `quantity` at `price` costs, and what the consumer reserves for it. */
  def cost(quantity: Long, price: Long): Long = quantity * price

  /** Snapshots, under the manifest `exchange`; the events' serializer is in their companion. */
  given Serializer[Exchange] = Codecs.serializer[Exchange]("exchange")

  /**
   * A command applied to an exchange: a refusal with no change, or the next value and its events,
   * where the next value is the events replayed onto this one (spec 005 FR-002).
   */
  def apply(exchange: Exchange, command: ExchangeCommand): Either[Refusal, Decision] =
    val stamped = atTheBook(exchange, command)
    exchange.lastTime.filter(stamped.at.isBefore) match
      case Some(last) => Left(Refusals.outOfOrder(last, stamped.at))
      case None =>
        stamped match
          case c: ExchangeCommand.Open     => open(exchange, c)
          case c: ExchangeCommand.Place    => place(exchange, c)
          case c: ExchangeCommand.Cancel   => cancel(exchange, c)
          case c: ExchangeCommand.Split    => split(exchange, c)
          case c: ExchangeCommand.Merge    => merge(exchange, c)
          case c: ExchangeCommand.Suspend  => suspend(exchange, c)
          case c: ExchangeCommand.Resume   => resume(exchange, c)
          case c: ExchangeCommand.Close    => close(exchange, c)
          case c: ExchangeCommand.Finalise => finalise(exchange, c)

  /**
   * An owner's command (place, cancel, split, merge) that reaches the book behind a later event is
   * applied at the book's last time: its time is when it was asked, and owners' commands on one
   * book cross on their way to it (a placement is gated and funded first). A market's command
   * (open, suspend, resume, close, finalise) keeps its own time and is refused when it is older,
   * which is how a redelivered one is told apart (FR-002).
   */
  private def atTheBook(exchange: Exchange, command: ExchangeCommand): ExchangeCommand =
    def at(t: Instant) = exchange.lastTime.filter(t.isBefore).getOrElse(t)
    command match
      case c: ExchangeCommand.Place  => c.copy(at = at(c.at))
      case c: ExchangeCommand.Cancel => c.copy(at = at(c.at))
      case c: ExchangeCommand.Split  => c.copy(at = at(c.at))
      case c: ExchangeCommand.Merge  => c.copy(at = at(c.at))
      case market                    => market

  private def open(exchange: Exchange, c: ExchangeCommand.Open): Either[Refusal, Decision] =
    if exchange.state != ExchangeState.Empty then Left(Refusals.state(exchange.state))
    else
      decide(
        exchange,
        Vector(
          ExchangeEvent.Opened(c.market, c.outcomes, c.unit, c.maxOrderCost, c.maxPosition, c.at)
        )
      )

  /** The answer to a command already applied under its id: the first, with no event. */
  private def repeated(exchange: Exchange): Either[Refusal, Decision] =
    Right(Decision(exchange, Vector.empty))

  /**
   * An order (FR-004 to FR-012). A known id is answered first, so a repeat after the exchange moved
   * on is still the first answer (research R5); then the state, the fields and the limits; then the
   * matcher, which may refuse a fill-whole order.
   */
  private def place(x: Exchange, c: ExchangeCommand.Place): Either[Refusal, Decision] =
    x.orders.get(c.order) match
      case Some(o) =>
        val same = o.owner == c.owner && o.outcome == c.outcome && o.side == c.side &&
          o.price == c.price && o.quantity == c.quantity && o.duration == c.duration
        if same then repeated(x) else Left(Refusals.id(c.order))
      case None =>
        val held = x.position(c.owner, c.outcome)
        if x.state != ExchangeState.Open then Left(Refusals.state(x.state))
        else if !x.outcomes.contains(c.outcome) then Left(Refusals.outcome(c.outcome))
        else if c.price < 1 || c.price >= x.unit then Left(Refusals.price(c.price))
        else if c.quantity < 1 then Left(Refusals.quantity(c.quantity.toString))
        // Divided and subtracted, not multiplied and added: a quantity near `Long.MaxValue` would
        // wrap a product or a sum below the limit and be accepted with a negative cost.
        else if c.side == Side.Buy && c.quantity > x.maxOrderCost / c.price then
          Left(Refusals.limit(Refusals.maximumOrderCost))
        else if c.side == Side.Buy &&
          c.quantity > x.maxPosition - held.quantity - held.restingToBuy
        then Left(Refusals.limit(Refusals.maximumPosition))
        else if c.side == Side.Sell && c.quantity > held.quantity - held.restingUnderSells then
          Left(Refusals.quantityHeld(held.quantity - held.restingUnderSells))
        else
          val cost = if c.side == Side.Buy then Exchange.cost(c.quantity, c.price) else 0L
          val placed = ExchangeEvent.OrderPlaced(
            c.order,
            c.owner,
            c.outcome,
            c.side,
            c.price,
            c.quantity,
            c.duration,
            cost,
            x.nextOrder,
            c.at
          )
          Matching
            .place(replay(x, placed), c.order, c.at)
            .flatMap(rest => decide(x, placed +: rest))

  /**
   * A cancel by the order's owner of an order still resting (FR-006); a buy releases what is left.
   */
  private def cancel(x: Exchange, c: ExchangeCommand.Cancel): Either[Refusal, Decision] =
    x.orders.get(c.order) match
      case None                          => Left(Refusals.id(c.order))
      case Some(o) if o.owner != c.owner => Left(Refusals.owner(c.owner))
      case Some(o) if !resting(o)        => Left(Refusals.orderState(o.state))
      case Some(o) =>
        val (released, ordinal) = release(o)
        decide(x, Vector(ExchangeEvent.OrderCancelled(o.id, o.owner, released, ordinal, c.at)))

  /** Whether an order is in its book. */
  private[exchange] def resting(o: Order): Boolean =
    o.state == OrderState.Resting || o.state == OrderState.FilledInPart

  /**
   * What ending an order gives back: a buy's remaining cost under its next ordinal; a sell,
   * nothing.
   */
  private[exchange] def release(o: Order): (Long, Int) =
    if o.side == Side.Buy && o.remaining > 0 then (o.remaining * o.price, o.releases + 1)
    else (0L, 0)

  /** A known set key: the same split or merge is the first answer, another is refused (FR-007). */
  private def known(x: Exchange, key: SetKey, kind: SetKind, owner: OwnerId, sets: Long) =
    x.setMoves.get(key).map { m =>
      if m.kind == kind && m.owner == owner && m.sets == sets then repeated(x)
      else Left(Refusals.key(key))
    }

  private def split(x: Exchange, c: ExchangeCommand.Split): Either[Refusal, Decision] =
    known(x, c.key, SetKind.Split, c.owner, c.sets).getOrElse {
      if x.state != ExchangeState.Open then Left(Refusals.state(x.state))
      else if c.sets < 1 then Left(Refusals.quantity(c.sets.toString))
      else if x.outcomes.exists { o =>
          val p = x.position(c.owner, o)
          c.sets > x.maxPosition - p.quantity - p.restingToBuy
        }
      then Left(Refusals.limit(Refusals.maximumPosition))
      else
        decide(
          x,
          Vector(ExchangeEvent.SetsSplit(c.key, c.owner, c.sets, c.sets * x.unit, c.at))
        )
    }

  private def merge(x: Exchange, c: ExchangeCommand.Merge): Either[Refusal, Decision] =
    known(x, c.key, SetKind.Merge, c.owner, c.sets).getOrElse {
      lazy val free = x.outcomes
        .map { o =>
          val p = x.position(c.owner, o)
          p.quantity - p.restingUnderSells
        }
        .minOption
        .getOrElse(0L)
      if x.state != ExchangeState.Open then Left(Refusals.state(x.state))
      else if c.sets < 1 then Left(Refusals.quantity(c.sets.toString))
      else if free < c.sets then Left(Refusals.quantityHeld(free))
      else
        decide(
          x,
          Vector(ExchangeEvent.SetsMerged(c.key, c.owner, c.sets, c.sets * x.unit, c.at))
        )
    }

  private def suspend(x: Exchange, c: ExchangeCommand.Suspend): Either[Refusal, Decision] =
    if x.state != ExchangeState.Open then Left(Refusals.state(x.state))
    else decide(x, Vector(ExchangeEvent.Suspended(c.at)))

  private def resume(x: Exchange, c: ExchangeCommand.Resume): Either[Refusal, Decision] =
    if x.state != ExchangeState.Suspended then Left(Refusals.state(x.state))
    else decide(x, Vector(ExchangeEvent.Resumed(c.at)))

  /** Every resting order expires, in the books' order, then the exchange is closed (FR-016). */
  private def close(x: Exchange, c: ExchangeCommand.Close): Either[Refusal, Decision] =
    x.state match
      case ExchangeState.Closed                      => repeated(x)
      case ExchangeState.Empty | ExchangeState.Final => Left(Refusals.state(x.state))
      case _ =>
        val expired = for
          book    <- x.books.values.toVector
          resting <- book.buys ++ book.sells
          order   <- x.orders.get(resting.order).toVector
        yield
          val (released, ordinal) = release(order)
          ExchangeEvent.OrderExpired(order.id, order.owner, released, ordinal, c.at)
        decide(x, expired :+ ExchangeEvent.Closed(c.at))

  /**
   * Every position with shares is paid once, a losing one nothing (research R6); a void keeps what
   * one unit does not divide into equal parts (FR-017, research R7).
   */
  private def finalise(x: Exchange, c: ExchangeCommand.Finalise): Either[Refusal, Decision] =
    x.state match
      case ExchangeState.Final if x.finality.contains(c.finality) => repeated(x)
      case ExchangeState.Closed =>
        c.finality match
          case Finality.Resolved(o) if !x.outcomes.contains(o) => Left(Refusals.outcome(o))
          case finality =>
            val part = if x.outcomes.isEmpty then 0L else x.unit / x.outcomes.size
            val paid = for
              (owner, held)  <- x.positions.toVector
              (outcome, pos) <- held.toVector if pos.quantity > 0
            yield
              val amount = finality match
                case Finality.Resolved(won) => if outcome == won then pos.quantity * x.unit else 0L
                case Finality.Void          => pos.quantity * part
              ExchangeEvent.PositionPaid(
                PositionKey.of(owner, outcome),
                owner,
                outcome,
                pos.quantity,
                amount,
                c.at
              )
            val left = finality match
              case Finality.Void => x.setsOutstanding * (x.unit - part * x.outcomes.size)
              case _             => 0L
            val kept = Option.when(left > 0)(ExchangeEvent.RemainderKept(left, c.at)).toVector
            decide(x, paid ++ kept :+ ExchangeEvent.Final(finality, c.at))
      case _ => Left(Refusals.state(x.state))

  private def decide(exchange: Exchange, events: Vector[ExchangeEvent]): Either[Refusal, Decision] =
    Right(Decision(events.foldLeft(exchange)(replay), events))

  /** An event applied to an exchange: the next value. Total over every case. */
  def replay(exchange: Exchange, event: ExchangeEvent): Exchange =
    import ExchangeEvent.*
    val next = event match
      case e: Opened =>
        exchange.copy(
          market = e.market,
          outcomes = e.outcomes,
          unit = e.unit,
          maxOrderCost = e.maxOrderCost,
          maxPosition = e.maxPosition,
          state = ExchangeState.Open,
          books = SortedMap.from(e.outcomes.map(_ -> Book()))
        )

      case e: OrderPlaced =>
        val order = Order(
          e.order,
          e.owner,
          e.outcome,
          e.side,
          e.price,
          e.quantity,
          e.quantity,
          e.duration,
          OrderState.Resting,
          e.cost,
          e.sequence,
          releases = 0
        )
        val placed = exchange.copy(
          orders = exchange.orders.updated(e.order, order),
          books = exchange.books.updatedWith(e.outcome)(
            _.map(_.rest(e.side, Resting(e.order, e.price, e.sequence)))
          ),
          nextOrder = e.sequence + 1
        )
        placed.withPosition(e.owner, e.outcome)(p =>
          if e.side == Side.Sell then p.copy(restingUnderSells = p.restingUnderSells + e.quantity)
          else p.copy(restingToBuy = p.restingToBuy + e.quantity)
        )

      case e: TradeMade =>
        val filled = e.legs.foldLeft(exchange) { (x, leg) =>
          x.withOrder(leg.order)(o =>
            o.copy(
              remaining = o.remaining - e.quantity,
              state = OrderState.FilledInPart,
              releases = if leg.released > 0 then leg.releaseOrdinal else o.releases
            )
          )
        }
        val traded = moved(filled, e)
        val sets = e.through match
          case Through.Direct => traded.setsOutstanding
          case Through.Split  => traded.setsOutstanding + e.quantity
          case Through.Merge  => traded.setsOutstanding - e.quantity
        traded.copy(
          setsOutstanding = sets,
          lastPrices = traded.lastPrices ++ e.prices,
          nextTrade = e.sequence + 1
        )

      case e: OrderFilled =>
        exchange.withoutResting(e.order).withOrder(e.order)(_.copy(state = OrderState.Filled))

      case e: OrderCancelled => ended(exchange, e.order, OrderState.Cancelled, e.releaseOrdinal)
      case e: OrderExpired   => ended(exchange, e.order, OrderState.Expired, e.releaseOrdinal)

      case e: SetsSplit =>
        moved(exchange, e).copy(
          setMoves =
            exchange.setMoves.updated(e.key, SetMove(SetKind.Split, e.owner, e.sets, e.at)),
          setsOutstanding = exchange.setsOutstanding + e.sets
        )

      case e: SetsMerged =>
        moved(exchange, e).copy(
          setMoves =
            exchange.setMoves.updated(e.key, SetMove(SetKind.Merge, e.owner, e.sets, e.at)),
          setsOutstanding = exchange.setsOutstanding - e.sets
        )

      case _: Suspended => exchange.copy(state = ExchangeState.Suspended)
      case _: Resumed   => exchange.copy(state = ExchangeState.Open)
      case _: Closed    => exchange.copy(state = ExchangeState.Closed)

      case e: PositionPaid => moved(exchange, e)

      case e: RemainderKept => exchange.copy(remainder = exchange.remainder + e.amount)

      case e: Final =>
        exchange.copy(state = ExchangeState.Final, setsOutstanding = 0, finality = Some(e.finality))
    next.copy(lastTime = Some(event.at))

  private def moved(exchange: Exchange, event: ExchangeEvent): Exchange =
    PositionMoves
      .of(event, exchange.outcomes, exchange.unit)
      .foldLeft(exchange)((x, m) => x.withPosition(m.owner, m.outcome)(m.move))

  private def ended(exchange: Exchange, id: OrderId, state: OrderState, ordinal: Int): Exchange =
    exchange.orders.get(id).fold(exchange) { order =>
      val freed = exchange.withPosition(order.owner, order.outcome)(p =>
        if order.side == Side.Sell then
          p.copy(restingUnderSells = p.restingUnderSells - order.remaining)
        else p.copy(restingToBuy = p.restingToBuy - order.remaining)
      )
      freed
        .withoutResting(id)
        .withOrder(id)(o =>
          o.copy(state = state, releases = if ordinal > 0 then ordinal else o.releases)
        )
    }

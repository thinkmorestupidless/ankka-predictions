package com.thinkmorestupidless.predictions.markets

import org.scalacheck.{Gen, Prop}

import java.time.{Duration, Instant, ZoneOffset}

/**
 * Spec 006 SC-002 (research R11): random command sequences, and after every command the invariants
 * of the data model: a decision's events replayed onto its value are its next value; the journal
 * replays to the value; the last change's time never goes back and is the last event's; the record
 * has one entry per event; the next due time is the state's; a final market refuses every command
 * but finalise, which yields no event. `-Dpredictions.sequences=<n>` sets the count (100,000).
 */
class MarketProperties extends munit.ScalaCheckSuite:

  private val sequences = sys.props.get("predictions.sequences").fold(100_000)(_.toInt)

  override def scalaCheckTestParameters =
    super.scalaCheckTestParameters.withMinSuccessfulTests(sequences).withWorkers(1)

  override val munitTimeout = scala.concurrent.duration.Duration(30, "min")

  private val t0        = Instant.parse("2026-10-21T10:00:00Z")
  private val operators = Vector("olga", "ops").map(OperatorId(_))
  private val outcomes  = Vector("yes", "no", "maybe").map(OutcomeId(_))
  private val evidence  = Vector(Evidence("source", "locator", "excerpt"))

  private def definition(close: Instant, openTime: Option[Instant], question: String) =
    Definition(
      question,
      "",
      "sport",
      outcomes.take(2),
      "GBP",
      openTime,
      close,
      close.atZone(ZoneOffset.UTC).toLocalDate,
      "criteria",
      "authority"
    )

  private val definitions: Gen[Definition] = for
    close    <- Gen.choose(5L, 40L).map(d => t0.plus(Duration.ofDays(d)))
    openTime <- Gen.option(Gen.choose(1L, 4L).map(d => t0.plus(Duration.ofDays(d))))
    question <- Gen.frequency(9 -> Gen.const("Will it happen?"), 1 -> Gen.const(" "))
  yield definition(close, openTime, question)

  private val windows: Gen[Duration] =
    Gen.frequency(
      8 -> Gen.const(Duration.ofHours(24)),
      1 -> Gen.const(Duration.ZERO),
      1 -> Gen.const(Duration.ofHours(-1))
    )

  private val texts: Gen[String] = Gen.frequency(9 -> Gen.const("a reason"), 1 -> Gen.const(""))

  /** A command at `at`, from a vocabulary that reaches every state. */
  private def command(at: Instant): Gen[MarketCommand] =
    val op = Gen.oneOf(operators)
    Gen.frequency(
      2 -> (for o <- op; d <- definitions yield MarketCommand.Draft(MarketId("m-1"), o, d, at)),
      2 -> (for
        o    <- op; d <- definitions
        kind <- Gen.oneOf("description", "criteria", "question")
      yield MarketCommand.Change(
        o,
        kind match
          case "description" => d.copy(description = "more")
          case "criteria"    => d.copy(criteria = "other criteria")
          case _             => d.copy(question = "Something else?")
        ,
        at
      )),
      3 -> op.map(MarketCommand.Approve(_, at)),
      2 -> Gen.const(MarketCommand.Open(at)),
      1 -> (for o <- op; r <- texts yield MarketCommand.Suspend(o, r, at)),
      1 -> op.map(MarketCommand.Resume(_, at)),
      3 -> Gen.oneOf(
        Gen.const(MarketCommand.Close(None, None, at)),
        for o <- op; r <- texts yield MarketCommand.Close(Some(o), Some(r), at)
      ),
      1 -> (for o <- op; d <- Gen.choose(-5L, 10L); r <- texts
      yield MarketCommand.MoveClose(o, at.plus(Duration.ofDays(d)), r, at)),
      3 -> (for
        o <- op; out                                                      <- Gen.oneOf(outcomes);
        e <- Gen.frequency(9 -> evidence, 1 -> Vector.empty[Evidence]); w <- windows
      yield MarketCommand.Resolve(o, out, e, "authority", w, at)),
      4 -> (for o <- op; out <- Gen.oneOf(outcomes); w <- windows
      yield MarketCommand.Correct(o, out, evidence, w, at)),
      3 -> (for o <- op; r <- texts; w <- windows yield MarketCommand.Void(o, r, evidence, w, at)),
      3 -> Gen.const(MarketCommand.Finalise(at))
    )

  // Half the runs start from a market closed early, so resolution, correction and a void after
  // a resolution are reached often enough to be tested, not only reachable.
  private val closing: Vector[MarketCommand] = Vector(
    MarketCommand.Draft(
      MarketId("m-1"),
      operators(0),
      definition(t0.plus(Duration.ofDays(30)), None, "Will it happen?"),
      t0
    ),
    MarketCommand.Approve(operators(0), t0),
    MarketCommand.Close(Some(operators(0)), Some("early"), t0)
  )

  private def later(c: MarketCommand): MarketCommand = c match
    case x: MarketCommand.Draft     => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Change    => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Approve   => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Open      => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Suspend   => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Resume    => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Close     => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.MoveClose => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Resolve   => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Correct   => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Void      => x.copy(at = x.at.plusSeconds(1))
    case x: MarketCommand.Finalise  => x.copy(at = x.at.plusSeconds(1))

  /** Times that mostly move on, by hours to days, and sometimes go back. */
  private val runs: Gen[Vector[MarketCommand]] = for
    length <- Gen.choose(1, 30)
    steps <- Gen.listOfN(
      length,
      Gen.frequency(
        6 -> Gen.choose(1L, 12L),
        3 -> Gen.choose(12L, 72L),
        2 -> Gen.choose(72L, 24L * 20),
        1 -> Gen.const(-2L)
      )
    )
    times = steps.scanLeft(t0)((t, h) => t.plus(Duration.ofHours(h))).tail
    cs     <- Gen.sequence[Vector[MarketCommand], MarketCommand](times.map(command))
    closed <- Gen.oneOf(true, false)
  yield if closed then closing ++ cs.map(later) else cs

  /**
   * The next due time worked out from the journal alone, not from the market's value: the open time
   * once approved, the close once opened (the latest moved close), the window's end once resolved,
   * corrected or voided, nothing once closed or final.
   */
  private def due(journal: Vector[MarketEvent]): Option[Instant] =
    import MarketEvent.*
    // (definition, phase, window end): the phase is what is timed next, from the event's name.
    val (d, phase, end) =
      journal.foldLeft((Option.empty[Definition], "draft", Option.empty[Instant])) {
        case ((_, _, _), e: Drafted)    => (Some(e.definition), "draft", None)
        case ((_, p, w), e: Changed)    => (Some(e.definition), p, w)
        case ((d, _, w), _: Approved)   => (d, "opening", w)
        case ((d, _, w), _: Opened)     => (d, "closing", w)
        case ((d, p, w), _: Suspended)  => (d, p, w)
        case ((d, p, w), _: Resumed)    => (d, p, w)
        case ((d, p, w), e: CloseMoved) => (d.map(_.copy(close = e.close)), p, w)
        case ((d, _, _), _: Closed)     => (d, "closed", None)
        case ((d, _, _), e: Resolved)   => (d, "window", Some(e.windowEnd))
        case ((d, _, _), e: Corrected)  => (d, "window", Some(e.windowEnd))
        case ((d, _, _), e: Voided)     => (d, "window", Some(e.windowEnd))
        case ((d, _, _), _: Final)      => (d, "final", None)
      }
    phase match
      case "opening" => d.flatMap(_.openTime)
      case "closing" => d.map(_.close)
      case "window"  => end
      case _         => None

  private val ran     = java.util.concurrent.atomic.AtomicInteger()
  private val counted = scala.collection.concurrent.TrieMap.empty[String, Long]

  override def afterAll(): Unit =
    val shown = counted.toVector.sorted.map((k, v) => s"$k $v").mkString(", ")
    println(s"MarketProperties ran ${ran.get} sequences: $shown")

  property("no sequence of commands breaks replay, the record or the next due time") {
    Prop.forAllNoShrink(runs) { commands =>
      ran.incrementAndGet(): Unit
      commands.zipWithIndex.foldLeft((Market.empty, Vector.empty[MarketEvent])) {
        case ((m, journal), (c, i)) =>
          def clue   = s"after command $i of ${commands.mkString("\n  ", "\n  ", "")}"
          val answer = Market(m, c)
          if m.state == MarketState.Final then
            c match
              case _: MarketCommand.Finalise =>
                assertEquals(answer.map(_.events), Right(Vector.empty), clue)
              case _ => assert(answer.isLeft, s"a final market accepted $c: $clue")
          answer match
            case Left(_) => (m, journal)
            case Right(d) =>
              assertEquals(d.events.foldLeft(m)(Market.replay), d.next, s"the decision: $clue")
              val all = journal ++ d.events
              d.events.foreach { e =>
                counted.updateWith(e.getClass.getSimpleName)(n => Some(n.getOrElse(0L) + 1)): Unit
                if e.isInstanceOf[MarketEvent.Voided] && m.state == MarketState.Resolved then
                  counted.updateWith("Voided after Resolved")(n => Some(n.getOrElse(0L) + 1)): Unit
              }
              assertEquals(all.foldLeft(Market.empty)(Market.replay), d.next, s"the journal: $clue")
              assertEquals(d.next.lastTime, all.lastOption.map(_.at).orElse(m.lastTime), clue)
              m.lastTime.foreach(t => assert(!d.next.lastTime.exists(_.isBefore(t)), clue))
              assertEquals(d.next.record.size, all.size, s"the record: $clue")
              assertEquals(d.next.nextDue, due(all), s"next due: $clue")
              // An approved market is always due to open; an open one never opens past its close.
              if d.next.state == MarketState.Approved then
                assert(d.next.nextDue.isDefined, s"approved with nothing due: $clue")
              d.events.collect { case o: MarketEvent.Opened => o }.foreach { o =>
                assert(
                  d.next.definition.exists(_.close.isAfter(o.at)),
                  s"opened past its close: $clue"
                )
              }
              (d.next, all)
      }: Unit
    }
  }

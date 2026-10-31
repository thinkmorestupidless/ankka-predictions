package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.{Duration, Instant, ZoneOffset}

/**
 * One market, as a value: its definition, state, record, resolution and corrections, void and the
 * time of its last change (spec 006 FR-001).
 */
final case class Market(
    market: MarketId = MarketId(""),
    definition: Option[Definition] = None,
    state: MarketState = MarketState.Empty,
    record: Vector[Entry] = Vector.empty,
    resolutions: Vector[Resolution] = Vector.empty,
    void: Option[VoidRecord] = None,
    lastTime: Option[Instant] = None
):

  /** The resolution in force: the first, or the last correction revising it. */
  def resolution: Option[Resolution] = resolutions.lastOption

  /**
   * The one time the market is next due to change by itself (spec 006 FR-013): the open time while
   * approved, the close while open or suspended, the end of the review window while resolved or
   * void, none otherwise. The consumer's timer applies the change at that time.
   */
  def nextDue: Option[Instant] = state match
    case MarketState.Approved                     => definition.flatMap(_.openTime)
    case MarketState.Open | MarketState.Suspended => definition.map(_.close)
    case MarketState.Resolved                     => resolution.map(_.windowEnd)
    case MarketState.Void                         => void.map(_.windowEnd)
    case _                                        => None

object Market:

  val empty: Market = Market()

  /** Snapshots, under the manifest `market`; the events' serializer is in their companion. */
  given Serializer[Market] = Codecs.serializer[Market]("market")

  /**
   * A command applied to a market: a refusal with no change, or the next value and its events,
   * where the next value is the events replayed onto this one (spec 006 FR-002). The time is
   * checked first, then the state, then the command's fields, in the order research R2 gives; a
   * finalise of a final market is answered with no event before any of them (FR-012).
   */
  def apply(market: Market, command: MarketCommand): Either[Refusal, Decision] =
    command match
      case _: MarketCommand.Finalise if market.state == MarketState.Final =>
        Right(Decision(market, Vector.empty))
      case _ =>
        market.lastTime.filter(command.at.isBefore) match
          case Some(last) => Left(Refusals.outOfOrder(last, command.at))
          case None =>
            command match
              case c: MarketCommand.Draft     => draft(market, c)
              case c: MarketCommand.Change    => change(market, c)
              case c: MarketCommand.Approve   => approve(market, c)
              case c: MarketCommand.Open      => open(market, c)
              case c: MarketCommand.Suspend   => suspend(market, c)
              case c: MarketCommand.Resume    => resume(market, c)
              case c: MarketCommand.Close     => close(market, c)
              case c: MarketCommand.MoveClose => moveClose(market, c)
              case c: MarketCommand.Resolve   => resolve(market, c)
              case c: MarketCommand.Correct   => correct(market, c)
              case c: MarketCommand.Void      => void(market, c)
              case c: MarketCommand.Finalise  => finalise(market, c)

  private def in(market: Market, states: MarketState*): Either[Refusal, Unit] =
    Either.cond(states.contains(market.state), (), Refusals.state(market.state))

  private def check(refusal: Option[Refusal]): Either[Refusal, Unit] = refusal.toLeft(())

  /** The definition of a market past its draft; a value built by hand without one is refused. */
  private def defined(market: Market): Either[Refusal, Definition] =
    market.definition.toRight(Refusals.state(market.state))

  private def reason(text: String): Either[Refusal, Unit] =
    Either.cond(!Drafting.blank(text), (), Refusals.field("reason"))

  /** At least one piece of evidence, none with a blank part (research R6). */
  private def evidence(pieces: Vector[Evidence]): Either[Refusal, Unit] =
    Either.cond(
      pieces.nonEmpty && pieces.forall(e =>
        !Drafting.blank(e.source) && !Drafting.blank(e.locator) && !Drafting.blank(e.excerpt)
      ),
      (),
      Refusals.evidence
    )

  private def outcome(market: Market, o: OutcomeId): Either[Refusal, Unit] =
    Either.cond(market.definition.exists(_.outcomes.contains(o)), (), Refusals.outcome(o))

  private def window(length: Duration): Either[Refusal, Unit] =
    Either.cond(!length.isNegative, (), Refusals.reviewWindow(length))

  /** The review window in force has not ended at `at` (research R9). */
  private def withinWindow(end: Option[Instant], at: Instant): Either[Refusal, Unit] =
    end.filter(!at.isBefore(_)).map(Refusals.reviewWindow).toLeft(())

  private def draft(market: Market, c: MarketCommand.Draft): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Empty)
      _ <- check(Drafting.check(c.definition, c.at))
      d <- decide(market, Vector(MarketEvent.Drafted(c.market, c.operator, c.definition, c.at)))
    yield d

  private def change(market: Market, c: MarketCommand.Change): Either[Refusal, Decision] =
    check(Drafting.change(market, c.definition, c.at))
      .flatMap(_ => decide(market, Vector(MarketEvent.Changed(c.operator, c.definition, c.at))))

  private def approve(market: Market, c: MarketCommand.Approve): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Draft)
      d <- defined(market)
      _ <- Either.cond(d.close.isAfter(c.at), (), Refusals.close(d.close))
      approved = MarketEvent.Approved(c.operator, c.at)
      opensNow = d.openTime.forall(!_.isAfter(c.at))
      next <- decide(
        market,
        if opensNow then Vector(approved, MarketEvent.Opened(c.at)) else Vector(approved)
      )
    yield next

  private def open(market: Market, c: MarketCommand.Open): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Approved)
      _ <- market.definition
        .flatMap(_.openTime)
        .filter(_.isAfter(c.at))
        .map(Refusals.openTime)
        .toLeft(())
      // A late open never makes a market tradeable past its close (research R5).
      defn <- defined(market)
      _    <- Either.cond(defn.close.isAfter(c.at), (), Refusals.close(defn.close))
      d    <- decide(market, Vector(MarketEvent.Opened(c.at)))
    yield d

  private def suspend(market: Market, c: MarketCommand.Suspend): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Open)
      _ <- reason(c.reason)
      d <- decide(market, Vector(MarketEvent.Suspended(c.operator, c.reason, c.at)))
    yield d

  private def resume(market: Market, c: MarketCommand.Resume): Either[Refusal, Decision] =
    in(market, MarketState.Suspended)
      .flatMap(_ => decide(market, Vector(MarketEvent.Resumed(c.operator, c.at))))

  /** At or after the close, as it comes; before it, an operator and a reason (research R7). */
  private def close(market: Market, c: MarketCommand.Close): Either[Refusal, Decision] =
    for
      _    <- in(market, MarketState.Open, MarketState.Suspended)
      defn <- defined(market)
      due   = defn.close
      early = c.at.isBefore(due)
      _ <- Either.cond(
        !early || (c.operator.isDefined && c.reason.exists(!Drafting.blank(_))),
        (),
        Refusals.close(due)
      )
      d <- decide(market, Vector(MarketEvent.Closed(c.operator, c.reason, c.at)))
    yield d

  private def moveClose(market: Market, c: MarketCommand.MoveClose): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Open, MarketState.Suspended)
      d <- defined(market)
      _ <- Either.cond(c.close.isAfter(d.close), (), Refusals.close(c.close))
      _ <- reason(c.reason)
      closes   = c.close.atZone(ZoneOffset.UTC).toLocalDate
      expected = if d.expectedResolution.isBefore(closes) then closes else d.expectedResolution
      next <- decide(
        market,
        Vector(MarketEvent.CloseMoved(c.operator, c.close, expected, c.reason, c.at))
      )
    yield next

  private def resolve(market: Market, c: MarketCommand.Resolve): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Closed)
      _ <- outcome(market, c.outcome)
      _ <- evidence(c.evidence)
      _ <- window(c.window)
      d <- decide(
        market,
        Vector(
          MarketEvent.Resolved(
            c.operator,
            c.outcome,
            c.evidence,
            c.authority,
            c.at.plus(c.window),
            c.at
          )
        )
      )
    yield d

  private def correct(market: Market, c: MarketCommand.Correct): Either[Refusal, Decision] =
    for
      _ <- in(market, MarketState.Resolved)
      _ <- withinWindow(market.resolution.map(_.windowEnd), c.at)
      _ <- outcome(market, c.outcome)
      _ <- evidence(c.evidence)
      _ <- Either.cond(!market.resolution.exists(_.outcome == c.outcome), (), Refusals.noChange)
      _ <- window(c.window)
      d <- decide(
        market,
        Vector(
          MarketEvent.Corrected(c.operator, c.outcome, c.evidence, c.at.plus(c.window), c.at)
        )
      )
    yield d

  /** From approved to resolved, a resolved market only while its window is open (research R9). */
  private def void(market: Market, c: MarketCommand.Void): Either[Refusal, Decision] =
    for
      _ <- in(
        market,
        MarketState.Approved,
        MarketState.Open,
        MarketState.Suspended,
        MarketState.Closed,
        MarketState.Resolved
      )
      _ <-
        if market.state == MarketState.Resolved then
          withinWindow(market.resolution.map(_.windowEnd), c.at)
        else Right(())
      _ <- reason(c.reason)
      _ <- evidence(c.evidence)
      _ <- window(c.window)
      d <- decide(
        market,
        Vector(
          MarketEvent.Voided(c.operator, c.reason, c.evidence, c.at.plus(c.window), c.at)
        )
      )
    yield d

  private def finalise(market: Market, c: MarketCommand.Finalise): Either[Refusal, Decision] =
    for
      _   <- in(market, MarketState.Resolved, MarketState.Void)
      end <- market.nextDue.toRight(Refusals.state(market.state))
      _   <- Either.cond(!c.at.isBefore(end), (), Refusals.reviewWindow(end))
      d   <- decide(market, Vector(MarketEvent.Final(c.at)))
    yield d

  private def decide(market: Market, events: Vector[MarketEvent]): Either[Refusal, Decision] =
    Right(Decision(events.foldLeft(market)(replay), events))

  /** An event applied to a market: the next value. Total over every case. */
  def replay(market: Market, event: MarketEvent): Market =
    import MarketEvent.*
    def entry(what: String, operator: Option[OperatorId], reason: Option[String] = None) =
      market.record :+ Entry(what, operator, event.at, reason)
    val next = event match
      case e: Drafted =>
        market.copy(
          market = e.market,
          definition = Some(e.definition),
          state = MarketState.Draft,
          record = entry("drafted", Some(e.operator))
        )
      case e: Changed =>
        market.copy(definition = Some(e.definition), record = entry("changed", Some(e.operator)))
      case e: Approved =>
        market.copy(state = MarketState.Approved, record = entry("approved", Some(e.operator)))
      case _: Opened => market.copy(state = MarketState.Open, record = entry("opened", None))
      case e: Suspended =>
        market.copy(
          state = MarketState.Suspended,
          record = entry("suspended", Some(e.operator), Some(e.reason))
        )
      case e: Resumed =>
        market.copy(state = MarketState.Open, record = entry("resumed", Some(e.operator)))
      case e: Closed =>
        market.copy(state = MarketState.Closed, record = entry("closed", e.operator, e.reason))
      case e: CloseMoved =>
        market.copy(
          definition = market.definition.map(
            _.copy(close = e.close, expectedResolution = e.expectedResolution)
          ),
          record = entry("close moved", Some(e.operator), Some(e.reason))
        )
      case e: Resolved =>
        market.copy(
          state = MarketState.Resolved,
          resolutions = Vector(
            Resolution(e.outcome, e.evidence, e.authority, e.operator, e.at, e.windowEnd)
          ),
          record = entry("resolved", Some(e.operator))
        )
      case e: Corrected =>
        val authority = market.resolution.fold("")(_.authority)
        market.copy(
          resolutions = market.resolutions :+
            Resolution(e.outcome, e.evidence, authority, e.operator, e.at, e.windowEnd),
          record = entry("corrected", Some(e.operator))
        )
      case e: Voided =>
        market.copy(
          state = MarketState.Void,
          void = Some(VoidRecord(e.reason, e.evidence, e.operator, e.at, e.windowEnd)),
          record = entry("voided", Some(e.operator), Some(e.reason))
        )
      case _: Final => market.copy(state = MarketState.Final, record = entry("final", None))
    next.copy(lastTime = Some(event.at))

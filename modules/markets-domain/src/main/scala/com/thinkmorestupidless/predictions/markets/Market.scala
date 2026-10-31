package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.Instant

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
   * where the next value is the events replayed onto this one (spec 006 FR-002).
   *
   * Spec 004 builds the out-of-order check and enough of draft, approve and open to reach an open
   * market, which the components start from; the field checks and every other rule are spec 006's,
   * and until they land each other command is refused as not built.
   */
  def apply(market: Market, command: MarketCommand): Either[Refusal, Decision] =
    market.lastTime.filter(command.at.isBefore) match
      case Some(last) => Left(Refusals.outOfOrder(last, command.at))
      case None =>
        command match
          case c: MarketCommand.Draft   => draft(market, c)
          case c: MarketCommand.Approve => approve(market, c)
          case c: MarketCommand.Open    => open(market, c)
          case other                    => Left(Refusals.notBuilt(other.name))

  private def draft(market: Market, c: MarketCommand.Draft): Either[Refusal, Decision] =
    if market.state != MarketState.Empty then Left(Refusals.state(market.state))
    else decide(market, Vector(MarketEvent.Drafted(c.market, c.operator, c.definition, c.at)))

  private def approve(market: Market, c: MarketCommand.Approve): Either[Refusal, Decision] =
    if market.state != MarketState.Draft then Left(Refusals.state(market.state))
    else
      val opensNow = market.definition.forall(_.openTime.forall(!_.isAfter(c.at)))
      val approved = MarketEvent.Approved(c.operator, c.at)
      decide(
        market,
        if opensNow then Vector(approved, MarketEvent.Opened(c.at)) else Vector(approved)
      )

  private def open(market: Market, c: MarketCommand.Open): Either[Refusal, Decision] =
    if market.state != MarketState.Approved then Left(Refusals.state(market.state))
    else
      market.definition.flatMap(_.openTime).filter(_.isAfter(c.at)) match
        case Some(openTime) => Left(Refusals.openTime(openTime))
        case None           => decide(market, Vector(MarketEvent.Opened(c.at)))

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

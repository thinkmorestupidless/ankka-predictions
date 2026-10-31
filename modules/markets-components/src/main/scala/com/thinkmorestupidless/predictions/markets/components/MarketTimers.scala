package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.{CommandError, Codecs, ComponentId, EntityId, Serializer}
import com.thinkmorestupidless.ankka.sdk.{
  Consumer,
  ConsumerContext,
  TimedAction,
  TimedActionContext,
  TimerScheduler
}
import com.thinkmorestupidless.predictions.markets.*

import java.time.Instant
import scala.concurrent.duration.DurationLong

/**
 * Applies the change a market is due at its next-due time (spec 006 FR-013): the open while
 * approved, the close while open or suspended, the finalising while resolved or void. At least
 * once, so a market with nothing due now is nothing to do, and a refusal is not retried.
 */
final class MarketTimers(market: MarketEntityCompanion, ctx: TimedActionContext)
    extends TimedAction:

  def due(id: MarketId): Effect =
    val entity  = ctx.componentClient.forEventSourcedEntity(EntityId(id.value))
    val current = entity.call(market.get).invoke()
    val now     = timerContext.clock.instant()
    current.nextDue.filter(!_.isAfter(now)) match
      case None => effects.done()
      case Some(due) =>
        val at = (Vector(now, due) ++ current.lastTime).max
        try
          current.state match
            case MarketState.Approved =>
              entity.call(market.open).invoke(MarketCommand.Open(at)): Unit
            case MarketState.Open | MarketState.Suspended =>
              entity.call(market.close).invoke(MarketCommand.Close(None, None, at)): Unit
            case MarketState.Resolved | MarketState.Void =>
              entity.call(market.finalise).invoke(MarketCommand.Finalise(at)): Unit
            case _ => ()
        catch
          case error: CommandError =>
            error match
              case Refused(refusal) =>
                MarketTimers.log.log(
                  System.Logger.Level.WARNING,
                  s"the change due to ${id.value} at $due was refused: $refusal"
                )
              case fault => throw fault
        effects.done()

object MarketTimers:
  private val log = System.getLogger("com.thinkmorestupidless.predictions.markets.timers")

  /** The timer of `market`, by name, so setting it again moves it. */
  def timer(market: MarketId): String = s"market-due-${market.value}"

/** The market timers' declaration. */
final class MarketTimersCompanion(market: MarketEntityCompanion)
    extends TimedAction.Companion[MarketTimers](ComponentId("market-timers")):

  def create(ctx: TimedActionContext): MarketTimers = new MarketTimers(market, ctx)

  given Serializer[MarketId] = Codecs.serializer("market-id")

  val due = handler[MarketId]("due")(_.due)

/**
 * Sets each market's timer for its next-due time after every change, or removes it when nothing is
 * due, with the consumer's scheduler: an entity has no scheduler in ankka (research V5). At least
 * once, and safe to repeat, since a timer is named by its market.
 */
final class MarketSchedule(
    market: MarketEntityCompanion,
    timers: MarketTimersCompanion,
    scheduler: () => TimerScheduler,
    ctx: ConsumerContext
) extends Consumer[MarketEvent, Unit]:

  def onMessage(event: MarketEvent): Effect =
    val id = MarketId(messageContext.subject)
    val current =
      ctx.componentClient.forEventSourcedEntity(EntityId(id.value)).call(market.get).invoke()
    current.nextDue match
      case Some(at) =>
        val delay = (at.toEpochMilli - Instant.now().toEpochMilli).max(0L).millis
        scheduler().createSingleTimer(MarketTimers.timer(id), delay, timers.due.deferred(id))
      case None => scheduler().delete(MarketTimers.timer(id))
    effects.done()

final class MarketScheduleConsumer(
    market: MarketEntityCompanion,
    timers: MarketTimersCompanion,
    scheduler: () => TimerScheduler
) extends Consumer.Companion[MarketSchedule, MarketEvent, Unit](
      ComponentId("market-schedule"),
      MarketEntityCompanion.events
    ):
  def create(ctx: ConsumerContext): MarketSchedule =
    new MarketSchedule(market, timers, scheduler, ctx)

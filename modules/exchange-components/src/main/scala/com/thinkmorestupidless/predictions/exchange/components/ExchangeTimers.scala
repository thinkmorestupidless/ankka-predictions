package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.{CommandError, Codecs, ComponentId, EntityId, Serializer}
import com.thinkmorestupidless.ankka.sdk.{TimedAction, TimedActionContext, TimerScheduler}
import com.thinkmorestupidless.predictions.exchange.*

import java.time.Instant
import scala.concurrent.duration.{DurationLong, FiniteDuration}

/**
 * Closes an exchange at its close time. At least once, so an exchange already closed, or never
 * opened, is nothing to do; the close is applied at the later of now and the exchange's last event,
 * so a late sweep is not refused as out of order.
 */
final class ExchangeTimers(ctx: TimedActionContext) extends TimedAction:

  def close(market: MarketId): Effect =
    val exchange = ctx.componentClient.forEventSourcedEntity(EntityId(market.value))
    val current  = exchange.call(ExchangeEntity.get).invoke()
    current.state match
      case ExchangeState.Open | ExchangeState.Suspended =>
        val now = timerContext.clock.instant()
        val at  = current.lastTime.filter(_.isAfter(now)).getOrElse(now)
        try exchange.call(ExchangeEntity.close).invoke(ExchangeCommand.Close(at)): Unit
        catch case Refused(_) => ()
        effects.done()
      case _ => effects.done()

object ExchangeTimers extends TimedAction.Companion[ExchangeTimers](ComponentId("exchange-timers")):

  def create(ctx: TimedActionContext): ExchangeTimers = new ExchangeTimers(ctx)

  given Serializer[MarketId] = Codecs.serializer("exchange-market")

  val close = handler[MarketId]("close")(_.close)

  /** The timer that closes `market`, by name, so scheduling again moves it. */
  def closeTimer(market: MarketId): String = s"exchange-close-${market.value}"

  /** Close `market` at `at`, as seen from `now`; a time already passed is due at once. */
  def scheduleClose(timers: TimerScheduler, market: MarketId, at: Instant, now: Instant): Unit =
    val delay: FiniteDuration = (at.toEpochMilli - now.toEpochMilli).max(0L).millis
    timers.createSingleTimer(closeTimer(market), delay, close.deferred(market))

package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.sdk.TimerScheduler
import com.thinkmorestupidless.predictions.markets.*

// The seam between the market components and the consumer (`contracts/ports.md`): who may do what
// to a market. A port that cannot answer throws; a refusal changes nothing.

enum ResolutionAnswer:
  case Allow
  case Refused(reason: String)

trait ResolutionPort:
  def mayDraft(operator: OperatorId): ResolutionAnswer
  def mayApprove(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayResolve(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayVoid(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayClose(operator: OperatorId, market: MarketId): ResolutionAnswer

  /** Suspend and resume: the operator's emergency authority. */
  def maySuspend(operator: OperatorId, market: MarketId): ResolutionAnswer

/**
 * `timers` is the consumer's timer scheduler, asked for only once its service has started (its
 * `TimerRuntime.timerScheduler`): an entity's context has none in ankka, so the market's timer is
 * set by the components' `market-schedule` consumer with this one (research R9, V5).
 */
final case class MarketPorts(resolution: ResolutionPort, timers: () => TimerScheduler)

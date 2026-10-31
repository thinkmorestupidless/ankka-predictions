package com.thinkmorestupidless.predictions.markets

import java.time.Instant

/** Why a command was refused: a reason and the names it refers to. It changes nothing. */
final case class Refusal(reason: String, names: Map[String, String] = Map.empty)

/** What an accepted command led to: the next value and the events that produce it from the last. */
final case class Decision(next: Market, events: Vector[MarketEvent])

/** The refusals a market gives, by the words spec 006 uses. */
object Refusals:

  def outOfOrder(last: Instant, at: Instant): Refusal =
    Refusal("out of order", Map("last" -> last.toString, "at" -> at.toString))

  def state(state: MarketState): Refusal = Refusal("state", Map("state" -> MarketState.word(state)))
  def field(name: String): Refusal       = Refusal("field", Map("field" -> name))
  def openTime(at: Instant): Refusal     = Refusal("open time", Map("open time" -> at.toString))
  def close(at: Instant): Refusal        = Refusal("close", Map("close" -> at.toString))
  val evidence: Refusal                  = Refusal("evidence")
  def outcome(outcome: OutcomeId): Refusal =
    Refusal("outcome", Map("outcome" -> outcome.value))
  def reviewWindow(end: Instant): Refusal =
    Refusal("review window", Map("review window" -> end.toString))
  val noChange: Refusal = Refusal("no change")

  /** A rule spec 006 has not yet built; the stub's answer to every such command. */
  def notBuilt(command: String): Refusal = Refusal("not built", Map("command" -> command))

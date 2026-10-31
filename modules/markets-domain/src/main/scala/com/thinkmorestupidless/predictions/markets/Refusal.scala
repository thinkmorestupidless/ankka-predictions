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

  /**
   * A field that breaks its rule, by the name the scenarios use: `question`, `description`,
   * `category`, `outcomes`, `currency`, `close`, `expected resolution date`, `resolution criteria`,
   * `authority`, a blank `reason`, or an approved market's `open time` not ahead of a change (spec
   * 006 research R3, R4, R8).
   */
  def field(name: String): Refusal   = Refusal("field", Map("field" -> name))
  def openTime(at: Instant): Refusal = Refusal("open time", Map("open time" -> at.toString))
  def close(at: Instant): Refusal    = Refusal("close", Map("close" -> at.toString))
  val evidence: Refusal              = Refusal("evidence")
  def outcome(outcome: OutcomeId): Refusal =
    Refusal("outcome", Map("outcome" -> outcome.value))
  def reviewWindow(end: Instant): Refusal =
    Refusal("review window", Map("review window" -> end.toString))

  /** A review window of negative length (research R9). */
  def reviewWindow(length: java.time.Duration): Refusal =
    Refusal("review window", Map("review window" -> length.toString))
  val noChange: Refusal = Refusal("no change")

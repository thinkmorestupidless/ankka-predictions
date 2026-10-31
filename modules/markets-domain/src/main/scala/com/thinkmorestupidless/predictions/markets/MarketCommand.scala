package com.thinkmorestupidless.predictions.markets

import java.time.{Duration, Instant}

/**
 * What can be asked of a market (spec 006 FR-002). Every command carries its time and, where an
 * operator acts, the operator the consumer says acted; the domain reads no clock and asks no one.
 */
enum MarketCommand:
  def at: Instant

  case Draft(market: MarketId, operator: OperatorId, definition: Definition, at: Instant)
  case Change(operator: OperatorId, definition: Definition, at: Instant)
  case Approve(operator: OperatorId, at: Instant)
  case Open(at: Instant)
  case Suspend(operator: OperatorId, reason: String, at: Instant)
  case Resume(operator: OperatorId, at: Instant)

  /** At or after the close, no one and no reason; before it, an operator and a reason. */
  case Close(operator: Option[OperatorId], reason: Option[String], at: Instant)

  case MoveClose(operator: OperatorId, close: Instant, reason: String, at: Instant)

  /** `window` is the review window's length, the consumer's setting. */
  case Resolve(
      operator: OperatorId,
      outcome: OutcomeId,
      evidence: Vector[Evidence],
      authority: String,
      window: Duration,
      at: Instant
  )

  case Correct(
      operator: OperatorId,
      outcome: OutcomeId,
      evidence: Vector[Evidence],
      window: Duration,
      at: Instant
  )

  case Void(
      operator: OperatorId,
      reason: String,
      evidence: Vector[Evidence],
      window: Duration,
      at: Instant
  )

  case Finalise(at: Instant)

  /** The command's wire name (`contracts/artefacts.md`). */
  def name: String = this match
    case _: Draft     => "draft"
    case _: Change    => "change"
    case _: Approve   => "approve"
    case _: Open      => "open"
    case _: Suspend   => "suspend"
    case _: Resume    => "resume"
    case _: Close     => "close"
    case _: MoveClose => "move-close"
    case _: Resolve   => "resolve"
    case _: Correct   => "correct"
    case _: Void      => "void"
    case _: Finalise  => "finalise"

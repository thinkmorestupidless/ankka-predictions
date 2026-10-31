package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.{Instant, LocalDate}

/**
 * What happened to a market (spec 006 FR-014), journalled under the manifest `market-event` and
 * named in JSON by `type`. A published protocol: a case is never renamed or removed, and a field is
 * only added, with a default (spec 004 FR-009).
 */
enum MarketEvent:
  def at: Instant

  case Drafted(market: MarketId, operator: OperatorId, definition: Definition, at: Instant)
  case Changed(operator: OperatorId, definition: Definition, at: Instant)
  case Approved(operator: OperatorId, at: Instant)
  case Opened(at: Instant)
  case Suspended(operator: OperatorId, reason: String, at: Instant)
  case Resumed(operator: OperatorId, at: Instant)
  case Closed(operator: Option[OperatorId], reason: Option[String], at: Instant)

  /** The close moved later, and the expected resolution date with it where it would be earlier. */
  case CloseMoved(
      operator: OperatorId,
      close: Instant,
      expectedResolution: LocalDate,
      reason: String,
      at: Instant
  )

  case Resolved(
      operator: OperatorId,
      outcome: OutcomeId,
      evidence: Vector[Evidence],
      authority: String,
      windowEnd: Instant,
      at: Instant
  )

  case Corrected(
      operator: OperatorId,
      outcome: OutcomeId,
      evidence: Vector[Evidence],
      windowEnd: Instant,
      at: Instant
  )

  case Voided(
      operator: OperatorId,
      reason: String,
      evidence: Vector[Evidence],
      windowEnd: Instant,
      at: Instant
  )

  case Final(at: Instant)

object MarketEvent:

  /**
   * The journal's serializer, under the manifest `market-event`: in the companion, so a consumer's
   * entity finds it without an import.
   */
  given Serializer[MarketEvent] = Codecs.serializer[MarketEvent]("market-event")

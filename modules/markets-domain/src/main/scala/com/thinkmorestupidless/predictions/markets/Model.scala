package com.thinkmorestupidless.predictions.markets

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec

import java.time.{Instant, LocalDate}

/**
 * A market's state: `empty → draft → approved → open ↔ suspended → closed → resolved → final`, with
 * `void` from approved, open, suspended or closed, then `final` (spec 006 FR-001).
 */
enum MarketState:
  case Empty, Draft, Approved, Open, Suspended, Closed, Resolved, Void, Final

object MarketState:
  def word(s: MarketState): String  = s.toString.toLowerCase
  given JsonValueCodec[MarketState] = wordCodec(values, word)

/** What a draft says, and what a change replaces while it is a draft (spec 006 FR-004, FR-005). */
final case class Definition(
    question: String,
    description: String,
    category: String,
    outcomes: Vector[OutcomeId],
    currency: String,
    openTime: Option[Instant],
    close: Instant,
    expectedResolution: LocalDate,
    criteria: String,
    authority: String
)

/** One piece of evidence: a source, a locator and an excerpt. */
final case class Evidence(source: String, locator: String, excerpt: String)

/** A resolution, or a correction revising it, with the end of its review window. */
final case class Resolution(
    outcome: OutcomeId,
    evidence: Vector[Evidence],
    authority: String,
    operator: OperatorId,
    at: Instant,
    windowEnd: Instant
)

/** A void: its reason and evidence, with the end of its review window. */
final case class VoidRecord(
    reason: String,
    evidence: Vector[Evidence],
    operator: OperatorId,
    at: Instant,
    windowEnd: Instant
)

/**
 * One change in a market's record: what changed, who did it, when, and why if a reason was given.
 */
final case class Entry(
    what: String,
    operator: Option[OperatorId],
    at: Instant,
    reason: Option[String]
)

package com.thinkmorestupidless.predictions.markets

import java.time.{Duration, Instant, LocalDate, ZoneOffset}
import scala.collection.mutable

/**
 * The step definitions' world (spec 006 research R10): each market by its id, with the events that
 * made it, and the answer to the last command. A seeded market is reached by applying commands to
 * `Market.empty`, never built as a value, so a seed that breaks a rule fails the scenario and every
 * seed replays. A step with no time acts at `now`.
 */
final class MarketsWorld:

  type Answer = Either[Refusal, Decision]

  val now: Instant         = Instant.parse("2026-10-21T10:00:00Z")
  val seeded: Instant      = Instant.parse("2026-09-01T00:00:00Z")
  val ops: OperatorId      = OperatorId("ops")
  var operator             = OperatorId("olga")
  var window: Duration     = Duration.ofHours(24)
  var last: Option[Answer] = None

  /** The market the scenario's last step named, for a step that names none. */
  var current: String = ""

  private final case class Kept(value: Market, events: Vector[MarketEvent])
  private val markets = mutable.Map.empty[String, Kept]

  /** A valid definition, closing at `close`, its expected resolution date the close's own. */
  def definition(
      close: Instant = Instant.parse("2027-05-24T00:00:00Z"),
      openTime: Option[Instant] = None,
      outcomes: Vector[String] = Vector("yes", "no"),
      question: String = "Will Newcastle finish in the top four?",
      criteria: String = "The final Premier League table of the 2026-27 season",
      authority: String = "premierleague.com"
  ): Definition = Definition(
    question,
    "",
    "sport",
    outcomes.map(OutcomeId(_)),
    "GBP",
    openTime,
    close,
    close.atZone(ZoneOffset.UTC).toLocalDate,
    criteria,
    authority
  )

  def value(id: String): Market =
    markets.getOrElse(id, sys.error(s"no market $id in this scenario")).value

  def events(id: String): Vector[MarketEvent] = markets(id).events

  /** Apply a command to the market (empty when there is none yet), recording its answer. */
  def apply(id: String, command: MarketCommand): Answer =
    current = id
    val kept   = markets.getOrElse(id, Kept(Market.empty, Vector.empty))
    val answer = Market(kept.value, command)
    answer.foreach(d => markets(id) = Kept(d.next, kept.events ++ d.events))
    last = Some(answer)
    answer

  /** Apply commands that must all be accepted: a seed. */
  def seed(id: String, commands: MarketCommand*): Unit =
    current = id
    commands.foreach(c => apply(id, c).left.foreach(r => sys.error(s"seed $id: $c refused: $r")))
    last = None

  def drafted(id: String, d: Definition, at: Instant = seeded, by: OperatorId = ops): Unit =
    seed(id, MarketCommand.Draft(MarketId(id), by, d, at))

  /** Drafted and approved: open at once unless the open time is ahead of the approval. */
  def approved(id: String, d: Definition, at: Instant = seeded): Unit =
    drafted(id, d, at)
    seed(id, MarketCommand.Approve(ops, at.plusSeconds(1)))

  /** A market closed an hour before `at`, drafted sixty days before it. */
  def closed(id: String, at: Instant, d: Instant => Definition = c => definition(close = c)): Unit =
    val close = at.minus(Duration.ofHours(1))
    approved(id, d(close), at.minus(Duration.ofDays(60)))
    seed(id, MarketCommand.Close(None, None, close))

  def resolved(id: String, outcome: String, at: Instant): Unit =
    closed(id, at)
    seed(
      id,
      MarketCommand.Resolve(
        ops,
        OutcomeId(outcome),
        Vector(evidence),
        "premierleague.com",
        window,
        at
      )
    )

  val evidence: Evidence =
    Evidence("premierleague.com", "https://premierleague.com/tables", "Newcastle finished fourth")

  /**
   * The command `nextDue` calls for at `t`: open while approved, close while open or suspended,
   * finalise while resolved or void.
   */
  def comes(id: String, t: Instant): Answer =
    value(id).state match
      case MarketState.Approved                     => apply(id, MarketCommand.Open(t))
      case MarketState.Open | MarketState.Suspended => apply(id, MarketCommand.Close(None, None, t))
      case _                                        => apply(id, MarketCommand.Finalise(t))

  def refusal: Refusal = last match
    case Some(Left(r))  => r
    case Some(Right(d)) => sys.error(s"accepted with ${d.events}")
    case None           => sys.error("no command was applied")

  def accepted: Decision = last match
    case Some(Right(d)) => d
    case Some(Left(r))  => sys.error(s"refused: $r")
    case None           => sys.error("no command was applied")

  def date(s: String): LocalDate = LocalDate.parse(s)

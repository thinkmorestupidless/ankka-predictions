package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.predictions.markets.*

import java.time.{Duration, Instant}

/**
 * Writes the markets' journal fixtures for a release (research R12), from this build's serializers:
 * `sbt "marketsComponents/Test/runMain
 * com.thinkmorestupidless.predictions.markets.components.JournalFixtureWriter 0.0.0"`. Run once per
 * release; a written fixture is never rewritten.
 */
object JournalFixtureWriter:

  private val t = Instant.parse("2026-10-31T10:00:00Z")

  /** One event of every case, in a market's order. */
  def everyEvent: Vector[MarketEvent] =
    import MarketEvent.*
    val ops      = OperatorId("ops")
    val lead     = OperatorId("lead")
    val defined  = Samples.definition("yes", "no", "USD", t)
    val evidence = Vector(Evidence("the source", "https://example.org/result", "it happened"))
    val window   = t.plus(Duration.ofDays(9))
    Vector(
      Drafted(MarketId("m-1"), ops, defined, t),
      Changed(ops, defined.copy(question = "Will it happen by the close?"), t),
      Approved(lead, t),
      Opened(t),
      Suspended(lead, "the source is down", t),
      Resumed(lead, t),
      CloseMoved(
        lead,
        defined.close.plus(Duration.ofDays(1)),
        defined.expectedResolution.plusDays(1),
        "a delay",
        t
      ),
      Closed(None, None, t),
      Resolved(lead, OutcomeId("yes"), evidence, "the authority", window, t),
      Corrected(lead, OutcomeId("no"), evidence, window, t),
      Voided(lead, "the event was cancelled", evidence, window, t),
      Final(t)
    )

  def main(args: Array[String]): Unit =
    val release = args.headOption.getOrElse(sys.error("name the release"))
    val dir     = Journal.root.resolve(release)
    require(
      !java.nio.file.Files.exists(dir),
      s"$dir exists: a release's fixtures are never rewritten"
    )

    val every = everyEvent
    Journal.write(
      dir.resolve("every-event.jsonl"),
      every,
      every.foldLeft(Market.empty)(Market.replay)
    )

    // A scenario this build's domain runs: a market drafted and approved, opening at once.
    val drafted = Market(
      Market.empty,
      MarketCommand.Draft(
        MarketId("m-1"),
        OperatorId("ops"),
        Samples.definition("yes", "no", "USD", t),
        t
      )
    ).fold(r => sys.error(s"the market was not drafted: $r"), identity)
    val approved = Market(drafted.next, MarketCommand.Approve(OperatorId("lead"), t))
      .fold(r => sys.error(s"the market was not approved: $r"), identity)
    Journal.write(dir.resolve("opened.jsonl"), drafted.events ++ approved.events, approved.next)

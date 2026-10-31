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

    // A market drafted and approved, opening at once: the scenario every consumer starts from.
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

    // A market's whole life (research R13), and one voided within its window after a resolution.
    val ops      = OperatorId("ops")
    val lead     = OperatorId("lead")
    val evidence = Vector(Evidence("the source", "https://example.org/result", "it happened"))
    val day      = Duration.ofDays(1)
    def run(commands: MarketCommand*): Decision =
      commands.foldLeft(Decision(Market.empty, Vector.empty)) { (d, c) =>
        Market(d.next, c)
          .fold(r => sys.error(s"$c: $r"), n => Decision(n.next, d.events ++ n.events))
      }
    val defined = Samples.definition("yes", "no", "USD", t, openTime = Some(t.plus(day)))
    val closed = Vector(
      MarketCommand.Draft(MarketId("m-2"), ops, defined, t),
      MarketCommand.Change(ops, defined.copy(description = "Whether it happens, as reported."), t),
      MarketCommand.Approve(lead, t),
      MarketCommand.Open(t.plus(day)),
      MarketCommand.Suspend(lead, "the source is down", t.plus(day.multipliedBy(2))),
      MarketCommand.Resume(lead, t.plus(day.multipliedBy(3))),
      MarketCommand
        .MoveClose(lead, defined.close.plus(day), "a delay", t.plus(day.multipliedBy(4))),
      MarketCommand.Close(None, None, defined.close.plus(day))
    )
    val resolvedAt = defined.close.plus(day.multipliedBy(2))
    val resolved = run(
      (closed ++ Vector(
        MarketCommand.Resolve(lead, OutcomeId("yes"), evidence, "the authority", day, resolvedAt),
        MarketCommand.Correct(lead, OutcomeId("no"), evidence, day, resolvedAt.plusSeconds(3600)),
        MarketCommand.Finalise(resolvedAt.plus(day.multipliedBy(2)))
      ))*
    )
    Journal.write(dir.resolve("resolved.jsonl"), resolved.events, resolved.next)
    val voided = run(
      (closed ++ Vector(
        MarketCommand.Resolve(lead, OutcomeId("yes"), evidence, "the authority", day, resolvedAt),
        MarketCommand
          .Void(lead, "the question had no answer", evidence, day, resolvedAt.plusSeconds(3600)),
        MarketCommand.Finalise(resolvedAt.plus(day.multipliedBy(2)))
      ))*
    )
    Journal.write(dir.resolve("voided.jsonl"), voided.events, voided.next)

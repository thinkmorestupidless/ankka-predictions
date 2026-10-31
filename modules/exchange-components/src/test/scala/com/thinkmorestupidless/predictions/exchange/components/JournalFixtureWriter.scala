package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.predictions.exchange.*

import java.time.Instant

/**
 * Writes the exchange's journal fixtures for a release (research R12), from this build's
 * serializers: `sbt "exchangeComponents/Test/runMain
 * com.thinkmorestupidless.predictions.exchange.components.JournalFixtureWriter 0.0.0"`. Run once
 * per release; a written fixture is never rewritten.
 */
object JournalFixtureWriter:

  private val t = Instant.parse("2026-10-31T10:00:00Z")

  def main(args: Array[String]): Unit =
    val release = args.headOption.getOrElse(sys.error("name the release"))
    val dir     = Journal.root.resolve(release)
    require(
      !java.nio.file.Files.exists(dir),
      s"$dir exists: a release's fixtures are never rewritten"
    )

    // Every case, in the order the samples give them; most are ones the stub domain does not produce.
    val every = Samples.everyEvent(t)
    Journal.write(
      dir.resolve("every-event.jsonl"),
      every,
      every.foldLeft(Exchange.empty)(Exchange.replay)
    )

    // A scenario this build's domain runs: an exchange opened.
    val opened = Exchange(
      Exchange.empty,
      ExchangeCommand.Open(
        MarketId("m-1"),
        Vector(OutcomeId("yes"), OutcomeId("no")),
        100,
        100_000,
        10_000,
        t
      )
    ).fold(r => sys.error(s"the exchange did not open: $r"), identity)
    Journal.write(dir.resolve("opened.jsonl"), opened.events, opened.next)

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

    // Every case, in the order the samples give them, replayed whatever a domain would decide.
    val every = Samples.everyEvent(t)
    Journal.write(
      dir.resolve("every-event.jsonl"),
      every,
      every.foldLeft(Exchange.empty)(Exchange.replay)
    )

    // An exchange opened, the scenario every consumer starts from.
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

    // A market traded to its end: a split, a sell, a buy at a better price, a trade through the
    // pool, a cancel, a close and a finality, so a journal with money on it is replayed.
    val yes = OutcomeId("yes")
    val no  = OutcomeId("no")
    def place(id: String, owner: String, outcome: OutcomeId, side: Side, price: Long, at: Int) =
      ExchangeCommand.Place(
        OrderId(id),
        OwnerId(owner),
        outcome,
        side,
        price,
        10,
        OrderDuration.RestUntilClose,
        t.plusSeconds(at.toLong)
      )
    val traded = Vector(
      ExchangeCommand.Split(SetKey("k-1"), OwnerId("ada"), 20, t.plusSeconds(1)),
      place("s-1", "ada", yes, Side.Sell, 55, 2),
      place("b-1", "bob", yes, Side.Buy, 60, 3),
      place("b-2", "cy", no, Side.Buy, 45, 4),
      place("b-3", "bob", yes, Side.Buy, 58, 5),
      place("b-4", "cy", no, Side.Buy, 30, 6),
      ExchangeCommand.Cancel(OrderId("b-4"), OwnerId("cy"), t.plusSeconds(7)),
      ExchangeCommand.Close(t.plusSeconds(8)),
      ExchangeCommand.Finalise(Finality.Resolved(yes), t.plusSeconds(9))
    ).foldLeft(opened) { (d, c) =>
      Exchange(d.next, c)
        .fold(r => sys.error(s"$c: $r"), n => Decision(n.next, d.events ++ n.events))
    }
    Journal.write(dir.resolve("traded.jsonl"), traded.events, traded.next)

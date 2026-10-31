package com.thinkmorestupidless.predictions.markets

import java.time.{Instant, LocalDate}

/**
 * What spec 004 builds of the markets domain, which the components start from: a draft approved
 * with no open time opens in the same change, one with an open time ahead opens when it comes and
 * not before, and the next due time follows the state.
 */
class MarketStubSuite extends munit.FunSuite:

  private val t0   = Instant.parse("2026-10-31T10:00:00Z")
  private val olga = OperatorId("olga")

  private def definition(openTime: Option[Instant]) = Definition(
    "Will Newcastle finish in the top four?",
    "",
    "sport",
    Vector(OutcomeId("yes"), OutcomeId("no")),
    "GBP",
    openTime,
    Instant.parse("2027-05-24T00:00:00Z"),
    LocalDate.parse("2027-05-24"),
    "The final Premier League table of the 2026-27 season",
    "premierleague.com"
  )

  private def run(market: Market, commands: MarketCommand*): Market =
    commands.foldLeft(market)((m, c) => Market(m, c).fold(r => fail(s"$c refused: $r"), _.next))

  test("a draft approved with no open time opens at once, and replays to the same market") {
    val draft = MarketCommand.Draft(MarketId("m-1"), olga, definition(None), t0)
    val decided = for
      d <- Market(Market.empty, draft)
      a <- Market(d.next, MarketCommand.Approve(olga, t0.plusSeconds(60)))
    yield (d, a)
    val (d, a) = decided.fold(r => fail(r.toString), identity)
    assertEquals(a.next.state, MarketState.Open)
    assertEquals(a.events.map(_.getClass.getSimpleName), Vector("Approved", "Opened"))
    assertEquals((d.events ++ a.events).foldLeft(Market.empty)(Market.replay), a.next)
    assertEquals(a.next.nextDue, Some(Instant.parse("2027-05-24T00:00:00Z")))
  }

  test("a market with an open time ahead is approved, refused opening before it and opens at it") {
    val opens = t0.plusSeconds(3600)
    val approved = run(
      Market.empty,
      MarketCommand.Draft(MarketId("m-2"), olga, definition(Some(opens)), t0),
      MarketCommand.Approve(olga, t0)
    )
    assertEquals(approved.state, MarketState.Approved)
    assertEquals(approved.nextDue, Some(opens))
    assertEquals(
      Market(approved, MarketCommand.Open(t0.plusSeconds(60))).left.map(_.reason),
      Left("open time")
    )
    assertEquals(run(approved, MarketCommand.Open(opens)).state, MarketState.Open)
  }

  test("a command earlier than the market's last change is refused as out of order") {
    val drafted =
      run(Market.empty, MarketCommand.Draft(MarketId("m-3"), olga, definition(None), t0))
    assertEquals(
      Market(drafted, MarketCommand.Approve(olga, t0.minusSeconds(1))).left.map(_.reason),
      Left("out of order")
    )
  }

  test("a rule spec 006 has not built is refused as not built, naming the command") {
    val open = run(
      Market.empty,
      MarketCommand.Draft(MarketId("m-4"), olga, definition(None), t0),
      MarketCommand.Approve(olga, t0)
    )
    assertEquals(
      Market(open, MarketCommand.Suspend(olga, "investigation", t0)),
      Left(Refusal("not built", Map("command" -> "suspend")))
    )
  }

  test("a market and its events round-trip through their serializers") {
    val market = run(
      Market.empty,
      MarketCommand.Draft(MarketId("m-5"), olga, definition(Some(t0.plusSeconds(60))), t0),
      MarketCommand.Approve(olga, t0)
    )
    val s = summon[com.thinkmorestupidless.ankka.core.Serializer[Market]]
    assertEquals(s.fromBytes(s.toBytes(market)), market)
    assertEquals(s.manifest, "market")
  }

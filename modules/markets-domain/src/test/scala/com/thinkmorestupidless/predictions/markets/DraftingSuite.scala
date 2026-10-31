package com.thinkmorestupidless.predictions.markets

import java.time.{Instant, LocalDate}

/** The definition's field rules at their edges (spec 006 FR-004; research R3, R4). */
class DraftingSuite extends munit.FunSuite:

  private val at = Instant.parse("2026-10-21T10:00:00Z")

  private val valid = Definition(
    "Will Newcastle finish in the top four?",
    "",
    "sport",
    Vector(OutcomeId("yes"), OutcomeId("no")),
    "GBP",
    None,
    Instant.parse("2027-05-24T23:00:00Z"),
    LocalDate.parse("2027-05-24"),
    "The final Premier League table of the 2026-27 season",
    "premierleague.com"
  )

  private def refused(d: Definition): Option[String] =
    Drafting.check(d, at).map(_.names("field"))

  private def outcomes(n: Int) = (1 to n).map(i => OutcomeId(s"o-$i")).toVector

  test("a valid definition passes") {
    assertEquals(Drafting.check(valid, at), None)
  }

  test("the question is 1 to 500 code points and not blank") {
    assertEquals(refused(valid.copy(question = "")), Some("question"))
    assertEquals(refused(valid.copy(question = "   ")), Some("question"))
    assertEquals(refused(valid.copy(question = "x" * 500)), None)
    assertEquals(refused(valid.copy(question = "x" * 501)), Some("question"))
    // 500 characters outside the basic plane are 1,000 UTF-16 units and still 500 code points.
    assertEquals(refused(valid.copy(question = "😀" * 500)), None)
  }

  test("the description is at most 5,000") {
    assertEquals(refused(valid.copy(description = "x" * 5_000)), None)
    assertEquals(refused(valid.copy(description = "x" * 5_001)), Some("description"))
  }

  test("the category and the currency are not blank") {
    assertEquals(refused(valid.copy(category = " ")), Some("category"))
    assertEquals(refused(valid.copy(currency = "")), Some("currency"))
  }

  test("two to twenty outcomes, each 1 to 100, all distinct") {
    assertEquals(refused(valid.copy(outcomes = outcomes(1))), Some("outcomes"))
    assertEquals(refused(valid.copy(outcomes = outcomes(2))), None)
    assertEquals(refused(valid.copy(outcomes = outcomes(20))), None)
    assertEquals(refused(valid.copy(outcomes = outcomes(21))), Some("outcomes"))
    assertEquals(
      refused(valid.copy(outcomes = Vector(OutcomeId("yes"), OutcomeId("yes")))),
      Some("outcomes")
    )
    assertEquals(
      refused(valid.copy(outcomes = Vector(OutcomeId("yes"), OutcomeId("n" * 101)))),
      Some("outcomes")
    )
    assertEquals(
      refused(valid.copy(outcomes = Vector(OutcomeId("yes"), OutcomeId("")))),
      Some("outcomes")
    )
  }

  test("the close is after the open time, or after the command's time when there is none") {
    val opens = Instant.parse("2027-01-01T00:00:00Z")
    assertEquals(refused(valid.copy(openTime = Some(opens), close = opens)), Some("close"))
    assertEquals(refused(valid.copy(openTime = Some(opens), close = opens.plusSeconds(1))), None)
    assertEquals(refused(valid.copy(close = at)), Some("close"))
  }

  test("the expected resolution date is no earlier than the close's date in UTC") {
    assertEquals(refused(valid.copy(expectedResolution = LocalDate.parse("2027-05-24"))), None)
    assertEquals(
      refused(valid.copy(expectedResolution = LocalDate.parse("2027-05-23"))),
      Some("expected resolution date")
    )
  }

  test("the resolution criteria are 1 to 2,000 and the authority 1 to 200") {
    assertEquals(refused(valid.copy(criteria = "x" * 2_001)), Some("resolution criteria"))
    assertEquals(refused(valid.copy(authority = "")), Some("authority"))
    assertEquals(refused(valid.copy(authority = "x" * 201)), Some("authority"))
  }

  private def market(state: MarketState) =
    Market(MarketId("m-1"), Some(valid), state, lastTime = Some(at))

  test("a change touches only what the state allows, and something must change") {
    val criteria = valid.copy(criteria = "The table as published")
    assertEquals(Drafting.change(market(MarketState.Draft), criteria, at), None)
    assertEquals(Drafting.change(market(MarketState.Approved), criteria, at), None)
    assertEquals(
      Drafting.change(market(MarketState.Approved), valid.copy(question = "Will it rain?"), at),
      Some(Refusals.state(MarketState.Approved))
    )
    assertEquals(
      Drafting.change(market(MarketState.Open), criteria, at),
      Some(Refusals.state(MarketState.Open))
    )
    assertEquals(
      Drafting.change(market(MarketState.Closed), valid.copy(description = "More."), at),
      None
    )
    assertEquals(
      Drafting.change(market(MarketState.Final), valid.copy(description = "More."), at),
      Some(Refusals.state(MarketState.Final))
    )
    assertEquals(Drafting.change(market(MarketState.Draft), valid, at), Some(Refusals.noChange))
  }

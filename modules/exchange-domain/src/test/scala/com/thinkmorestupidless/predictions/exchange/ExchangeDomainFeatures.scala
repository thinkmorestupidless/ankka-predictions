package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

import java.time.Instant

/**
 * The exchange's scenarios of `features/libraries/domain.feature`, against the value alone: no
 * runtime, no clock. The market's scenario is the markets domain's suite's; those that need a rule
 * of spec 005 (matching, the price check, a trade) wait on it.
 */
class ExchangeDomainFeatures extends GherkinSuite("features/libraries/domain.feature"):

  override protected def ranElsewhere: Map[String, String] = Map(
    "a market rebuilt from its events alone is the market that produced them" ->
      "MarketsDomainFeatures",
    "a book rebuilt from its events alone is the book that produced them" ->
      "spec 005, when matching is built",
    "a refused command leaves the book it was applied to unchanged" ->
      "spec 005, when the price check is built",
    "every event that moves money names the amount, the owner and what it is keyed by" ->
      "spec 005, when matching is built"
  )

  private val day = "2026-10-31"

  private var exchange: Exchange                = Exchange.empty
  private var answer: Either[Refusal, Decision] = Left(Refusal("nothing applied"))

  override def beforeEach(context: BeforeEach): Unit =
    exchange = Exchange.empty
    answer = Left(Refusal("nothing applied"))

  private def time(hhmm: String): Instant = Instant.parse(s"${day}T$hhmm:00Z")

  Given("an exchange {string} whose last event is at {string}") { (market: String, hhmm: String) =>
    val opened = Exchange(
      Exchange.empty,
      ExchangeCommand.Open(
        MarketId(market),
        Vector(OutcomeId("yes"), OutcomeId("no")),
        unit = 100,
        maxOrderCost = 100_000,
        maxPosition = 10_000,
        at = time(hhmm)
      )
    )
    exchange = opened.fold(r => fail(s"the exchange did not open: $r"), _.next)
    assertEquals(exchange.lastTime, Some(time(hhmm)))
  }

  When("an order carrying the time {string} is applied to the exchange") { (hhmm: String) =>
    answer = Exchange(
      exchange,
      ExchangeCommand.Place(
        OrderId("o-1"),
        OwnerId("ada"),
        OutcomeId("yes"),
        Side.Buy,
        price = 60,
        quantity = 10,
        duration = OrderDuration.RestUntilClose,
        at = time(hhmm)
      )
    )
  }

  Then("the command is refused as out of order") { () =>
    assertEquals(answer.left.map(_.reason), Left("out of order"))
  }

  Then("the exchange is unchanged") { () =>
    // A refusal carries no next value: the exchange held here is the one the command was applied to.
    assert(answer.isLeft, s"the command was accepted: $answer")
  }

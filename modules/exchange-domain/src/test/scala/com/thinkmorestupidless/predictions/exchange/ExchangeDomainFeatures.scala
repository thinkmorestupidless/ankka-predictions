package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/**
 * The exchange's scenarios of `features/libraries/domain.feature`, against the value alone: no
 * runtime, no clock. The market's scenario is the markets domain's suite's.
 */
class ExchangeDomainFeatures extends ExchangeSteps("features/libraries/domain.feature"):

  override protected def ranElsewhere: Map[String, String] = Map(
    "a market rebuilt from its events alone is the market that produced them" ->
      "MarketsDomainFeatures"
  )

  private val words = Map("three" -> 3, "six" -> 6, "twenty" -> 20)

  private def count(word: String): Int = words.getOrElse(word, fail(s"no number '$word'"))

  private var rebuilt: Exchange = Exchange.empty

  private def time(hhmm: String): Instant = Instant.parse(s"2026-10-31T$hhmm:00Z")

  Given("an exchange {string} that has accepted {string} orders and made {string} trades") {
    (m: String, orders: String, trades: String) =>
      world.open(m, Vector("yes", "no"), 100)
      world.seedHolding(m, "bob", 100)
      world.seedHolding(m, "cy", 100)
      // Six sells met by six buys, one trade each; then eight orders that meet nothing.
      (1 to 6).foreach { i =>
        accepted(world.place(m, "bob", Side.Sell, s"s-$i", 10, "yes", 49L + i), s"s-$i")
        accepted(world.place(m, "ada", Side.Buy, s"b-$i", 10, "yes", 60), s"b-$i")
      }
      (1 to 4).foreach { i =>
        accepted(world.place(m, "ada", Side.Buy, s"r-$i", 5, "yes", 9L + i), s"r-$i")
        accepted(world.place(m, "cy", Side.Sell, s"q-$i", 5, "yes", 89L + i), s"q-$i")
      }
      val x = world.value(m)
      assertEquals(x.orders.size, count(orders))
      assertEquals(
        world.events(m).count(_.isInstanceOf[ExchangeEvent.TradeMade]),
        count(trades)
      )
  }

  When("the exchange's events are replayed onto an empty exchange in sequence") { () =>
    world.acted = true
    rebuilt = world.events("m-1").foldLeft(Exchange.empty)(Exchange.replay)
  }

  Then("the rebuilt exchange equals the exchange that produced them") { () =>
    assertEquals(rebuilt, world.value("m-1"))
  }

  Then("the rebuilt book of {string} in {string} equals the book that produced them") {
    (outcome: String, m: String) =>
      val book = world.value(m).books.get(OutcomeId(outcome))
      assert(
        book.exists(b => b.buys.nonEmpty && b.sells.nonEmpty),
        s"an empty book proves nothing: $book"
      )
      assertEquals(rebuilt.books.get(OutcomeId(outcome)), book)
  }

  Given("an exchange {string} with {string} resting orders in the book of {string}") {
    (m: String, orders: String, outcome: String) =>
      world.open(m, Vector("yes", "no"), 100)
      world.seedHolding(m, "bob", 100)
      val placed = Vector(("ada", Side.Buy, 50L), ("ada", Side.Buy, 51L), ("bob", Side.Sell, 70L))
      placed.zipWithIndex.foreach { case ((owner, side, price), i) =>
        accepted(world.place(m, owner, side, s"o-$i", 10, outcome, price), s"o-$i")
      }
      assertEquals(world.value(m).orders.size, count(orders))
  }

  When("a buy order at price {string} is applied to the exchange") { (price: String) =>
    world.acted = true
    world.place("m-1", "ada", Side.Buy, "o-100", 10, "yes", n(price)): Unit
  }

  Then("the exchange is unchanged") { () =>
    assert(world.last.exists(_.isLeft), s"the command was accepted: ${world.last}")
    assertEquals(Some(world.value("m-1")), world.before)
  }

  Given("an exchange {string} whose last event is at {string}") { (m: String, hhmm: String) =>
    world.open(m, Vector("yes", "no"), 100, at = Some(time(hhmm)))
    assertEquals(world.value(m).lastTime, Some(time(hhmm)))
  }

  When("an order carrying the time {string} is applied to the exchange") { (hhmm: String) =>
    world.acted = true
    world.apply(
      "m-1",
      ExchangeCommand.Place(
        OrderId("o-1"),
        OwnerId("ada"),
        OutcomeId("yes"),
        Side.Buy,
        60,
        10,
        OrderDuration.RestUntilClose,
        time(hhmm)
      )
    ): Unit
  }

  Then("the command is refused as out of order")(() => assertEquals(refusal.reason, "out of order"))

  private def leg(owner: String): (ExchangeEvent.TradeMade, Leg) =
    world.trades
      .flatMap(t => t.legs.map(t -> _))
      .find(_._2.owner == OwnerId(owner))
      .getOrElse(fail(s"no trade leg of $owner"))

  Then(
    "the trade event names {string} charged {string} keyed by the order {string} and the trade"
  ) { (owner: String, amount: String, order: String) =>
    val (t, l) = leg(owner)
    assertEquals((l.charge, l.order, t.trade), (n(amount), OrderId(order), TradeId.of(t.sequence)))
  }

  Then("the trade event names {string} paid {string} keyed by the trade") {
    (owner: String, amount: String) =>
      val (t, l) = leg(owner)
      assertEquals((l.paid, t.trade), (n(amount), TradeId.of(t.sequence)))
  }

  Then("the trade event names {string} released {string} keyed by the order {string}") {
    (owner: String, amount: String, order: String) =>
      val (_, l) = leg(owner)
      assertEquals((l.released, l.order), (n(amount), OrderId(order)))
      assert(l.releaseOrdinal > 0, s"a release under no ordinal: $l")
  }

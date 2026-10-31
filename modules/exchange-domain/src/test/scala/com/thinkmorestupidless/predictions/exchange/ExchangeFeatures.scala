package com.thinkmorestupidless.predictions.exchange

/**
 * `features/exchange/`: every scenario, against the value alone. The steps are `ExchangeSteps`; the
 * determinism scenario's are here, since only `matching.feature` has them.
 */
class ExchangeFeatures extends ExchangeSteps("features/exchange"):

  private var replayed: Vector[ExchangeEvent] = Vector.empty

  Given(
    "the orders {string}, {string}, {string} and {string} were placed in that order and made the trades {string} and {string}"
  ) { (s1: String, s2: String, b1: String, b2: String, t1: String, t2: String) =>
    accepted(world.place("m-1", "bob", Side.Sell, s1, 10, "yes", 55), s1)
    accepted(world.place("m-1", "cy", Side.Sell, s2, 10, "yes", 58), s2)
    accepted(world.place("m-1", "ada", Side.Buy, b1, 10, "yes", 60), b1)
    accepted(world.place("m-1", "ada", Side.Buy, b2, 10, "yes", 60), b2)
    assertEquals(trades(world.events("m-1")).map(_.trade.value), Vector(t1, t2))
  }

  When("the same orders are placed again in the same order on a fresh exchange") { () =>
    world.acted = true
    replayed = world
      .commands("m-1")
      .foldLeft((Exchange.empty, Vector.empty[ExchangeEvent])) { case ((x, events), c) =>
        val d =
          Exchange(x, c).fold(r => fail(s"$c was refused on the fresh exchange: $r"), identity)
        (d.next, events ++ d.events)
      }
      ._2
  }

  Then("the trades made are the same as {string} and {string} in price, quantity and orders") {
    (t1: String, t2: String) =>
      def shape(e: Vector[ExchangeEvent]) =
        trades(e)
          .filter(t => Set(t1, t2).contains(t.trade.value))
          .map(t => (t.trade, t.prices, t.quantity, t.legs.map(_.order)))
      assertEquals(shape(replayed), shape(world.events("m-1")))
      assertEquals(shape(replayed).size, 2)
  }

  private def trades(events: Vector[ExchangeEvent]): Vector[ExchangeEvent.TradeMade] =
    events.collect { case t: ExchangeEvent.TradeMade => t }

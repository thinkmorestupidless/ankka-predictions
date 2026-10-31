package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

import java.time.Instant

/**
 * The market's scenario of `features/libraries/domain.feature`, against the value alone; the
 * exchange's scenarios are the exchange domain's suite's.
 */
class MarketsDomainFeatures extends GherkinSuite("features/libraries/domain.feature"):

  override protected def ranElsewhere: Map[String, String] =
    Pending.all("features/libraries/domain.feature", "ExchangeDomainFeatures") -
      "a market rebuilt from its events alone is the market that produced them"

  private val world           = MarketsWorld()
  private var rebuilt: Market = Market.empty

  Given("a market {string} that has been approved, opened, closed and resolved") { (id: String) =>
    world.resolved(id, "yes", Instant.parse("2027-05-25T10:00:00Z"))
    val states = world.events(id).map(_.getClass.getSimpleName)
    assertEquals(states, Vector("Drafted", "Approved", "Opened", "Closed", "Resolved"))
  }

  When("the market's events are replayed onto an empty market in sequence") { () =>
    rebuilt = world.events(world.current).foldLeft(Market.empty)(Market.replay)
  }

  Then("the rebuilt market equals the market that produced them") { () =>
    assertEquals(rebuilt, world.value(world.current))
    assertEquals(rebuilt.state, MarketState.Resolved)
  }

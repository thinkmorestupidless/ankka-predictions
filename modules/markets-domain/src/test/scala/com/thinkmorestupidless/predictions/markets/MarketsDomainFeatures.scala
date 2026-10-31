package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

/**
 * The market's scenario of `features/libraries/domain.feature`. It needs a resolved market, which
 * is spec 006's rule; the exchange's scenarios are the exchange domain's suite's.
 */
class MarketsDomainFeatures extends GherkinSuite("features/libraries/domain.feature"):

  override protected def ranElsewhere: Map[String, String] =
    Pending.all("features/libraries/domain.feature", "ExchangeDomainFeatures") ++ Map(
      "a market rebuilt from its events alone is the market that produced them" ->
        "spec 006, when resolution is built"
    )

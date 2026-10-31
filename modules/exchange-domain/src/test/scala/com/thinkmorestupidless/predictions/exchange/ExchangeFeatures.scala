package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

/**
 * `features/exchange/`: every scenario is a rule of spec 005's, run against the value alone once
 * those rules are built. Until then each is reported ignored, naming spec 005.
 */
class ExchangeFeatures extends GherkinSuite("features/exchange"):

  override protected def ranElsewhere: Map[String, String] =
    Pending.all("features/exchange", "spec 005, when its rules are built")

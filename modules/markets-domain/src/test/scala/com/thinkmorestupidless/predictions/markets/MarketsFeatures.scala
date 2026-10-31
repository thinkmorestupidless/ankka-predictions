package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

/**
 * `features/markets/`: every scenario is a rule of spec 006's, run against the value alone once
 * those rules are built. Until then each is reported ignored, naming spec 006.
 */
class MarketsFeatures extends GherkinSuite("features/markets"):

  override protected def ranElsewhere: Map[String, String] =
    Pending.all("features/markets", "spec 006, when its rules are built")

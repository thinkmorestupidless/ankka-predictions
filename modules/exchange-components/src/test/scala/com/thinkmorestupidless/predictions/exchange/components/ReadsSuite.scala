package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.Serializer
import com.thinkmorestupidless.predictions.exchange.*

/**
 * The depth read's JSON, which a consumer's endpoint answers with: unchanged by the domain's
 * `Level`.
 */
class ReadsSuite extends munit.FunSuite:

  test("a depth is written as it was before its levels came from the domain") {
    val depth = Depth(OutcomeId("yes"), Vector(Level(52, 10)), Vector(Level(56, 20)), 7)
    assertEquals(
      String(summon[Serializer[Depth]].toBytes(depth), "UTF-8"),
      """{"outcome":"yes","buys":[{"price":52,"quantity":10}],"sells":[{"price":56,"quantity":20}],"sequence":7}"""
    )
  }

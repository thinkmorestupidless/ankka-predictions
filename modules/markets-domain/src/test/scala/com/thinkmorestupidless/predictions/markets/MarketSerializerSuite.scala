package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.core.Serializer

import java.time.Instant

/** A market and its events round-trip through their serializers, under their manifests. */
class MarketSerializerSuite extends munit.FunSuite:

  test("a market and its events round-trip through their serializers") {
    val world = MarketsWorld()
    world.resolved("m-5", "yes", Instant.parse("2027-05-25T10:00:00Z"))
    val market = world.value("m-5")
    val s      = summon[Serializer[Market]]
    assertEquals(s.fromBytes(s.toBytes(market)), market)
    assertEquals(s.manifest, "market")
    val e = summon[Serializer[MarketEvent]]
    world.events("m-5").foreach(event => assertEquals(e.fromBytes(e.toBytes(event)), event))
    assertEquals(e.manifest, "market-event")
  }

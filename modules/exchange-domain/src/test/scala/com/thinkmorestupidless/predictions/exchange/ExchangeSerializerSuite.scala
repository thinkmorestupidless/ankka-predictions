package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.core.Serializer

import java.time.Instant

/**
 * What spec 004 builds of the exchange domain, which the components start from: an exchange opens
 * once, a buy's cost is quantity times price, every other rule is refused as not built until spec
 * 005, and the value and its events cross their serializers unchanged.
 */
class ExchangeStubSuite extends munit.FunSuite:

  private val t0 = Instant.parse("2026-10-31T10:00:00Z")

  private val open: ExchangeCommand.Open = ExchangeCommand.Open(
    MarketId("m-1"),
    Vector(OutcomeId("yes"), OutcomeId("no")),
    unit = 100,
    maxOrderCost = 100_000,
    maxPosition = 10_000,
    at = t0
  )

  private def opened: Decision =
    Exchange(Exchange.empty, open).fold(r => fail(r.toString), identity)

  test(
    "an empty exchange opens with its outcomes, unit and limits, and replays to the same value"
  ) {
    val d = opened
    assertEquals(d.next.state, ExchangeState.Open)
    assertEquals(d.next.books.keySet.toVector, Vector(OutcomeId("no"), OutcomeId("yes")))
    assertEquals(d.events.foldLeft(Exchange.empty)(Exchange.replay), d.next)
  }

  test("an open exchange is not opened again") {
    assertEquals(
      Exchange(opened.next, open.copy(at = t0.plusSeconds(1))),
      Left(Refusal("state", Map("state" -> "open")))
    )
  }

  test("a buy's cost is its quantity times its price") {
    assertEquals(Exchange.cost(10, 60), 600L)
  }

  test("a rule spec 005 has not built is refused as not built, naming the command") {
    val cancel = ExchangeCommand.Cancel(OrderId("o-1"), OwnerId("ada"), t0)
    assertEquals(
      Exchange(opened.next, cancel),
      Left(Refusal("not built", Map("command" -> "cancel")))
    )
  }

  test("the value and every event case cross their serializers unchanged") {
    val ev = summon[Serializer[ExchangeEvent]]
    val x  = summon[Serializer[Exchange]]
    assertEquals((x.manifest, ev.manifest), ("exchange", "exchange-event"))
    assertEquals(x.fromBytes(x.toBytes(opened.next)), opened.next)
    Samples.everyEvent(t0).foreach(e => assertEquals(ev.fromBytes(ev.toBytes(e)), e, e.toString))
    val closed = new String(ev.toBytes(ExchangeEvent.Closed(t0)), "UTF-8")
    assertEquals(closed, """{"type":"Closed","at":"2026-10-31T10:00:00Z"}""")
  }

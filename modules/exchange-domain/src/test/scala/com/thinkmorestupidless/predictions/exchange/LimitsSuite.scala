package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/** The limits at the edge of a `Long`: a quantity whose cost or position would wrap is refused. */
class LimitsSuite extends munit.FunSuite:

  private val t   = Instant.parse("2026-10-31T10:00:00Z")
  private val ada = OwnerId("ada")
  private val yes = OutcomeId("yes")

  private val opened: Exchange =
    val open: ExchangeCommand =
      ExchangeCommand.Open(MarketId("m-1"), Vector(yes, OutcomeId("no")), 100, 100_000, 10_000, t)
    val split: ExchangeCommand = ExchangeCommand.Split(SetKey("k-1"), ada, 1, t)
    List(open, split).foldLeft(Exchange.empty)((x, c) =>
      Exchange(x, c).fold(r => fail(r.toString), _.next)
    )

  private def buy(quantity: Long, price: Long): ExchangeCommand =
    ExchangeCommand.Place(
      OrderId("o-1"),
      ada,
      yes,
      Side.Buy,
      price,
      quantity,
      OrderDuration.RestUntilClose,
      t
    )

  test("a buy whose cost would wrap is refused naming the maximum order cost") {
    assertEquals(
      Exchange(opened, buy(Long.MaxValue, 2)),
      Left(Refusals.limit(Refusals.maximumOrderCost))
    )
  }

  test("a buy whose position would wrap is refused naming the maximum position") {
    val unbounded: ExchangeCommand =
      ExchangeCommand.Open(
        MarketId("m-2"),
        Vector(yes, OutcomeId("no")),
        100,
        Long.MaxValue,
        10_000,
        t
      )
    val seeded: ExchangeCommand = ExchangeCommand.Split(SetKey("k-1"), ada, 6, t)
    // Holding 6, a buy of Long.MaxValue - 5 at 1 is within the maximum order cost, and
    // 6 + Long.MaxValue - 5 wraps below the maximum position.
    val x = List(unbounded, seeded).foldLeft(Exchange.empty)((x, c) =>
      Exchange(x, c).fold(r => fail(r.toString), _.next)
    )
    assertEquals(
      Exchange(x, buy(Long.MaxValue - 5, 1)),
      Left(Refusals.limit(Refusals.maximumPosition))
    )
  }

  test("a split whose position would wrap is refused naming the maximum position") {
    assertEquals(
      Exchange(opened, ExchangeCommand.Split(SetKey("k-2"), ada, Long.MaxValue, t)),
      Left(Refusals.limit(Refusals.maximumPosition))
    )
  }

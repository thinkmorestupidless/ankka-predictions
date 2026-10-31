package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/**
 * Spec 005 SC-003 (research R12): a place on an exchange of 10,000 resting orders is answered in
 * under 1 ms at the median, against the value alone. Both kinds are timed: one that rests and one
 * that trades with a resting sell.
 */
class ExchangePerfSuite extends munit.FunSuite:

  private val t0 = Instant.parse("2026-10-31T10:00:00Z")
  private var at = 0L

  private def place(
      x: Exchange,
      n: Int,
      owner: String,
      outcome: String,
      side: Side,
      price: Long
  ): Exchange =
    at += 1
    val c = ExchangeCommand.Place(
      OrderId(s"o-$n"),
      OwnerId(owner),
      OutcomeId(outcome),
      side,
      price,
      1,
      OrderDuration.RestUntilClose,
      t0.plusMillis(at)
    )
    Exchange(x, c).fold(r => fail(s"o-$n: $r"), _.next)

  private def median(xs: Seq[Double]): Double = xs.sorted.apply(xs.size / 2)

  test("a place on an exchange of 10,000 resting orders takes under 1 ms at the median") {
    val open = ExchangeCommand.Open(
      MarketId("perf"),
      Vector(OutcomeId("yes"), OutcomeId("no")),
      100,
      1_000_000,
      1_000_000,
      t0
    )
    var x = Exchange(Exchange.empty, open).fold(r => fail(r.toString), _.next)
    // A seller of yes, for the matching places.
    x = Exchange(x, ExchangeCommand.Split(SetKey("seed"), OwnerId("seller"), 10_000, t0))
      .fold(r => fail(r.toString), _.next)
    // Buys of both outcomes at 10 to 39: the two never make one unit, and no sell is below 40.
    (0 until 11_000).foreach { n =>
      x =
        place(x, n, s"owner-${n % 200}", if n % 2 == 0 then "yes" else "no", Side.Buy, 10L + n % 30)
    }
    val resting = (11_000 until 11_200).map { n =>
      val start = System.nanoTime()
      x = place(x, n, s"owner-${n % 200}", "yes", Side.Buy, 10L + n % 30)
      (System.nanoTime() - start) / 1e6
    }
    val matching = (11_200 until 11_400).map { n =>
      x = place(x, n, "seller", "yes", Side.Sell, 90)
      val start = System.nanoTime()
      x = place(x, n + 1_000, s"owner-${n % 200}", "yes", Side.Buy, 95)
      (System.nanoTime() - start) / 1e6
    }
    println(
      f"resting place median ${median(resting)}%.3f ms; matching place median ${median(matching)}%.3f ms"
    )
    assert(median(resting) < 1.0, s"a resting place's median was ${median(resting)} ms")
    assert(median(matching) < 1.0, s"a matching place's median was ${median(matching)} ms")
  }

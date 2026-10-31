package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/**
 * Spec 005 SC-003 (research R12): a place on an exchange of 10,000 resting orders is answered in
 * under 1 ms at the median, against the value alone. Three kinds are timed: one that rests, one
 * that trades with a resting sell of its own outcome, and one that trades through the pool.
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
  ): Exchange = decide(x, n, owner, outcome, side, price).next

  private def decide(
      x: Exchange,
      n: Int,
      owner: String,
      outcome: String,
      side: Side,
      price: Long
  ): Decision =
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
    Exchange(x, c).fold(r => fail(s"o-$n: $r"), identity)

  private def through(d: Decision): Vector[Through] =
    d.events.collect { case t: ExchangeEvent.TradeMade => t.through }

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
    // Buys of yes at 10 to 39 and of no at 1 to 4: a buy of yes at 95 meets no buy of no that
    // makes a unit with it, so it trades with the sell at 90 directly.
    (0 until 11_000).foreach { n =>
      x =
        if n % 2 == 0 then place(x, n, s"owner-${n % 200}", "yes", Side.Buy, 10L + n % 30)
        else place(x, n, s"owner-${n % 200}", "no", Side.Buy, 1L + n % 4)
    }
    val resting = (11_000 until 11_200).map { n =>
      val start = System.nanoTime()
      x = place(x, n, s"owner-${n % 200}", "yes", Side.Buy, 10L + n % 30)
      (System.nanoTime() - start) / 1e6
    }
    val matching = (11_200 until 11_400).map { n =>
      x = place(x, n, "seller", "yes", Side.Sell, 90)
      val start = System.nanoTime()
      val d     = decide(x, n + 1_000, s"owner-${n % 200}", "yes", Side.Buy, 95)
      val took  = (System.nanoTime() - start) / 1e6
      assertEquals(through(d), Vector(Through.Direct), s"o-${n + 1_000}")
      x = d.next
      took
    }
    // A buy of no at 45 against a resting buy of yes at 60: one unit and more, through the pool.
    val pooled = (11_400 until 11_600).map { n =>
      x = place(x, n, s"owner-${n % 200}", "yes", Side.Buy, 60)
      val start = System.nanoTime()
      val d     = decide(x, n + 1_000, s"owner-${(n + 1) % 200}", "no", Side.Buy, 45)
      val took  = (System.nanoTime() - start) / 1e6
      assertEquals(through(d), Vector(Through.Split), s"o-${n + 1_000}")
      x = d.next
      took
    }
    println(
      f"resting place median ${median(resting)}%.3f ms; matching place median ${median(matching)}%.3f ms; pool place median ${median(pooled)}%.3f ms"
    )
    assert(median(resting) < 1.0, s"a resting place's median was ${median(resting)} ms")
    assert(median(matching) < 1.0, s"a matching place's median was ${median(matching)} ms")
    assert(median(pooled) < 1.0, s"a pool place's median was ${median(pooled)} ms")
  }

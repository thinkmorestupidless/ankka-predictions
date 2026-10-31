package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.EntityId
import com.thinkmorestupidless.ankka.runtime.ProjectionRuntime
import com.thinkmorestupidless.ankka.testkit.AnkkaTestKit
import com.thinkmorestupidless.predictions.exchange.*

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt

/**
 * Spec 004 SC-002's entity half: `place` through the entity on an exchange of 10,000 resting orders
 * takes 10 ms at the median on a laptop. It needs an order to rest, which is spec 005's; until then
 * the domain refuses `place` as not built and the test is skipped, saying so.
 */
class ExchangePerfSuite extends munit.FunSuite:

  override val munitTimeout = 15.minutes

  private val components =
    ExchangeComponents(ExchangePorts(ScriptedLedger(), ScriptedGate(), Duration.ofSeconds(1)))

  private var kit: AnkkaTestKit = null

  override def beforeAll(): Unit =
    kit = AnkkaTestKit.start(components.descriptors, Seq(ProjectionRuntime()))

  override def afterAll(): Unit = if kit != null then kit.stop()

  private val m        = MarketId("perf-1")
  private def exchange = kit.componentClient.forEventSourcedEntity(EntityId(m.value))

  private def order(n: Int, side: Side, price: Long): ExchangeCommand.Place =
    ExchangeCommand.Place(
      OrderId(s"o-$n"),
      OwnerId(s"owner-${n % 100}"),
      OutcomeId("yes"),
      side,
      price,
      1,
      OrderDuration.RestUntilClose,
      Instant.now()
    )

  test("a place on an exchange of 10,000 resting orders takes 10 ms at the median") {
    exchange
      .call(ExchangeEntity.open)
      .invoke(
        ExchangeCommand.Open(
          m,
          Vector(OutcomeId("yes"), OutcomeId("no")),
          100,
          1_000_000,
          1_000_000,
          Instant.now()
        )
      ): Unit
    val built =
      try { exchange.call(ExchangeEntity.place).invoke(order(0, Side.Buy, 10)): Unit; true }
      catch case Refused(r) if r == Refusals.notBuilt("place") => false
    assume(built, "pending on spec 005: the domain cannot rest an order yet")

    // Buys from 10 to 49, never meeting a sell, so every one rests.
    (1 until 10_000).foreach(n =>
      exchange.call(ExchangeEntity.place).invoke(order(n, Side.Buy, 10 + n % 40))
    )
    val times = (10_000 until 10_200).map { n =>
      val start = System.nanoTime()
      exchange.call(ExchangeEntity.place).invoke(order(n, Side.Buy, 10 + n % 40))
      (System.nanoTime() - start) / 1_000_000.0
    }.sorted
    val median = times(times.size / 2)
    assert(median <= 10.0, s"the median place took $median ms")
  }

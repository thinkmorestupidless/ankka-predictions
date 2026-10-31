package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.EntityId
import com.thinkmorestupidless.ankka.runtime.{ProjectionRuntime, TimerRuntime}
import com.thinkmorestupidless.ankka.testkit.AnkkaTestKit
import com.thinkmorestupidless.predictions.markets.*

import java.time.Instant
import scala.concurrent.duration.DurationInt

/**
 * What the markets components do on their own time: an approved market's timer is set by the
 * `market-schedule` consumer with the consumer's scheduler (research V5) and opens it when due; the
 * catalogue lists markets by state.
 */
class MarketsComponentsSuite extends munit.FunSuite:

  override val munitTimeout = 5.minutes

  private val resolution = ScriptedResolution()
  private val timers     = TimerRuntime(pollInterval = 200.millis)
  private val components = MarketsComponents(MarketPorts(resolution, () => timers.timerScheduler))

  private var kit: AnkkaTestKit = null

  override def beforeAll(): Unit =
    kit = AnkkaTestKit.start(components.descriptors, Seq(ProjectionRuntime(), timers))

  override def afterAll(): Unit = if kit != null then kit.stop()

  private def entity(m: MarketId) = kit.componentClient.forEventSourcedEntity(EntityId(m.value))

  private def drafted(m: MarketId, openTime: Option[Instant]): Unit =
    val now = Instant.now()
    entity(m)
      .call(components.market.draft)
      .invoke(
        MarketCommand
          .Draft(m, OperatorId("ops"), Samples.definition("yes", "no", "USD", now, openTime), now)
      ): Unit
    entity(m)
      .call(components.market.approve)
      .invoke(MarketCommand.Approve(OperatorId("lead"), now)): Unit

  test("an approved market opens by its timer at its open time") {
    val m = MarketId("timed-1")
    drafted(m, Some(Instant.now().plusSeconds(2)))
    assertEquals(entity(m).call(components.market.get).invoke().state, MarketState.Approved)
    val opened = kit.eventually("the market opened", 30.seconds)(
      Option(entity(m).call(components.market.get).invoke()).filter(_.state == MarketState.Open)
    )
    assertEquals(opened.record.map(_.what).last, "opened")
  }

  test("the market's timer is set for its next due time and moves with it") {
    val m = MarketId("timed-2")
    drafted(m, Some(Instant.now().plusSeconds(3600)))
    kit.eventually("the timer was set", 30.seconds)(
      Option.when(timers.timerScheduler.exists(MarketTimers.timer(m)))(())
    )
  }

  test("a refusal by the resolution port asks first and persists nothing") {
    val m = MarketId("asked-1")
    resolution.answer(OperatorId("mal"), ResolutionAnswer.Refused("not an operator"))
    val now = Instant.now()
    val error = intercept[com.thinkmorestupidless.ankka.core.CommandError](
      entity(m)
        .call(components.market.draft)
        .invoke(
          MarketCommand
            .Draft(m, OperatorId("mal"), Samples.definition("yes", "no", "USD", now), now)
        )
    )
    assertEquals(Refused.unapply(error).map(_.reason), Some("not an operator"))
    assertEquals(entity(m).call(components.market.get).invoke(), Market.empty)
  }

  test("the catalogue lists markets by state and by category") {
    val m = MarketId("listed-1")
    drafted(m, None)
    val rows = kit.service.viewClient.forView(components.catalogue)
    val open = kit.eventually("the market is listed open", 30.seconds)(
      Option(rows.ask(CatalogueRows.byState, "state" -> "open")).filter(_.exists(_.market == m))
    )
    assert(open.forall(_.state == MarketState.Open))
    assert(rows.ask(CatalogueRows.byCategory, "category" -> "events").exists(_.market == m))
  }

package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.EntityId
import com.thinkmorestupidless.ankka.runtime.ProjectionRuntime
import com.thinkmorestupidless.ankka.testkit.AnkkaTestKit
import com.thinkmorestupidless.predictions.exchange.*

import java.time.{Duration, Instant}
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.duration.DurationInt

/**
 * The placement workflow's patience and its crashes (research R7, R8). Until spec 005 the exchange
 * refuses every order as not built, so a buy that reaches it is released under ordinal 0: what each
 * test can see is how far the order got, what the ledger was asked, and that nothing rests.
 */
class PlacementSuite extends munit.FunSuite:

  override val munitTimeout = 5.minutes

  private val ledger     = ScriptedLedger()
  private val gate       = ScriptedGate()
  private val components = ExchangeComponents(ExchangePorts(ledger, gate, Duration.ofSeconds(1)))
  private val markets    = AtomicInteger()

  private var kit: AnkkaTestKit = null

  override def beforeAll(): Unit =
    kit = AnkkaTestKit.start(components.descriptors, Seq(ProjectionRuntime()))

  override def afterAll(): Unit = if kit != null then kit.stop()

  private def openMarket(): MarketId =
    val m = MarketId(s"p-${markets.incrementAndGet()}")
    exchange(m)
      .call(ExchangeEntity.open)
      .invoke(
        ExchangeCommand
          .Open(m, Vector(OutcomeId("yes"), OutcomeId("no")), 100, 100_000, 10_000, Instant.now())
      ): Unit
    m

  private def exchange(m: MarketId) = kit.componentClient.forEventSourcedEntity(EntityId(m.value))

  private def buy(m: MarketId, owner: String, order: String): PlaceOrder =
    PlaceOrder(
      m,
      ExchangeCommand.Place(
        OrderId(order),
        OwnerId(owner),
        OutcomeId("yes"),
        Side.Buy,
        60,
        10,
        OrderDuration.RestUntilClose,
        Instant.now()
      )
    )

  private def placed(request: PlaceOrder): PlacementOutcome =
    kit.componentClient
      .forWorkflow(PlacementWorkflow.id(request))
      .call(components.placement.place)
      .thenAwaitEnd(30.seconds)
      .invoke(request)
      .outcome

  private def startOnly(request: PlaceOrder): Unit =
    kit.componentClient
      .forWorkflow(PlacementWorkflow.id(request))
      .call(components.placement.place)
      .invoke(request): Unit

  private def ended(request: PlaceOrder): PlacementOutcome =
    kit.eventually("the placement ended", 30.seconds) {
      val now = kit.componentClient
        .forWorkflow(PlacementWorkflow.id(request))
        .call(components.placement.outcome)
        .invoke()
      Option.when(now != PlacementOutcome.Running)(now)
    }

  private def lifecycle(request: PlaceOrder) =
    kit.componentClient
      .forWorkflow(PlacementWorkflow.id(request))
      .lifecycle(components.placement)
      .invoke()

  private def ofMarket(m: MarketId, what: String): Vector[LedgerCall] =
    ledger.calls(what).filter(_.market == m)

  private def nothingRests(m: MarketId): Unit =
    assertEquals(exchange(m).call(ExchangeEntity.get).invoke().orders.size, 0)

  test("a buy the exchange refuses is released once, under ordinal 0") {
    val m = openMarket()
    assertEquals(placed(buy(m, "ada", "o-1")), PlacementOutcome.Refused(Refusals.notBuilt("place")))
    assertEquals(ofMarket(m, "reserve").map(c => (c.key, c.amount)), Vector("o-1" -> 600L))
    assertEquals(ofMarket(m, "release").map(c => (c.key, c.amount)), Vector("o-1/0" -> 600L))
    nothingRests(m)
  }

  test(
    "a ledger that throws within the patience is asked again, and the order reaches the exchange"
  ) {
    val m = openMarket()
    ledger.throwing(OwnerId("cid"), times = 2)
    // Reaching the exchange is all the stub domain allows: it refuses, so the reservation is released.
    assertEquals(placed(buy(m, "cid", "o-2")), PlacementOutcome.Refused(Refusals.notBuilt("place")))
    assertEquals(
      ofMarket(m, "reserve").map(_.answer),
      Vector(Left("threw"), Left("threw"), Right(LedgerAnswer.Done))
    )
    assertEquals(ofMarket(m, "release").map(_.key), Vector("o-2/0"))
    nothingRests(m)
  }

  test("a ledger that throws past the patience refuses the order as unavailable, with no event") {
    val m = openMarket()
    ledger.throwing(OwnerId("dee"), times = 1_000)
    assertEquals(placed(buy(m, "dee", "o-3")), PlacementOutcome.Refused(Refusals.ledgerUnavailable))
    assert(ofMarket(m, "reserve").nonEmpty)
    assert(ofMarket(m, "reserve").forall(_.answer == Left("threw")))
    assertEquals(ofMarket(m, "release"), Vector.empty)
    nothingRests(m)
  }

  test("a repeat of a placement is answered with what the first recorded, and asks nothing again") {
    val m       = openMarket()
    val request = buy(m, "eve", "o-4")
    val first   = placed(request)
    val asked   = ledger.calls.size
    assertEquals(placed(request), first)
    assertEquals(ledger.calls.size, asked)
  }

  test(
    "a placement that stopped while reserving resumes, and the reservation is a repeat, not a second"
  ) {
    val m       = openMarket()
    val request = buy(m, "fay", "o-5")
    ledger.hold("reserve")
    startOnly(request)
    assert(
      ledger.held("reserve"),
      s"no reserve arrived: ${ofMarket(m, "reserve") ++ ofMarket(m, "release")}; ${lifecycle(request)}"
    )
    kit.restartService()
    ledger.let("reserve")
    assertEquals(ended(request), PlacementOutcome.Refused(Refusals.notBuilt("place")))
    // Asked twice, the held call and the resumed step's, under one key: the second is a repeat.
    val reserves = ofMarket(m, "reserve")
    assertEquals(reserves.map(_.key), Vector("o-5", "o-5"))
    assert(reserves.forall(_.answer == Right(LedgerAnswer.Done)), s"answered otherwise: $reserves")
    assertEquals(ofMarket(m, "release").map(_.key).distinct, Vector("o-5/0"))
    nothingRests(m)
  }

  test(
    "a placement that stopped after the exchange answered ends with that answer, not a second ask"
  ) {
    val m       = openMarket()
    val request = buy(m, "gus", "o-6")
    ledger.hold("release")
    startOnly(request)
    assert(
      ledger.held("release"),
      s"no release arrived: ${ofMarket(m, "reserve") ++ ofMarket(m, "release")}; ${lifecycle(request)}"
    )
    kit.restartService()
    ledger.let("release")
    assertEquals(ended(request), PlacementOutcome.Refused(Refusals.notBuilt("place")))
    // Resumed at the release: the reservation was not asked again.
    assertEquals(ofMarket(m, "reserve").size, 1)
    assertEquals(ofMarket(m, "release").map(_.key), Vector("o-6/0", "o-6/0"))
  }

package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.EntityId
import com.thinkmorestupidless.ankka.runtime.{ProjectionRuntime, TimerRuntime}
import com.thinkmorestupidless.ankka.testkit.{AnkkaTestKit, GherkinSuite}
import com.thinkmorestupidless.predictions.exchange.*
import com.thinkmorestupidless.predictions.markets
import com.thinkmorestupidless.predictions.markets.components.{
  MarketPorts,
  MarketsComponents,
  ResolutionAnswer,
  ResolutionPort
}

import java.time.{Duration, Instant}
import scala.concurrent.duration.DurationInt

/**
 * The exchange's scenarios of `features/libraries/components.feature`, on a real service with a
 * throwaway Postgres and scripted ports (research R11). The resolution port's scenario is the
 * markets components' suite's.
 */
class ComponentsFeatures extends GherkinSuite("features/libraries/components.feature"):

  override val munitTimeout = 5.minutes

  override protected def ranElsewhere: Map[String, String] = Map(
    "a resolution the resolution port refuses changes nothing" -> "markets' ComponentsFeatures"
  )

  private val ledger     = ScriptedLedger()
  private val gate       = ScriptedGate()
  private val components = ExchangeComponents(ExchangePorts(ledger, gate, Duration.ofSeconds(2)))

  private var kit: AnkkaTestKit                        = null
  private var bare: Option[AnkkaTestKit]               = None
  private var outcome: Option[PlacementOutcome]        = None
  private var unregistered: Option[ExchangeComponents] = None

  override def beforeAll(): Unit =
    kit = AnkkaTestKit.start(components.descriptors, Seq(ProjectionRuntime()))

  override def afterAll(): Unit =
    bare.foreach(_.stop())
    if kit != null then kit.stop()

  override def beforeEach(context: BeforeEach): Unit =
    outcome = None

  /** A market id of this scenario's own, so scenarios sharing the journal do not meet. */
  private def market(name: String): MarketId = MarketId(s"$name-${scenarioId.hashCode.toHexString}")

  private def exchange(m: MarketId) = kit.componentClient.forEventSourcedEntity(EntityId(m.value))

  Given("an open market {string} with the outcomes {string} and {string} in {string}") {
    (name: String, first: String, second: String, _: String) =>
      val m = market(name)
      exchange(m)
        .call(ExchangeEntity.open)
        .invoke(
          ExchangeCommand.Open(
            m,
            Vector(OutcomeId(first), OutcomeId(second)),
            100,
            100_000,
            10_000,
            Instant.now()
          )
        ): Unit
  }

  Given("a player {string} with an active account")((_: String) => ())

  Given("a ledger port that allows {string}") { (owner: String) =>
    ledger.answer(OwnerId(owner), LedgerAnswer.Done)
  }

  Given("a gate port that allows {string}") { (owner: String) =>
    gate.answer(OwnerId(owner), GateAnswer.Allow)
  }

  Given("a ledger port that refuses {string} as insufficient") { (owner: String) =>
    ledger.answer(OwnerId(owner), LedgerAnswer.Insufficient)
  }

  Given("a gate port that refuses {string} with the reason {string}") {
    (owner: String, reason: String) =>
      gate.answer(OwnerId(owner), GateAnswer.Refused(reason))
  }

  When(
    "{string} places a buy order {string} for {string} shares of {string} in {string} at price {string}"
  ) {
    (
        owner: String,
        order: String,
        quantity: String,
        outcomeName: String,
        name: String,
        price: String
    ) =>
      val request = PlaceOrder(
        market(name),
        ExchangeCommand.Place(
          OrderId(order),
          OwnerId(owner),
          OutcomeId(outcomeName),
          Side.Buy,
          price.toLong,
          quantity.toLong,
          OrderDuration.RestUntilClose,
          Instant.now()
        )
      )
      val ended = kit.componentClient
        .forWorkflow(PlacementWorkflow.id(request))
        .call(components.placement.place)
        .thenAwaitEnd(30.seconds)
        .invoke(request)
      outcome = Some(ended.outcome)
  }

  Given("{string} holds {string} shares of {string} in {string}") {
    (owner: String, shares: String, _: String, name: String) =>
      // A split gives a share of every outcome, and is how a holding comes about with the pool exact.
      exchange(market(name))
        .call(ExchangeEntity.split)
        .invoke(
          ExchangeCommand
            .Split(SetKey(s"seed-$owner"), OwnerId(owner), shares.toLong, Instant.now())
        ): Unit
  }

  Given(
    "a resting sell order by {string} for {string} shares of {string} in {string} at price {string}"
  ) { (owner: String, quantity: String, outcomeName: String, name: String, price: String) =>
    exchange(market(name))
      .call(ExchangeEntity.place)
      .invoke(
        ExchangeCommand.Place(
          OrderId(s"sell-$owner"),
          OwnerId(owner),
          OutcomeId(outcomeName),
          Side.Sell,
          price.toLong,
          quantity.toLong,
          OrderDuration.RestUntilClose,
          Instant.now()
        )
      ): Unit
  }

  private def settled(trade: String): Vector[LedgerCall] =
    ledger.calls("settle").filter(c => c.market == market("m-1") && c.key == trade)

  Then("the ledger port is asked to settle the trade {string} keyed by {string}") {
    (trade: String, key: String) =>
      assertEquals(trade, key)
      val calls =
        kit.eventually(s"$trade settled", 30.seconds)(Option(settled(trade)).filter(_.nonEmpty))
      assertEquals(calls.map(c => (c.amount, c.answer)), Vector((600L, Right(LedgerAnswer.Done))))
  }

  Then(
    "asking the ledger port to settle the trade {string} keyed by {string} again is answered as the first was"
  ) { (trade: String, _: String) =>
    val first = settled(trade).head
    val again = ledger.settle(market("m-1"), TradeId(trade), Through.Direct, Vector.empty)
    assertEquals(Right(again), first.answer)
    assertEquals(settled(trade).size, 2)
  }

  Then("the order is refused as insufficient") { () =>
    assertEquals(outcome, Some(PlacementOutcome.Refused(Refusal("insufficient"))))
  }

  Then("the order is refused with the reason {string}") { (reason: String) =>
    assertEquals(outcome, Some(PlacementOutcome.Refused(Refusal(reason))))
  }

  Then("nothing rests in the book of {string} in {string}") { (outcomeName: String, name: String) =>
    val depth = exchange(market(name)).call(ExchangeEntity.depth).invoke(OutcomeId(outcomeName))
    assertEquals((depth.buys, depth.sells), (Vector.empty, Vector.empty))
    assertEquals(exchange(market(name)).call(ExchangeEntity.get).invoke().orders.size, 0)
  }

  Then("the ledger port was not asked to reserve") { () =>
    assertEquals(
      ledger.calls("reserve").filter(_.market.value.endsWith(scenarioId.hashCode.toHexString)),
      Vector.empty
    )
  }

  Given("a service that takes the components and registers none of them") { () =>
    unregistered = Some(ExchangeComponents(ExchangePorts(ledger, gate, Duration.ofSeconds(2))))
    val resolution = new ResolutionPort:
      def mayDraft(operator: markets.OperatorId)                        = ResolutionAnswer.Allow
      def mayApprove(operator: markets.OperatorId, m: markets.MarketId) = ResolutionAnswer.Allow
      def mayResolve(operator: markets.OperatorId, m: markets.MarketId) = ResolutionAnswer.Allow
      def mayVoid(operator: markets.OperatorId, m: markets.MarketId)    = ResolutionAnswer.Allow
      def mayClose(operator: markets.OperatorId, m: markets.MarketId)   = ResolutionAnswer.Allow
      def maySuspend(operator: markets.OperatorId, m: markets.MarketId) = ResolutionAnswer.Allow
    marketsComponents =
      Some(MarketsComponents(MarketPorts(resolution, () => TimerRuntime().timerScheduler)))
  }

  private var marketsComponents: Option[MarketsComponents] = None

  When("the service starts") { () =>
    bare = Some(AnkkaTestKit.start(Seq.empty, Seq(ProjectionRuntime())))
  }

  Then("no book and no market is served") { () =>
    val client = bare.getOrElse(fail("no service started")).componentClient
    val id     = EntityId("m-1")
    // A component the service never registered has no shard: the call fails naming it.
    def unserved(component: String)(call: => Any): Unit =
      val e = intercept[Exception](call)
      assert(e.getMessage.contains(s"[$component]"), s"the call failed otherwise: $e")
    val exchange = unregistered.getOrElse(fail("no exchange components built")).exchange
    unserved("exchange")(client.forEventSourcedEntity(id).call(exchange.get).invoke())
    val market = marketsComponents.getOrElse(fail("no markets components built")).market
    unserved("market")(client.forEventSourcedEntity(id).call(market.get).invoke())
  }

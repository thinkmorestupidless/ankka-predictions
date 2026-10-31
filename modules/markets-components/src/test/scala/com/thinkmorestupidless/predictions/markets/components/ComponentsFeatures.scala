package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.{CommandError, EntityId, ErrorCode}
import com.thinkmorestupidless.ankka.runtime.{ProjectionRuntime, TimerRuntime}
import com.thinkmorestupidless.ankka.testkit.{AnkkaTestKit, GherkinSuite}
import com.thinkmorestupidless.predictions.markets.*

import java.time.{Duration, Instant, LocalDate}
import scala.concurrent.duration.DurationInt

/**
 * The resolution port's scenario of `features/libraries/components.feature`, on a real service with
 * a throwaway Postgres and a scripted port (research R11). The exchange's scenarios are the
 * exchange components' suite's.
 */
class ComponentsFeatures extends GherkinSuite("features/libraries/components.feature"):

  override val munitTimeout = 5.minutes

  private val exchanges = "exchange's ComponentsFeatures"

  override protected def ranElsewhere: Map[String, String] = Map(
    "a ledger that refuses the reservation leaves the book unchanged and the order resting nowhere" -> exchanges,
    "a gate that refuses the order reserves nothing and names the gate's reason" -> exchanges,
    "a trade is settled through the ledger keyed by the trade, and a repeat is answered as the first was" -> exchanges,
    "the components are registered by the consumer and never by the library" -> exchanges
  )

  private val resolution = ScriptedResolution()
  private val timers     = TimerRuntime(pollInterval = 200.millis)
  private val components = MarketsComponents(MarketPorts(resolution, () => timers.timerScheduler))

  private var kit: AnkkaTestKit             = null
  private var refused: Option[CommandError] = None

  override def beforeAll(): Unit =
    kit = AnkkaTestKit.start(components.descriptors, Seq(ProjectionRuntime(), timers))

  override def afterAll(): Unit = if kit != null then kit.stop()

  override def beforeEach(context: BeforeEach): Unit = refused = None

  private def market(name: String): MarketId = MarketId(s"$name-${scenarioId.hashCode.toHexString}")

  private def entity(m: MarketId) = kit.componentClient.forEventSourcedEntity(EntityId(m.value))

  Given("an open market {string} with the outcomes {string} and {string} in {string}") {
    (name: String, first: String, second: String, currency: String) =>
      val m   = market(name)
      val now = Instant.now()
      entity(m)
        .call(components.market.draft)
        .invoke(
          MarketCommand
            .Draft(m, OperatorId("ops"), Samples.definition(first, second, currency, now), now)
        ): Unit
      entity(m)
        .call(components.market.approve)
        .invoke(MarketCommand.Approve(OperatorId("lead"), now)): Unit
  }

  Given("a player {string} with an active account")((_: String) => ())
  Given("a ledger port that allows {string}")((_: String) => ())
  Given("a gate port that allows {string}")((_: String) => ())

  Given("a resolution port that refuses {string}") { (operator: String) =>
    resolution.answer(OperatorId(operator), ResolutionAnswer.Refused("not a resolver"))
  }

  When("{string} resolves {string} to {string}") {
    (operator: String, name: String, outcome: String) =>
      val command: MarketCommand.Resolve = MarketCommand.Resolve(
        OperatorId(operator),
        OutcomeId(outcome),
        Vector(Evidence("the source", "https://example.org/result", "it happened")),
        "the authority",
        Duration.ofHours(1),
        Instant.now()
      )
      try entity(market(name)).call(components.market.resolve).invoke(command): Unit
      catch case e: CommandError => refused = Some(e)
  }

  Then("the resolution is refused") { () =>
    val error = refused.getOrElse(fail("the resolution was accepted"))
    assertEquals(error.code, ErrorCode.Forbidden)
    assertEquals(
      Refused.unapply(error),
      Some(Refusal("not a resolver", Map("permission" -> "resolve")))
    )
  }

  Then("{string} is an open market") { (name: String) =>
    assertEquals(entity(market(name)).call(components.market.get).invoke().state, MarketState.Open)
  }

/** A definition the drafts here share. */
object Samples:
  def definition(
      first: String,
      second: String,
      currency: String,
      now: Instant,
      openTime: Option[Instant] = None
  ): Definition =
    Definition(
      question = "Will it happen?",
      description = "Whether it happens by the close.",
      category = "events",
      outcomes = Vector(OutcomeId(first), OutcomeId(second)),
      currency = currency,
      openTime = openTime,
      close = now.plus(Duration.ofDays(7)),
      expectedResolution =
        LocalDate.ofInstant(now.plus(Duration.ofDays(8)), java.time.ZoneOffset.UTC),
      criteria = "It happens, as the authority reports.",
      authority = "the authority"
    )

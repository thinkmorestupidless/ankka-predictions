package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.{
  CommandError,
  Codecs,
  ComponentId,
  EntityId,
  ErrorCode,
  Serializer
}
import com.thinkmorestupidless.ankka.core.effect.{
  WorkflowEffect,
  WorkflowReadOnlyEffect,
  WorkflowStepEffect
}
import com.thinkmorestupidless.ankka.sdk.{
  RecoverStrategy,
  Workflow,
  WorkflowContext,
  WorkflowSettings
}
import com.thinkmorestupidless.predictions.exchange.*

import scala.annotation.tailrec
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.util.control.NonFatal

/** An order to place on the exchange of `market`. */
final case class PlaceOrder(market: MarketId, order: ExchangeCommand.Place)

/** Where a placement stands: still running, or what it ended with. */
enum PlacementOutcome:
  case Running
  case Accepted(events: Vector[ExchangeEvent])
  case Refused(refusal: Refusal)

/** What a placement recorded: the order it was started with, whether its cost is held, its end. */
final case class PlacementState(
    request: Option[PlaceOrder] = None,
    reserved: Boolean = false,
    outcome: PlacementOutcome = PlacementOutcome.Running
)

object PlacementState:
  given Serializer[PlacementState] = Codecs.serializer[PlacementState]("exchange-placement")

/**
 * One order's way onto the book (research R7), under the id `<market>/<order>`: the gate, then for
 * a buy the reservation of its cost, then the exchange (an order priced or sized below one goes to
 * the exchange alone, which refuses it); a buy the exchange refuses is released under ordinal 0.
 * `place` is the start and the claim: a repeat is answered with what the first recorded. The book
 * never waits on the ledger, because the entity is reached only once the money is held.
 */
final class Placement(ports: ExchangePorts, steps: PlacementWorkflow, ctx: WorkflowContext)
    extends Workflow[PlacementState]:

  def emptyState: PlacementState = PlacementState()

  override def settings: WorkflowSettings = steps.settings

  def place(request: PlaceOrder): WorkflowEffect[PlacementState, PlacementOutcome] =
    currentState.request match
      // A repeat is the same order whenever it was sent: its time is the caller's clock, not its terms.
      case Some(first) if first == request.copy(order = request.order.copy(at = first.order.at)) =>
        effects.reply(currentState.outcome)
      case Some(_) =>
        effects.error(
          s"the order ${request.order.order.value} was placed on other terms",
          ErrorCode.Conflict
        )
      case None if ctx.workflowId != PlacementWorkflow.id(request) =>
        effects.error(
          s"a placement of ${request.order.order.value} runs as ${PlacementWorkflow.id(request)}",
          ErrorCode.BadRequest
        )
      case None =>
        // A price or a quantity below one is refused by the exchange whatever its unit, so such an
        // order goes straight to it: nothing is gated, and no reservation of nothing or less is asked.
        val unfit = request.order.price < 1 || request.order.quantity < 1
        effects
          .updateState(PlacementState(Some(request)))
          .transitionTo(if unfit then steps.applyStep.ref else steps.gate.ref)
          .thenReply(PlacementOutcome.Running)

  def outcome: WorkflowReadOnlyEffect[PlacementState, PlacementOutcome] =
    if currentState.request.isEmpty then effects.error("no such placement", ErrorCode.NotFound)
    else effects.reply(currentState.outcome)

  def gate: WorkflowStepEffect[PlacementState] =
    ports.gate.mayPlace(order.owner, market, order) match
      case GateAnswer.Allow =>
        stepEffects.thenTransitionTo(
          if order.side == Side.Buy then steps.reserve.ref else steps.applyStep.ref
        )
      case GateAnswer.Refused(reason) => end(Refusal(reason))

  def reserve: WorkflowStepEffect[PlacementState] =
    patiently(ports.ledger.reserve(market, order.order, money)) match
      case Some(LedgerAnswer.Done) =>
        stepEffects
          .updateState(currentState.copy(reserved = true))
          .thenTransitionTo(steps.applyStep.ref)
      case Some(LedgerAnswer.Insufficient)    => end(Refusal("insufficient"))
      case Some(LedgerAnswer.Refused(reason)) => end(Refusal(reason))
      case None                               => stepEffects.thenTransitionTo(steps.unavailable.ref)

  def applyOrder: WorkflowStepEffect[PlacementState] =
    val exchange = ctx.componentClient.forEventSourcedEntity(EntityId(market.value))
    try
      val events = exchange.call(ExchangeEntity.place).invoke(order)
      stepEffects
        .updateState(currentState.copy(outcome = PlacementOutcome.Accepted(events)))
        .thenEnd
    catch
      case error: CommandError =>
        error match
          case Refused(refusal) if currentState.reserved =>
            stepEffects
              .updateState(currentState.copy(outcome = PlacementOutcome.Refused(refusal)))
              .thenTransitionTo(steps.release.ref)
          case Refused(refusal) => end(refusal)
          case fault            => throw fault

  def release: WorkflowStepEffect[PlacementState] =
    ports.ledger.release(market, order.order, 0, money) match
      case LedgerAnswer.Done => stepEffects.thenEnd
      case other =>
        Placement.log.log(
          System.Logger.Level.WARNING,
          s"the release of ${order.order.value} under ordinal 0 in ${market.value} was answered $other"
        )
        stepEffects.thenEnd

  def unavailable: WorkflowStepEffect[PlacementState] = end(Refusals.ledgerUnavailable)

  private def request: PlaceOrder =
    currentState.request.getOrElse(throw IllegalStateException("a placement step ran unstarted"))
  private def market: MarketId             = request.market
  private def order: ExchangeCommand.Place = request.order
  private def money: Money = Money(Exchange.cost(order.quantity, order.price), order.owner)

  private def end(refusal: Refusal): WorkflowStepEffect[PlacementState] =
    stepEffects.updateState(currentState.copy(outcome = PlacementOutcome.Refused(refusal))).thenEnd

  /**
   * The ledger's answer, asking again with a growing pause while it throws, until the consumer's
   * patience is spent (research R8); `None` when it never answered. ankka's step recovery counts
   * retries and knows no duration, so the patience is kept here.
   */
  private def patiently[A](call: => A): Option[A] =
    val deadline = System.nanoTime() + ports.ledgerPatience.toNanos
    @tailrec def attempt(pause: Long): Option[A] =
      val answer =
        try Some(call)
        catch case NonFatal(_) => None
      answer match
        case Some(a) => Some(a)
        case None if System.nanoTime() + pause * 1_000_000 < deadline =>
          Thread.sleep(pause)
          attempt((pause * 2).min(1000))
        case None => None
    attempt(50)

object Placement:
  private val log = System.getLogger("com.thinkmorestupidless.predictions.exchange.placement")

/** The placement workflow's declaration, built from the consumer's ports. */
final class PlacementWorkflow(ports: ExchangePorts)
    extends Workflow.Companion[Placement, PlacementState](
      ComponentId("exchange-placement"),
      summon[Serializer[PlacementState]]
    ):

  def create(ctx: WorkflowContext): Placement = new Placement(ports, this, ctx)

  given Serializer[PlaceOrder]       = Codecs.serializer("exchange-place-order")
  given Serializer[PlacementOutcome] = Codecs.serializer("exchange-placement-outcome")

  val gate        = step("gate")(_.gate)
  val reserve     = step("reserve")(_.reserve)
  val applyStep   = step("apply")(_.applyOrder)
  val release     = step("release")(_.release)
  val unavailable = step("unavailable")(_.unavailable)

  val place   = command[PlaceOrder, PlacementOutcome]("place")(_.place)
  val outcome = query[PlacementOutcome]("outcome")(_.outcome)

  /**
   * The reserve step outlasts the patience; a step that faults past it is refused as unavailable.
   */
  private[components] lazy val settings: WorkflowSettings =
    val patience = FiniteDuration(ports.ledgerPatience.toNanos, "nanos")
    WorkflowSettings.builder
      .stepTimeout(reserve, patience + 10.seconds)
      .stepRecovery(reserve, RecoverStrategy.maxRetries(0).failoverTo(unavailable))
      .stepRecovery(release, RecoverStrategy.maxRetries(5))
      .build

object PlacementWorkflow:

  /** The id a placement runs under: the market and the order. */
  def id(request: PlaceOrder): EntityId =
    EntityId(s"${request.market.value}/${request.order.order.value}")

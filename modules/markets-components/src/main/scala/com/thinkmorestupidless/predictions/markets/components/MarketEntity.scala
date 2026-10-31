package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.{Codecs, ComponentId, ErrorCode, Serializer}
import com.thinkmorestupidless.ankka.sdk.{EventSourcedEntity, EventSourcedEntityContext}
import com.thinkmorestupidless.predictions.markets.*

/**
 * One market, under its id: every command applies the domain and persists its events. Drafting,
 * approving, suspending, resuming, closing early, resolving, correcting and voiding ask the
 * resolution port first; its refusal changes nothing and is answered as forbidden, naming the
 * permission asked for.
 */
final class MarketEntity(resolution: ResolutionPort, ctx: EventSourcedEntityContext)
    extends EventSourcedEntity[Market, MarketEvent]:

  def emptyState: Market = Market.empty

  def applyEvent(event: MarketEvent): Market = Market.replay(currentState, event)

  def decide(command: MarketCommand): Effect[Vector[MarketEvent]] =
    asked(command) match
      case Some((permission, ResolutionAnswer.Refused(reason))) =>
        effects.error(
          Refused.error(Refusal(reason, Map("permission" -> permission)), ErrorCode.Forbidden)
        )
      case _ =>
        Market(currentState, command) match
          case Left(refusal)                              => effects.error(Refused.error(refusal))
          case Right(decision) if decision.events.isEmpty => effects.reply(Vector.empty)
          case Right(decision) =>
            effects.persistAll(decision.events).thenReply(_ => decision.events)

  def get: ReadOnlyEffect[Market] = effects.reply(currentState)

  /** The permission a command needs and the port's answer, or none for a command that asks none. */
  private def asked(command: MarketCommand): Option[(String, ResolutionAnswer)] =
    val market = MarketId(ctx.entityId)
    import MarketCommand.*
    command match
      case c: Draft   => Some("draft" -> resolution.mayDraft(c.operator))
      case c: Approve => Some("approve" -> resolution.mayApprove(c.operator, market))
      case c: Suspend => Some("suspend" -> resolution.maySuspend(c.operator, market))
      case c: Resume  => Some("suspend" -> resolution.maySuspend(c.operator, market))
      case Close(Some(operator), _, _) => Some("close" -> resolution.mayClose(operator, market))
      case c: Resolve => Some("resolve" -> resolution.mayResolve(c.operator, market))
      case c: Correct => Some("resolve" -> resolution.mayResolve(c.operator, market))
      case c: Void    => Some("void" -> resolution.mayVoid(c.operator, market))
      case _: Change | _: Open | _: Close | _: MoveClose | _: Finalise => None

/** The market entity's declaration, built from the consumer's resolution port. */
final class MarketEntityCompanion(resolution: ResolutionPort)
    extends EventSourcedEntity.Companion[MarketEntity, Market, MarketEvent](
      ComponentId("market"),
      summon[Serializer[Market]],
      summon[Serializer[MarketEvent]]
    ):

  def create(ctx: EventSourcedEntityContext): MarketEntity = new MarketEntity(resolution, ctx)

  given Serializer[Vector[MarketEvent]] = Codecs.serializer("market-events")

  given Serializer[MarketCommand.Draft]     = Codecs.serializer("market-draft")
  given Serializer[MarketCommand.Change]    = Codecs.serializer("market-change")
  given Serializer[MarketCommand.Approve]   = Codecs.serializer("market-approve")
  given Serializer[MarketCommand.Open]      = Codecs.serializer("market-open")
  given Serializer[MarketCommand.Suspend]   = Codecs.serializer("market-suspend")
  given Serializer[MarketCommand.Resume]    = Codecs.serializer("market-resume")
  given Serializer[MarketCommand.Close]     = Codecs.serializer("market-close")
  given Serializer[MarketCommand.MoveClose] = Codecs.serializer("market-move-close")
  given Serializer[MarketCommand.Resolve]   = Codecs.serializer("market-resolve")
  given Serializer[MarketCommand.Correct]   = Codecs.serializer("market-correct")
  given Serializer[MarketCommand.Void]      = Codecs.serializer("market-void")
  given Serializer[MarketCommand.Finalise]  = Codecs.serializer("market-finalise")

  private type Events = Vector[MarketEvent]

  val draft     = command[MarketCommand.Draft, Events]("draft")(_.decide)
  val change    = command[MarketCommand.Change, Events]("change")(_.decide)
  val approve   = command[MarketCommand.Approve, Events]("approve")(_.decide)
  val open      = command[MarketCommand.Open, Events]("open")(_.decide)
  val suspend   = command[MarketCommand.Suspend, Events]("suspend")(_.decide)
  val resume    = command[MarketCommand.Resume, Events]("resume")(_.decide)
  val close     = command[MarketCommand.Close, Events]("close")(_.decide)
  val moveClose = command[MarketCommand.MoveClose, Events]("move-close")(_.decide)
  val resolve   = command[MarketCommand.Resolve, Events]("resolve")(_.decide)
  val correct   = command[MarketCommand.Correct, Events]("correct")(_.decide)
  val void      = command[MarketCommand.Void, Events]("void")(_.decide)
  val finalise  = command[MarketCommand.Finalise, Events]("finalise")(_.decide)

  val get = query[Market]("get")(_.get)

object MarketEntityCompanion:
  /** The markets' events, for a view or consumer that does not hold the entity's declaration. */
  val events: com.thinkmorestupidless.ankka.sdk.ChangeSource[MarketEvent] =
    com.thinkmorestupidless.ankka.sdk.ChangeSource
      .EventSourced(ComponentId("market"), summon[Serializer[MarketEvent]])

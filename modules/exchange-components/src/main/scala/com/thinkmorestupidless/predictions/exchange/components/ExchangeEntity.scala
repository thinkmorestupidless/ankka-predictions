package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.{Codecs, ComponentId, Serializer}
import com.thinkmorestupidless.ankka.sdk.{EventSourcedEntity, EventSourcedEntityContext}
import com.thinkmorestupidless.predictions.exchange.*

import scala.collection.immutable.SortedMap

/**
 * One market's exchange, under the market's id: every command applies the domain and persists the
 * events it decided, and a refusal is the domain's (`Refused`). It calls no port, so the book never
 * waits on the ledger (research R7).
 */
final class ExchangeEntity extends EventSourcedEntity[Exchange, ExchangeEvent]:

  def emptyState: Exchange = Exchange.empty

  def applyEvent(event: ExchangeEvent): Exchange = Exchange.replay(currentState, event)

  def decide(command: ExchangeCommand): Effect[Vector[ExchangeEvent]] =
    Exchange(currentState, command) match
      case Left(refusal)                              => effects.error(Refused.error(refusal))
      case Right(decision) if decision.events.isEmpty => effects.reply(Vector.empty)
      case Right(decision) => effects.persistAll(decision.events).thenReply(_ => decision.events)

  def get: ReadOnlyEffect[Exchange] = effects.reply(currentState)

  def prices: ReadOnlyEffect[Prices] =
    effects.reply(Prices(currentState.lastPrices, commandContext.sequenceNumber))

  def depth(outcome: OutcomeId): ReadOnlyEffect[Depth] =
    effects.reply(Depth.of(currentState, outcome, commandContext.sequenceNumber))

  def position(owner: OwnerId): ReadOnlyEffect[Holdings] =
    effects.reply(Holdings(owner, currentState.positions.getOrElse(owner, SortedMap.empty)))

object ExchangeEntity
    extends EventSourcedEntity.Companion[ExchangeEntity, Exchange, ExchangeEvent](
      ComponentId("exchange"),
      summon[Serializer[Exchange]],
      summon[Serializer[ExchangeEvent]]
    ):

  def create(ctx: EventSourcedEntityContext): ExchangeEntity = new ExchangeEntity

  given Serializer[Vector[ExchangeEvent]] = Codecs.serializer("exchange-events")
  given Serializer[OutcomeId]             = Codecs.serializer("exchange-outcome")
  given Serializer[OwnerId]               = Codecs.serializer("exchange-owner")

  given Serializer[ExchangeCommand.Open]     = Codecs.serializer("exchange-open")
  given Serializer[ExchangeCommand.Place]    = Codecs.serializer("exchange-place")
  given Serializer[ExchangeCommand.Cancel]   = Codecs.serializer("exchange-cancel")
  given Serializer[ExchangeCommand.Split]    = Codecs.serializer("exchange-split")
  given Serializer[ExchangeCommand.Merge]    = Codecs.serializer("exchange-merge")
  given Serializer[ExchangeCommand.Suspend]  = Codecs.serializer("exchange-suspend")
  given Serializer[ExchangeCommand.Resume]   = Codecs.serializer("exchange-resume")
  given Serializer[ExchangeCommand.Close]    = Codecs.serializer("exchange-close")
  given Serializer[ExchangeCommand.Finalise] = Codecs.serializer("exchange-finalise")

  private type Events = Vector[ExchangeEvent]

  val open     = command[ExchangeCommand.Open, Events]("open")(_.decide)
  val place    = command[ExchangeCommand.Place, Events]("place")(_.decide)
  val cancel   = command[ExchangeCommand.Cancel, Events]("cancel")(_.decide)
  val split    = command[ExchangeCommand.Split, Events]("split")(_.decide)
  val merge    = command[ExchangeCommand.Merge, Events]("merge")(_.decide)
  val suspend  = command[ExchangeCommand.Suspend, Events]("suspend")(_.decide)
  val resume   = command[ExchangeCommand.Resume, Events]("resume")(_.decide)
  val close    = command[ExchangeCommand.Close, Events]("close")(_.decide)
  val finalise = command[ExchangeCommand.Finalise, Events]("finalise")(_.decide)

  val get      = query[Exchange]("get")(_.get)
  val prices   = query[Prices]("prices")(_.prices)
  val depth    = query[OutcomeId, Depth]("depth")(_.depth)
  val position = query[OwnerId, Holdings]("position")(_.position)

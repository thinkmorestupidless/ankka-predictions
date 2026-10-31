package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/**
 * Why a command was refused: a reason and the names it refers to (the field, the limit, the state,
 * the id). A refusal changes nothing and produces no event.
 */
final case class Refusal(reason: String, names: Map[String, String] = Map.empty)

/** What an accepted command led to: the next value and the events that produce it from the last. */
final case class Decision(next: Exchange, events: Vector[ExchangeEvent])

/** The refusals the exchange gives, by the words spec 005 uses. */
object Refusals:

  def outOfOrder(last: Instant, at: Instant): Refusal =
    Refusal("out of order", Map("last" -> last.toString, "at" -> at.toString))

  def state(state: ExchangeState): Refusal =
    Refusal("state", Map("state" -> ExchangeState.word(state)))

  def price(price: Long): Refusal       = Refusal("price", Map("price" -> price.toString))
  /** The quantity as it was given, which may not have been a number. */
  def quantity(quantity: String): Refusal = Refusal("quantity", Map("quantity" -> quantity))
  def outcome(outcome: OutcomeId): Refusal = Refusal("outcome", Map("outcome" -> outcome.value))
  def limit(name: String): Refusal         = Refusal("limit", Map("limit" -> name))
  def owner(owner: OwnerId): Refusal       = Refusal("owner", Map("owner" -> owner.value))
  def id(order: OrderId): Refusal          = Refusal("id", Map("id" -> order.value))
  def key(key: SetKey): Refusal            = Refusal("key", Map("key" -> key.value))
  def quantityHeld(held: Long): Refusal    = Refusal("quantity held", Map("held" -> held.toString))
  val notFillableWhole: Refusal            = Refusal("not fillable whole")

  /** A rule spec 005 has not yet built; the stub's answer to every such command. */
  def notBuilt(command: String): Refusal = Refusal("not built", Map("command" -> command))

  /** The ledger did not answer within the consumer's patience (research R8). */
  val ledgerUnavailable: Refusal = Refusal("ledger unavailable")

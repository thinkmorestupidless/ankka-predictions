package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.Instant
import scala.collection.immutable.SortedMap

/**
 * What happened to an exchange (spec 005 FR-019), journalled under the manifest `exchange-event`
 * and named in JSON by `type`. A published protocol: a case is never renamed or removed, and a
 * field is only added, with a default (spec 004 FR-009). Every case that moves money names the
 * amount, the owner and its key (FR-018).
 */
enum ExchangeEvent:
  def at: Instant

  case Opened(
      market: MarketId,
      outcomes: Vector[OutcomeId],
      unit: Long,
      maxOrderCost: Long,
      maxPosition: Long,
      at: Instant
  )

  /** An order accepted; for a buy, `cost` is what the consumer reserved under the order's id. */
  case OrderPlaced(
      order: OrderId,
      owner: OwnerId,
      outcome: OutcomeId,
      side: Side,
      price: Long,
      quantity: Long,
      duration: OrderDuration,
      cost: Long,
      sequence: Long,
      at: Instant
  )

  /**
   * A trade of `quantity`, the exchange's `sequence`th, keyed by `trade`; `prices` is the price of
   * each outcome traded, which becomes its last price.
   */
  case TradeMade(
      trade: TradeId,
      sequence: Long,
      through: Through,
      quantity: Long,
      prices: SortedMap[OutcomeId, Long],
      legs: Vector[Leg],
      at: Instant
  )

  case OrderFilled(order: OrderId, at: Instant)

  /** A resting order cancelled; a buy's remaining cost released under the order and ordinal. */
  case OrderCancelled(
      order: OrderId,
      owner: OwnerId,
      released: Long,
      releaseOrdinal: Int,
      at: Instant
  )

  /** A resting order expired at the close; a buy's remaining cost released likewise. */
  case OrderExpired(
      order: OrderId,
      owner: OwnerId,
      released: Long,
      releaseOrdinal: Int,
      at: Instant
  )

  /** An explicit split, charged `charge` into the pool under `key`. */
  case SetsSplit(key: SetKey, owner: OwnerId, sets: Long, charge: Long, at: Instant)

  /** An explicit merge, paid `paid` from the pool under `key`. */
  case SetsMerged(key: SetKey, owner: OwnerId, sets: Long, paid: Long, at: Instant)

  case Suspended(at: Instant)
  case Resumed(at: Instant)
  case Closed(at: Instant)

  /** A position paid at finality, keyed by the market and `key`. */
  case PositionPaid(
      key: PositionKey,
      owner: OwnerId,
      outcome: OutcomeId,
      quantity: Long,
      paid: Long,
      at: Instant
  )

  /** What a void's equal part left undivided, kept in the pool. */
  case RemainderKept(amount: Long, at: Instant)

  case Final(finality: Finality, at: Instant)

object ExchangeEvent:

  /**
   * The journal's serializer, under the manifest `exchange-event`: in the companion, so a
   * consumer's entity finds it without an import.
   */
  given Serializer[ExchangeEvent] = Codecs.serializer[ExchangeEvent]("exchange-event")

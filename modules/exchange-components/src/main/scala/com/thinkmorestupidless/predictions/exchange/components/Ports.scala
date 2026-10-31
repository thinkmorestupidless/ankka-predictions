package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.predictions.exchange.*

import java.time.Duration

// The seam between the components and the consumer (`contracts/ports.md`). Every call is
// synchronous; a port that cannot answer throws, and the components treat a throw as unavailable
// (research R6, R8). No casino vocabulary: an owner is an id, a brand is nothing.

/** Money as the domain says it: minor units and the owner. */
final case class Money(amount: Long, owner: OwnerId)

enum LedgerAnswer:
  case Done
  case Insufficient
  case Refused(reason: String)

/**
 * The consumer's ledger. Every call names the market, the exchange entity's id, so the consumer can
 * find the market's pool and currency; the libraries know neither. Every call is keyed, and a
 * repeat is answered as the first was (spec 004 FR-008).
 */
trait LedgerPort:

  /** Hold a buy's cost against its owner; keyed by the order id. */
  def reserve(market: MarketId, order: OrderId, money: Money): LedgerAnswer

  /**
   * Give back part of a buy's reservation (a fill-at-once remainder, a cancel, an expiry); keyed by
   * the order id and the release's ordinal for that order, from one. Ordinal 0 is the release of a
   * reservation the exchange refused (research R7).
   */
  def release(market: MarketId, order: OrderId, ordinal: Int, money: Money): LedgerAnswer

  /**
   * Settle a trade: `Direct` moves the buy leg's charge to the sell leg; `Split` charges every leg
   * from its reservation into the market's pool; `Merge` pays every leg from the pool. Each leg's
   * `released` is freed in the same call. Keyed by the trade; a consumer that needs one key per leg
   * derives it from the trade and the leg's owner, which never repeat in a trade (spec 005 FR-009).
   */
  def settle(market: MarketId, trade: TradeId, through: Through, legs: Vector[Leg]): LedgerAnswer

  /**
   * An explicit split, charged from the owner's available money into the pool; keyed by its key.
   */
  def charge(market: MarketId, key: SetKey, money: Money): LedgerAnswer

  /** An explicit merge, paid from the pool; keyed by its key. */
  def pay(market: MarketId, key: SetKey, money: Money): LedgerAnswer

  /** Pay a position at finality from the pool; keyed by the market and the position. */
  def payout(market: MarketId, key: PositionKey, money: Money): LedgerAnswer

enum GateAnswer:
  case Allow
  case Refused(reason: String)

/** The consumer's rules on who may trade: account, verification, limits. */
trait GatePort:

  /** May this owner place this order. */
  def mayPlace(owner: OwnerId, market: MarketId, order: ExchangeCommand.Place): GateAnswer

final case class ExchangePorts(
    ledger: LedgerPort,
    gate: GatePort,
    /** How long a reserve that throws is retried before the order is refused as unavailable. */
    ledgerPatience: Duration
)

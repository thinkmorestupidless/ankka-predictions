# The ports

The seam between the components and the consumer. Scala traits in the components modules; the
consumer implements each and hands them in when it constructs the components. Every call is
synchronous (handlers run on virtual threads); a port that cannot answer throws, and the
components treat a throw as "unavailable" (research R6, R8). No casino vocabulary (spec 004
FR-012): a player is an owner id, a brand is nothing.

```scala
package com.thinkmorestupidless.predictions.exchange.components

import java.time.Duration

/** Money as the domain says it: minor units, the owner, the key. */
final case class Money(amount: Long, owner: OwnerId)

enum LedgerAnswer:
  case Done
  case Insufficient
  case Refused(reason: String)

/** How a trade was made (spec 005 FR-011): between a buy and a sell, by splitting a set
  * between two buys, or by merging a set from two sells. */
enum Through:
  case Direct, Split, Merge

/** One side of a trade: what its order is charged from its reservation, what it is paid, and what
  * of its reservation the trade frees (a better price than the order's). */
final case class Leg(owner: OwnerId, order: OrderId, charge: Long, paid: Long, released: Long)

/** Every call names the market, the exchange entity's id, so the consumer can find the market's
  * pool and currency; the libraries know neither. */
trait LedgerPort:
  /** Hold a buy's cost against its owner; keyed by the order id. */
  def reserve(market: MarketId, order: OrderId, money: Money): LedgerAnswer
  /** Give back part of a buy's reservation (a fill-at-once remainder, a cancel, an expiry); keyed
    * by the order id and the release's ordinal for that order, from one. */
  def release(market: MarketId, order: OrderId, ordinal: Int, money: Money): LedgerAnswer
  /** Settle a trade: `Direct` moves the buy leg's charge to the sell leg; `Split` charges every
    * leg from its reservation into the market's pool; `Merge` pays every leg from the pool. Each
    * leg's `released` is freed in the same call. Keyed by the trade; a consumer that needs one key
    * per leg derives it from the trade and the leg's owner, which never repeat in a trade (spec
    * 005 FR-009). */
  def settle(market: MarketId, trade: TradeId, through: Through, legs: Vector[Leg]): LedgerAnswer
  /** An explicit split, charged from the owner's available money into the pool; keyed by its key. */
  def charge(market: MarketId, key: SetKey, money: Money): LedgerAnswer
  /** An explicit merge, paid from the pool; keyed by its key. */
  def pay(market: MarketId, key: SetKey, money: Money): LedgerAnswer
  /** Pay a position at finality from the pool; keyed by the market and the position. */
  def payout(market: MarketId, key: PositionKey, money: Money): LedgerAnswer

enum GateAnswer:
  case Allow
  case Refused(reason: String)

trait GatePort:
  /** May this owner place this order, whose cost (a buy) or shares (a sell) are as given. */
  def mayPlace(owner: OwnerId, market: MarketId, order: OrderRequest): GateAnswer

final case class ExchangePorts(
    ledger: LedgerPort,
    gate: GatePort,
    /** How long a reserve that throws is retried before the order is refused as unavailable. */
    ledgerPatience: Duration
)
```

```scala
package com.thinkmorestupidless.predictions.markets.components

enum ResolutionAnswer:
  case Allow
  case Refused(reason: String)

trait ResolutionPort:
  def mayDraft(operator: OperatorId): ResolutionAnswer
  def mayApprove(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayResolve(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayVoid(operator: OperatorId, market: MarketId): ResolutionAnswer
  def mayClose(operator: OperatorId, market: MarketId): ResolutionAnswer
  /** Suspend and resume: the operator's emergency authority. */
  def maySuspend(operator: OperatorId, market: MarketId): ResolutionAnswer

final case class MarketPorts(resolution: ResolutionPort)
```

## Rules the components hold at the seam

| Rule | Spec | Held by |
|---|---|---|
| A gate refusal reserves nothing and carries the gate's reason | 004 FR-007 | the placement workflow's `gate` step runs before `reserve` |
| A ledger refusal (`Insufficient`, `Refused`) leaves the book unchanged and no event | 004 FR-007 | the workflow reaches the entity only after `Done`; the entity calls no port (R7) |
| A reservation the exchange refuses is released under the order and ordinal 0 | R7 | the workflow's `release` step |
| Every ledger call is keyed; a repeat is answered as the first was | 004 FR-008 | the keys are the domain's ids; the settlement consumer is at least once |
| A release is keyed by its order and its ordinal for that order, so an order's second release is not a repeat of its first | 005 FR-005, FR-008, FR-018; R14 | `ExchangeEvent`'s release-carrying cases name the ordinal |
| A trade names how it was made and every leg's charge, payment and release | 005 FR-011, FR-019; R14 | `settlement` passes the `TradeMade` event's legs |
| A ledger that throws is retried within `ledgerPatience`, then the order is refused as `ledger unavailable` | 004 edge case, R8 | the `reserve` step's declared recovery |
| A resolution-port refusal changes nothing | 004 FR-007 | `resolve`, `void`, `approve`, `close`, `suspend`, `resume` ask first |
| The components register nothing | 004 FR-006 | no `registerAll` in a library; `descriptors` is a value the consumer passes |

## Ids

`OwnerId`, `OperatorId`, `MarketId`, `OrderId`, `TradeId`, `SetKey`, `PositionKey` are opaque
`String` wrappers in the domain packages, serialised as strings. The consumer decides what they
contain (a player id, a staff id); the libraries compare them and nothing more.

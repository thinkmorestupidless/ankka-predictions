# Data model: the exchange domain

What the value holds, what each command does to it, what each event carries, and what is true of
the value after every command. The types are spec 004's (`specs/004-libraries/contracts/artefacts.md`);
this page fixes their meaning. Signatures are in `contracts/domain-api.md`.

## The value

| Field | Type | Meaning |
|---|---|---|
| `market` | `MarketId` | the market this is the exchange of; the entity's id |
| `outcomes` | `Vector[OutcomeId]` | in the order given at open; two outcomes enable the pool match |
| `unit` | `Long` | what one share pays if its outcome wins; prices run from 1 to `unit − 1` |
| `maxOrderCost`, `maxPosition` | `Long` | carried at open, fixed for the exchange's life |
| `state` | `ExchangeState` | `empty → open ↔ suspended → closed → final` |
| `books` | `SortedMap[OutcomeId, Book]` | one per outcome; `buys` best (highest) first, `sells` best (lowest) first, oldest first within a price |
| `orders` | `SortedMap[OrderId, Order]` | every order accepted, with its state |
| `positions` | `SortedMap[OwnerId, SortedMap[OutcomeId, Position]]` | `quantity`, `restingUnderSells`, `cost`, and (added here) `restingToBuy` |
| `setMoves` | `SortedMap[SetKey, SetMove]` | every explicit split and merge, for repeats |
| `setsOutstanding`, `remainder` | `Long` | the pool is `setsOutstanding × unit + remainder` |
| `lastPrices` | `SortedMap[OutcomeId, Long]` | absent until the outcome trades |
| `nextOrder`, `nextTrade` | `Long` | the sequence the next accepted order and the next trade take, from 1 |
| `lastTime` | `Option[Instant]` | the time of the last event; a command earlier than it is out of order |
| `finality` | `Option[Finality]` | set by `Final` |

### An order

`id, owner, outcome, side, price, quantity, remaining, duration, state, cost, sequence, releases`.
`cost` is `quantity × price` for a buy and 0 for a sell. `releases` is the last release ordinal
the order used. States: `resting → filled-in-part → filled`, or `→ cancelled` / `→ expired` from
either of the first two.

### A position

`quantity` shares held; `restingUnderSells` of them offered under resting sells; `restingToBuy`
on order under resting buys; `cost`, what the held shares cost. Free to sell: `quantity −
restingUnderSells`. Room to buy or split: `maxPosition − quantity − restingToBuy`.

### A trade

Not kept in the value: `TradeMade` is the record. `sequence` from `nextTrade`; `trade` is
`TradeId.of(sequence)` (`t-1`, `t-2`, …); `through` direct, split or merge; `quantity`; `prices`
per outcome traded; two `Leg`s (owner, order, outcome, side, charge, paid, released,
releaseOrdinal).

## The commands

Every command carries `at`; one earlier than `lastTime` is refused `out of order`. A refusal
produces no event and no change.

| Command | Refused when (reason) | Events |
|---|---|---|
| `Open` | state not empty (`state`) | `Opened` |
| `Place` | state not open (`state`); outcome not the market's (`outcome`); price < 1 or ≥ unit (`price`); quantity < 1 (`quantity`); a buy's cost > maxOrderCost (`limit` = maximum order cost); a buy's quantity + held + resting to buy > maxPosition (`limit` = maximum position); a sell's quantity > held − resting under sells (`quantity held`); a fill-whole order short of a whole fill (`not fillable whole`); a known id with other fields (`id`) | a known id with the same fields: none. Else `OrderPlaced`, then per trade `TradeMade` and the `OrderFilled`s it completes, then a fill-at-once remainder's `OrderCancelled` |
| `Cancel` | order unknown or not resting / filled in part (`state`); caller not the owner (`owner`); state final or closed (`state`) | `OrderCancelled` with the buy's remaining cost released (a sell: 0) |
| `Split` | state not open (`state`); sets < 1 (`quantity`); any outcome's position would pass maxPosition (`limit`); known key with other fields (`key`) | `SetsSplit` (charge = sets × unit) |
| `Merge` | state not open (`state`); sets < 1 (`quantity`); any outcome held free < sets (`quantity held`); known key with other fields (`key`) | `SetsMerged` (paid = sets × unit) |
| `Suspend` | state not open (`state`) | `Suspended` |
| `Resume` | state not suspended (`state`) | `Resumed` |
| `Close` | state final (`state`); empty (`state`) | closed already: none. Else one `OrderExpired` per resting order (books in outcome order, buys then sells, best first), then `Closed` |
| `Finalise` | state open, suspended or empty (`state`); final with another finality (`state`) | final with the same finality: none. Else one `PositionPaid` per position with shares (every outcome), `RemainderKept` when a void leaves one, then `Final` |

## The events and their money

| Event | Money fields | Keyed by |
|---|---|---|
| `OrderPlaced` | `cost` (a buy's reservation, made by the consumer before the place) | the order |
| `TradeMade` | per leg `charge` (a buy), `paid` (a sell), `released` with `releaseOrdinal` (a buy at a better price) | the trade; a release by its order and ordinal |
| `OrderCancelled`, `OrderExpired` | `released`, `releaseOrdinal` | the order and ordinal |
| `SetsSplit` | `charge` | the key |
| `SetsMerged` | `paid` | the key |
| `PositionPaid` | `paid` (0 for a losing outcome) | the market and the position key `owner/outcome` |
| `RemainderKept` | `amount` | none; the pool keeps it |
| `Opened`, `OrderFilled`, `Suspended`, `Resumed`, `Closed`, `Final` | none | |

A pool trade's legs: a split is two buy legs of different outcomes, the resting one charged its
price and the taker `unit − q`, the taker's release `(p − (unit − q)) × quantity`; a merge is two
sell legs, paid likewise. `prices` carries both outcomes for a pool trade, one for a direct trade.

## What `replay` does with each event

`Opened` sets the exchange up with an empty book per outcome. `OrderPlaced` records the order,
rests it, and adds to `restingToBuy` (a buy) or `restingUnderSells` (a sell). `TradeMade` reduces
each leg's order's remaining, marks it filled in part, records its release ordinal, moves shares
and cost by `PositionMoves`, takes the quantity off `restingToBuy` (a buy leg) and
`restingUnderSells` (a sell leg), moves the sets outstanding for a split or merge, sets the last
prices, and advances `nextTrade`. `OrderFilled` takes the order out of its book. `OrderCancelled`
and `OrderExpired` take it out, free what it rested, and mark it. `SetsSplit` and `SetsMerged`
move shares and cost by `PositionMoves` and the sets outstanding, and record the key. `Suspended`,
`Resumed`, `Closed` set the state. `PositionPaid` zeroes the position. `RemainderKept` adds to the
remainder. `Final` sets the state, zeroes the sets outstanding and records the finality. Every
event sets `lastTime`.

## Invariants (after every command; SC-002)

1. For every outcome, the total of `quantity` over positions equals `setsOutstanding`
   (until `Final`, which zeroes both sides through `PositionPaid` and `Final`).
2. The pool, `setsOutstanding × unit + remainder`, equals what the events say: splits charged
   minus merges paid minus payouts paid, plus pool trades' charges minus their payments.
3. No position's `quantity` is negative; `restingUnderSells ≤ quantity`; `restingToBuy ≥ 0`.
4. For every buy: `cost − Σ released − Σ charged = remaining × price` while it rests, and 0 once
   filled, cancelled or expired. So the resting buys' remaining costs are exactly what is reserved.
5. `events.foldLeft(Exchange.empty)(replay) == next` for every accepted command, and a refused
   command's value is the one it was applied to.
6. Trades carry 1, 2, 3, …; orders carry acceptance sequences 1, 2, 3, …; the same commands in
   the same order give the same events.

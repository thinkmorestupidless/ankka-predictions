# Feature Specification: The Exchange Domain

**Feature Branch**: `005-exchange-domain`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "The `exchange-domain` library of spec 004, in full: the value of
one market's trading, the commands applied to it, the refusals, the events, and the rules the
book keeps. A share pays one unit if its outcome wins. Orders match by price and then by time at
the resting order's price, with partial fills and three durations. Complete sets are split from
and merged into the pool for one unit; in a two-outcome market opposite orders meet through the
pool. A close expires every order; an exchange made final pays every position once. Money is
never read: every event that moves money names the amount, the owner and what it is keyed by,
and the consumer's ledger does the moving. Nothing reads a clock, a wallet, a player or a
network."

## Context

Spec 004 made this repository two libraries and said what shape the exchange domain has: a value,
commands that yield a refusal or the next value and its events, events that replay. This spec
says what the value holds and what every command does, restating the rules of spec 003 that are
the book's and leaving out the ones that are the consumer's: the gates, the reservation in a
wallet, the facts on topics, the price stream and the observations into a reasoning graph, which
ankka-modules specs 022 and 023 carry.

One decision is taken here that spec 003 and the constitution did not. The value is the exchange
of one market, not the book of one outcome. Three rules span outcomes and cannot be kept inside
one outcome's book: the pool invariant (every outcome has the same total of shares held), a
player's complete sets, and the two-outcome match of a buy of one outcome against a buy of the
other. One value per market keeps them exact and keeps the whole market deterministic from one
sequence of commands; the books are sub-values inside it. Constitution principle II says "one
event sourced entity per outcome" and is amended by this spec to "one per market". The cost is
that every outcome of a market shares one entity; a market has at most twenty outcomes and the
target in spec 003 (one book taking 1,000 orders a second) becomes the market's.

Money is handled by saying, never by doing. A buy order's cost is quantity times price; the
consumer reserves it before applying the order and the exchange trusts that it did. A trade says
what the buyer is charged, what the seller is paid and what the buyer's order releases; a split
says what it charges; a payout says what it pays. The ledger port of `exchange-components` is
driven from those events alone, keyed as they say.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - An order is accepted, refused or cancelled on the value (Priority: P1)

A developer applies an order to an open exchange: an owner, an outcome, buy or sell, a price, a
quantity, a duration and an order id. The exchange refuses a price outside one to one less than
the unit, a quantity that is not a positive whole number, an outcome the market lacks, a state
that is not open, a cost above the maximum order cost, a position that would pass the maximum
position, or a sale of more shares than the owner holds beyond what rests under other sells, each
naming the field or the limit. On a suspended exchange nothing is placed, but a resting order may
still be cancelled, and when the exchange resumes its resting orders trade again. An accepted buy
rests at its cost. The owner, and no one else,
cancels a resting order and its remaining cost is released; a filled order cannot be cancelled. A
repeat under a known order id with the same fields is answered as the first was and produces no
event; the same id with different fields is refused naming the id.

**Why this priority**: Every rule below starts from an order the exchange has accepted.

**Independent Test**: Apply each refusal, a cancel by the owner and by another, and a repeat, to
a value with no runtime; the value is unchanged on every refusal.

**Acceptance Scenarios**:

- changed `features/exchange/orders.feature`: a buy order at a price and a quantity rests in the outcome's book at a cost of quantity times price
- changed `features/exchange/orders.feature`: an order at a price outside one to one less than the unit is refused
- changed `features/exchange/orders.feature`: an order for a quantity that is not a positive whole number is refused
- changed `features/exchange/orders.feature`: an order naming an outcome the market does not have is refused
- changed `features/exchange/orders.feature`: an order on an exchange that is not open is refused naming the state
- added `features/exchange/orders.feature`: a buy order whose cost is above the maximum order cost is refused naming the limit
- added `features/exchange/orders.feature`: a buy order that would take the position above the maximum position is refused naming the limit
- changed `features/exchange/orders.feature`: a player cancels a resting buy order and its remaining cost is released
- changed `features/exchange/orders.feature`: a player's cancel of another player's order is refused
- added `features/exchange/orders.feature`: a cancel of an order that is filled is refused naming the state
- changed `features/exchange/orders.feature`: a repeat of an order under the same id is answered as the first was and changes nothing
- added `features/exchange/orders.feature`: an order reusing the id of a different order is refused naming the id
- added `features/exchange/orders.feature`: an order on a suspended exchange is refused naming the state
- added `features/exchange/orders.feature`: a player cancels a resting order on a suspended exchange and its cost is released
- added `features/exchange/orders.feature`: a resumed exchange trades again with the orders that rested

---

### User Story 2 - Orders match by price and then by time, with partial fills (Priority: P1)

A buy matches the resting sells of its outcome at or below its price, lowest first and oldest
first at one price, each trade at the resting order's price, until it is filled or none matches;
a sell matches resting buys at or above its price the same way, highest first. A trade may fill
part of either order. An order to fill at once and cancel the rest leaves nothing resting and
releases the rest's cost; an order to fill whole or not at all is refused when the book cannot
fill it whole, and leaves the book unchanged. An order never matches an order of its own owner.
A trade at a better price than the buy order charges the trade's price and releases the
difference from the order. Each trade carries the exchange's next sequence number. The same
orders in the same sequence always produce the same trades.

**Why this priority**: This is the engine.

**Independent Test**: Seed a value with resting orders; apply orders that match none, one, several
and part of one; replay the same sequence on a fresh value and compare every trade.

**Acceptance Scenarios**:

- changed `features/exchange/matching.feature`: a buy order matches the lowest resting sell at or below its price at the resting order's price
- changed `features/exchange/matching.feature`: a buy order matches several resting sells in order of price and then time until it is filled
- changed `features/exchange/matching.feature`: a buy order larger than every matching sell is filled in part and the rest rests
- changed `features/exchange/matching.feature`: a buy order smaller than the resting sell fills in part and the sell keeps resting with what remains
- changed `features/exchange/matching.feature`: a buy order matches no sell above its price and rests whole
- changed `features/exchange/matching.feature`: a sell order matches the highest resting buy at or above its price at the resting order's price
- changed `features/exchange/matching.feature`: two resting orders at one price are matched oldest first
- changed `features/exchange/matching.feature`: an order to fill what it can at once and cancel the rest leaves nothing resting
- changed `features/exchange/matching.feature`: an order to fill whole at once or not at all is refused when the book cannot fill it whole
- added `features/exchange/matching.feature`: a trade at a better price than the buy order charges the trade's price and releases the difference
- changed `features/exchange/matching.feature`: the same orders in the same sequence produce the same trades
- changed `features/exchange/matching.feature`: an order never matches an order of the same player
- added `features/exchange/matching.feature`: each trade carries the next sequence number of the exchange

---

### User Story 3 - Positions, complete sets and the pool (Priority: P1)

A trade moves shares from the seller's position to the buyer's and moves cost with them: a buy
adds quantity times the trade's price, a sale removes the sold shares' part of the cost rounded
down. A sell needs shares the owner holds beyond what rests under other sells; a cancelled sell
frees them. A split charges one unit per set and gives one share of every outcome; a merge takes
one share of every outcome and pays one unit per set; both are keyed and a repeat changes
nothing. The pool holds one unit for every set outstanding, and every outcome has the same total
of shares held, which is the sets outstanding. In a two-outcome market a buy of one outcome is
matched against a resting buy of the other whose price makes one unit or more with it, by
splitting a set between them: the resting order pays its own price, the taker pays one unit less
that, and the taker's order releases the difference; a sell of one outcome is matched against a
resting sell of the other whose price makes one unit or less, by merging: the resting order is
paid its price and the taker the rest of the unit. The taker takes the better price of a resting
sell of its own outcome and a resting buy of the other, its own outcome first when they are
equal. A market with more than two outcomes matches directly and through explicit splits and
merges only.

**Why this priority**: Without the pool a new market has no sellers; without positions there is
nothing to pay at resolution.

**Independent Test**: Trade between two owners and through the pool; after every command, the
sets outstanding equal every outcome's total held, and the pool equals sets times the unit plus
the remainder.

**Acceptance Scenarios**:

- changed `features/exchange/positions.feature`: a trade adds the quantity to the buyer's position and removes it from the seller's
- changed `features/exchange/positions.feature`: a sell order for more shares than the player holds is refused
- changed `features/exchange/positions.feature`: a sell order for shares already resting under another sell order is refused
- changed `features/exchange/positions.feature`: a cancelled sell order frees its shares to be sold again
- added `features/exchange/positions.feature`: a position shows its quantity and its cost
- added `features/exchange/positions.feature`: a buy adds quantity times price to the position's cost
- added `features/exchange/positions.feature`: a sale removes the sold shares' part of the position's cost
- changed `features/exchange/complete-sets.feature`: a split pays one unit per set to the pool and gives one share of every outcome
- added `features/exchange/complete-sets.feature`: a split that would take a position above the maximum position is refused naming the limit
- changed `features/exchange/complete-sets.feature`: a merge gives one share of every outcome and receives one unit per set from the pool
- changed `features/exchange/complete-sets.feature`: a merge of more sets than the player holds of every outcome is refused
- added `features/exchange/complete-sets.feature`: a split or a merge repeated under its key is answered as the first was and changes nothing
- changed `features/exchange/complete-sets.feature`: the pool holds one unit for every set outstanding
- added `features/exchange/complete-sets.feature`: every outcome has the same total of shares held, which is the sets outstanding
- changed `features/exchange/complete-sets.feature`: in a two-outcome market a buy of one outcome matches a buy of the other whose prices make one unit or more by splitting a set between them
- changed `features/exchange/complete-sets.feature`: in a two-outcome market a buy matches a resting sell of its own outcome before a buy of the other at the same price
- added `features/exchange/complete-sets.feature`: in a two-outcome market a buy takes the better price of a resting sell of its own outcome and a buy of the other
- changed `features/exchange/complete-sets.feature`: in a two-outcome market a sell of one outcome matches a sell of the other whose prices make one unit or less by merging a set into the pool
- added `features/exchange/complete-sets.feature`: a market with more than two outcomes matches directly and through splits and merges only

---

### User Story 4 - A close expires every order and a final exchange pays every position once (Priority: P1)

A close expires every resting order and releases each buy's remaining cost; from then on every
order is refused naming the state. An exchange made final resolved pays one unit per share of the
resolved outcome and nothing for the others; made final void, it pays every share of every
outcome an equal part of one unit rounded down, and keeps and records the remainder in the pool.
Only a closed exchange can be made final; made final again, it produces no event and pays nothing
more. A closed exchange pays nothing until it is made final.

**Why this priority**: It is where money is decided.

**Independent Test**: Close a value with resting orders; make it final resolved, void with and
without a remainder, and again; every payout once, the pool empty but for the remainder.

**Acceptance Scenarios**:

- changed `features/exchange/close-and-settlement.feature`: a close expires every resting order and releases every cost
- added `features/exchange/close-and-settlement.feature`: a suspended exchange closes when its close comes and expires every resting order
- changed `features/exchange/close-and-settlement.feature`: an order on a closed exchange is refused
- changed `features/exchange/close-and-settlement.feature`: an exchange made final resolved pays one unit per share of the resolved outcome, nothing for the other outcomes, and empties the pool
- changed `features/exchange/close-and-settlement.feature`: an exchange made final void pays every share of every outcome an equal part of one unit
- added `features/exchange/close-and-settlement.feature`: a void whose equal part does not divide keeps the remainder in the pool and records it
- added `features/exchange/close-and-settlement.feature`: an exchange made final again pays nothing more
- added `features/exchange/close-and-settlement.feature`: an open exchange cannot be made final
- changed `features/exchange/close-and-settlement.feature`: a closed exchange pays nothing until it is made final

---

### User Story 5 - Prices are read from the value (Priority: P2)

For every outcome the value answers the last price, the best bid, the best ask and the depth. A
trade through the pool sets the last price of both outcomes: the taker's price for its outcome
and the resting order's for the other. An outcome that has not traded has no last price.

**Why this priority**: The consumer's catalogue and price stream read these; they are derived
from the books and prove nothing the books do not.

**Independent Test**: Trade and read; compare each figure to the books.

**Acceptance Scenarios**:

- changed `features/exchange/prices.feature`: a trade sets the outcome's last price
- added `features/exchange/prices.feature`: a trade through the pool sets the last price of both outcomes
- changed `features/exchange/prices.feature`: the best bid is the highest resting buy price and the best ask the lowest resting sell price
- changed `features/exchange/prices.feature`: the depth lists the quantity resting at each price
- added `features/exchange/prices.feature`: an outcome with no trade has no last price

---

### User Story 6 - The value, its events and replay (Priority: P1)

Every command yields a refusal naming its reason, with no event and no change, or the next value
and the events that led to it; replaying the events onto an empty value yields an equal value. A
command carries the time a rule needs; a time earlier than the value's last event is refused as
out of order. Every event that moves money names the amount, the owner and what the move is keyed
by: a reservation or a release by the order, a trade's charge and payment by the trade, a split
or a merge by its key, a payout by the market and the position.

**Why this priority**: Spec 004's promise, made concrete for this domain.

**Independent Test**: Property tests over random sequences: replay equality, refusal purity, the
invariants of FR-010, and every money event carrying its key.

**Acceptance Scenarios**:

- changed `features/libraries/domain.feature`: a book rebuilt from its events alone is the book that produced them
- changed `features/libraries/domain.feature`: a refused command leaves the book it was applied to unchanged
- changed `features/libraries/domain.feature`: the time a rule needs is carried by the command, not read
- added `features/libraries/domain.feature`: every event that moves money names the amount, the owner and what it is keyed by

---

### Edge Cases

- An order whose cost, with what the owner already has resting to buy, would pass the maximum
  position only through the two-outcome match: the check counts the order's own quantity and the
  owner's shares held and resting to buy in that outcome; a share received through a split trade
  counts as bought.
- A sell that would be matched against the owner's own resting buy: skipped, as a direct match of
  one's own order is.
- A fill-whole order that could fill whole only by taking the owner's own resting order: refused
  as not fillable whole.
- A void on an exchange with no sets outstanding: no payout, no remainder, an event all the same.
- A close on an exchange with no resting order: one event, nothing released.
- A suspend of a suspended exchange, or a resume of an open one: refused naming the state. The
  exchange suspends and resumes with its market (spec 006); driving it is the consumer's.
- Limits on an open exchange: carried when the exchange opens. [NEEDS CLARIFICATION: whether the
  maximum order cost and the maximum position may change on an open exchange, by a command of
  their own, or only when it opens]
- The two-outcome match takes the better price across a resting sell of the own outcome and a
  resting buy of the other, own outcome first when equal; spec 003 FR-009 said own outcome
  first regardless of price. [NEEDS CLARIFICATION: whether a taker may be given a worse price to
  keep an own-outcome sell ahead of a cross-outcome buy, as spec 003 said, or the better price
  as this spec says]

## Requirements *(mandatory)*

### Functional Requirements

**The value**

- **FR-001**: The value MUST be the exchange of one market: its outcomes, its unit, its maximum
  order cost and maximum position, its state (open, suspended, closed, final), one book per
  outcome, every
  order it has accepted by id with its state (resting, filled in part, filled, cancelled,
  expired), the positions per owner and outcome (quantity, quantity resting under sells, cost),
  the sets outstanding, the remainder, the last price of each outcome, the next sequence number
  and the time of its last event.
- **FR-002**: The commands MUST be open, place, cancel, split, merge, suspend, resume, close and
  finalise, each carrying the time it is applied at. A time earlier than the value's last event MUST be refused
  as out of order. Every command MUST yield either a refusal naming its reason, with no event and
  no change, or the next value and its events; replaying a value's events onto an empty value
  MUST yield an equal value.
- **FR-003**: The domain MUST read no clock, wallet, player, database or network, and MUST depend
  on `ankka-core` alone (spec 004 FR-003).

**Orders**

- **FR-004**: A place MUST carry an order id, an owner, an outcome, buy or sell, a price, a
  quantity and a duration, and MUST be refused naming the field when the price is outside one to
  one less than the unit, the quantity is not a positive whole number, the outcome is not the
  market's, or the state is not open; a buy MUST be refused naming the limit when its cost
  (quantity times price) is above the maximum order cost or when the owner's shares held plus
  shares resting to buy plus the quantity is above the maximum position; a sell MUST be refused
  naming the quantity held when the quantity is above what the owner holds beyond what rests
  under their other sells.
- **FR-005**: An accepted buy MUST record its cost; a cancel or an expiry MUST release the cost of
  the remaining quantity; a trade MUST charge the buyer the trade's price times the quantity and
  release the difference between that and the order's price times the quantity.
- **FR-006**: A cancel MUST be refused naming the owner when the caller is not the order's owner,
  and naming the state when the order is not resting; a cancelled sell MUST free its shares.
- **FR-007**: A place, split or merge repeated under a known id or key with the same fields MUST
  be answered as the first was and produce no event; under a known id or key with different
  fields it MUST be refused naming the id or key.

**Matching**

- **FR-008**: Each book MUST hold its resting orders by price and then by the sequence the
  exchange accepted them. A buy MUST match resting sells at or below its price, lowest first and
  oldest first within a price, at the resting order's price, until filled or none matches; a
  sell MUST match resting buys at or above its price the same way, highest first. A rest-until-
  close order's remainder MUST rest; a fill-at-once order's remainder MUST be cancelled and its
  cost released; a fill-whole order that cannot fill whole MUST be refused as not fillable whole
  and change nothing.
- **FR-009**: An order MUST never match an order of its own owner; the book MUST skip it.
- **FR-010**: Every trade MUST carry the exchange's next sequence number, from one; the same
  sequence of commands MUST always produce the same trades with the same numbers.
- **FR-011**: In a two-outcome market a buy of one outcome at price p MUST also be matched
  against resting buys of the other at price q where p plus q is one unit or more, by splitting a
  set: the resting order is charged q, the taker is charged one unit less q and its order releases
  the difference to p; a sell of one outcome at p MUST also be matched against resting sells of
  the other at q where p plus q is one unit or less, by merging: the resting order is paid q and
  the taker one unit less q. Across a resting sell of the own outcome and a resting buy of the
  other, the taker MUST take the better price, the own outcome first when they are equal, and
  oldest first within either. A trade through the pool MUST set the last price of both outcomes.
- **FR-012**: A market with more than two outcomes MUST match directly and through explicit
  splits and merges only.

**Positions and the pool**

- **FR-013**: A trade MUST move the quantity from the seller's position to the buyer's; a buy
  MUST add quantity times the trade's price to the buyer's cost, and a sale MUST remove the sold
  quantity's part of the seller's cost rounded down to the minor unit.
- **FR-014**: A split MUST charge one unit per set, add one share of every outcome to the owner's
  positions with an equal part of the unit per share as cost rounded down, and add the sets to the
  sets outstanding; a merge MUST take one share of every outcome, pay one unit per set and remove
  the sets. A split MUST respect the maximum position; a merge MUST be refused naming the quantity
  held when the owner lacks the shares.
- **FR-015**: After every command, every outcome's total of shares held MUST equal the sets
  outstanding, and the pool MUST equal the sets outstanding times the unit plus the remainder.

**Suspension**

- **FR-015a**: A suspend MUST be refused naming the state unless the exchange is open; suspended,
  every place, split or merge MUST be refused naming the state, a cancel MUST be applied as while
  open, and a close MUST be applied as while open. A resume MUST be refused naming the state unless
  the exchange is suspended; resumed, the exchange MUST be open with every order that rested.
  Neither moves money, and neither is keyed.

**Close and finality**

- **FR-016**: A close MUST expire every resting order, release every expired buy's remaining
  cost, free every expired sell's shares, and set the state to closed; every place, split or merge
  after it MUST be refused naming the state. A close repeated MUST produce no event.
- **FR-017**: A finalise MUST be refused naming the state unless the exchange is closed. Made
  final resolved, the exchange MUST pay each position one unit per share of the resolved outcome
  and nothing for the others; made final void, it MUST pay each share of every outcome one unit
  divided by the number of outcomes rounded down, and keep the remainder in the pool, recorded.
  Every payout MUST be keyed by the market and the position and made once; after it the sets
  outstanding MUST be zero and the state final. A finalise repeated MUST produce no event.

**Events and money**

- **FR-018**: Every event that moves money MUST name the amount, the owner and what it is keyed
  by: a reservation by the order id, a release by the order id and its ordinal for that order
  (from one, since one order can release more than once), a trade's legs by the trade, each leg
  naming its order, charge, payment and release and the trade naming how it was made (direct,
  split, merge), a split or a merge by its key, a payout by the market and the position; so that
  a ledger can be driven from the events alone (spec 004 research R14).
- **FR-019**: The events MUST be at least: opened, order placed, trade made, order filled, order
  cancelled, order expired, sets split, sets merged, suspended, resumed, closed, position paid,
  remainder kept, final;
  and MUST evolve as spec 004 FR-009 says.
- **FR-020**: The value MUST answer, per outcome, the last price (or none), the best bid, the
  best ask and the depth as quantity per price.

### Key Entities

- **Exchange**: the value (FR-001).
- **Book**: one outcome's resting orders by price and sequence.
- **Order**: id, owner, outcome, side, price, quantity, remaining, duration, state, cost.
- **Trade**: sequence number, the two orders, the quantity, the price of each side, the kind
  (direct, split, merge), what it charges, pays and releases.
- **Position**: owner, outcome, quantity, quantity resting under sells, cost.
- **Pool**: sets outstanding times the unit, plus the remainder.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every scenario under `features/exchange/` and the exchange scenarios of
  `features/libraries/domain.feature` pass against the value alone, with no runtime started.
- **SC-002**: A property suite over at least 100,000 random command sequences finds no sequence
  that breaks FR-015, leaves a position's quantity negative, leaves the sum of resting buys' costs
  unequal to what the events say is reserved, or replays to a value unequal to the one produced.
- **SC-003**: A place on an exchange with 10,000 resting orders across its books is answered in
  under 1 ms at the median on a laptop.
- **SC-004**: eitheror's `exchange` service adds no rule to these: its code is the ports, the
  endpoints and the facts (spec 004 SC-004).

## Assumptions

- The unit is carried when the exchange opens and is one hundred for a currency with two
  decimals; the domain never knows the currency.
- The limits default, in the consumer, to 1,000 units for the maximum order cost and 10,000
  shares for the maximum position (spec 003's defaults); the domain takes what it is given.
- No short selling: a sell needs shares. The other side of a two-outcome market is a buy of the
  other outcome.
- Fees are none; a later spec names them and the event that carries them.
- The order ids and keys the domain remembers are every id it has accepted; the consumer bounds
  what it keeps in a journal by the exchange's life, which ends when it is final.

## Dependencies

- Spec 004 (the libraries, the ports, the journal as a contract). Spec 006 (the markets domain)
  decides when an exchange opens, closes and is made final; the consumer drives one from the
  other.
- ankka-modules spec 022 (the ledger these events drive) and spec 023 (the consumer).

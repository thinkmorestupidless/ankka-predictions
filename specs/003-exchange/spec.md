# Feature Specification: Exchange

**Feature Branch**: `003-exchange`

**Created**: 2026-10-20

**Status**: Superseded

*Superseded on 2026-10-21 (decision D1): the book, matching, positions, complete sets and settlement are restated as the exchange domain in spec 005; the gates, the reservation, the facts, the price stream and the price observations are eitheror's, carried by ankka-modules specs 022 and 023. This spec is kept as written and is no longer checked against the living features.*

**Input**: User description: "The `exchange` service, the core of the product: a limit order book for every outcome of every open market, matching buy and sell orders by price and then by time, with partial fills, as quickly as possible at scale. Orders are checked against the player's status, the KYC play gate and the responsible-gambling bet gate, and their cost is reserved in the wallet before they reach a book. Positions per player, market and outcome; complete sets split from and merged into the market's pool; prices, depth and trades for the interface; settlement of every position when a market is final; facts on `exchange.orders`, `exchange.trades` and `exchange.prices`; price observations into the reasoning graph."

## Context

The ledger (spec 001) reserves, releases, settles and pays; the catalogue (spec 002) opens,
closes and finalises markets. This service is what stands between them: the books. A share of an
outcome pays one unit if the market resolves to that outcome. A buyer pays a price from 1 to 99
minor units per share, and the seller is either another player parting with shares or, through a
complete set, the market's pool: one share of every outcome together is worth exactly one unit
whatever happens, so a set can always be bought from the pool for one unit and sold back for one
unit. That is what makes every book liquid from the first order.

The book must be deterministic: the same orders in the same sequence produce the same trades,
every time, so that a replay is a proof and an audit is a replay. Money never waits on the book
and the book never waits on money: a buy order's cost is reserved before it is accepted, and
trades move money afterwards, keyed by trade, at least once.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A player places a buy order that is checked, reserved and rests in the book (Priority: P1)

A player places a buy order for an outcome of an open market: a price from 1 to 99 and a quantity
of whole shares. Before it is accepted the service checks that the player's account is active,
that the KYC play gate allows play, that the bet gate allows a stake of the order's cost, and
reserves quantity times price in the player's wallet. An order any check refuses never reaches the
book and reserves nothing. An accepted order rests in the outcome's book at its price, behind
earlier orders at that price, until it is matched, cancelled by the player, or the market closes.

**Why this priority**: Nothing trades until orders can be placed safely; the gates and the
reservation are what make the book trustworthy.

**Independent Test**: Place orders with each gate scripted to allow and to refuse, and with a
wallet short of money; confirm what rests, what is reserved and every refusal by name.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/orders.feature`: a buy order at a price and a quantity rests in the outcome's book and reserves quantity times price
- added `features/exchange/orders.feature`: a buy order whose cost exceeds the player's available money is refused as insufficient and rests nowhere
- added `features/exchange/orders.feature`: a buy order by a player whose account is not active is refused naming the status
- added `features/exchange/orders.feature`: a buy order the play gate refuses is refused naming the play gate
- added `features/exchange/orders.feature`: a buy order the bet gate refuses is refused naming the bet gate
- added `features/exchange/orders.feature`: a buy order when a gate cannot be reached is refused as unavailable
- added `features/exchange/orders.feature`: an order at a price outside one to ninety-nine is refused
- added `features/exchange/orders.feature`: an order for a quantity that is not a positive whole number is refused
- added `features/exchange/orders.feature`: an order on a market that is not open is refused naming the state
- added `features/exchange/orders.feature`: an order naming an outcome the market does not have is refused
- added `features/exchange/orders.feature`: a player cancels a resting buy order and its reservation is released
- added `features/exchange/orders.feature`: a player's cancel of another player's order is refused
- added `features/exchange/orders.feature`: a repeat of an order under the same key is answered as the first was and reserves nothing more

---

### User Story 2 - Orders match by price and then by time, with partial fills (Priority: P1)

A buy order matches the resting sell orders of the same outcome whose price is at or below its
own, lowest price first and oldest first at one price, at the resting order's price, until it is
filled or no sell matches; what remains rests. A sell order matches resting buys at or above its
price the same way, highest first. An order may be filled by several trades; a trade may fill part
of either order. A player may ask that an order be filled as far as it can be at once and the
rest cancelled, or that it be filled whole at once or not at all.

**Why this priority**: This is the engine.

**Independent Test**: Seed books with known resting orders; place orders that match none, one,
several and part of one; confirm every trade's price, quantity, parties and order, and the book
after; replay the same sequence and confirm the same trades.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/matching.feature`: a buy order matches the lowest resting sell at or below its price at the resting order's price
- added `features/exchange/matching.feature`: a buy order matches several resting sells in order of price and then time until it is filled
- added `features/exchange/matching.feature`: a buy order larger than every matching sell is filled in part and the rest rests
- added `features/exchange/matching.feature`: a buy order smaller than the resting sell fills in part and the sell keeps resting with what remains
- added `features/exchange/matching.feature`: a buy order matches no sell above its price and rests whole
- added `features/exchange/matching.feature`: a sell order matches the highest resting buy at or above its price at the resting order's price
- added `features/exchange/matching.feature`: two resting orders at one price are matched oldest first
- added `features/exchange/matching.feature`: an order to fill what it can at once and cancel the rest leaves nothing resting
- added `features/exchange/matching.feature`: an order to fill whole at once or not at all is refused when the book cannot fill it whole
- added `features/exchange/matching.feature`: the same orders in the same sequence produce the same trades
- added `features/exchange/matching.feature`: an order never matches an order of the same player

---

### User Story 3 - Positions, complete sets and the pool (Priority: P1)

A trade adds shares to the buyer's position in that outcome and takes them from the seller's. A
sell order needs shares the player holds and are not already resting under another sell order. A
player may split: pay one unit per set to the market's pool and receive one share of every
outcome; or merge: give one share of every outcome and receive one unit from the pool. In a
two-outcome market, a buy of one outcome at a price is matched, when no sell matches better,
against a buy of the other outcome whose price makes one unit or more with it, by splitting a set
from the pool between the two buyers at their prices; and a sell of one outcome is matched against
a sell of the other the same way by merging into the pool.

**Why this priority**: Without the pool a new market has no sellers; without positions there is
nothing to pay at resolution.

**Independent Test**: Trade between two players and against the pool; confirm positions, the pool
and both wallets after each step; confirm the pool always holds one unit per set outstanding.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/positions.feature`: a trade adds the quantity to the buyer's position and removes it from the seller's
- added `features/exchange/positions.feature`: a sell order for more shares than the player holds is refused
- added `features/exchange/positions.feature`: a sell order for shares already resting under another sell order is refused
- added `features/exchange/positions.feature`: a cancelled sell order frees its shares to be sold again
- added `features/exchange/positions.feature`: a player reads their positions with quantity and cost
- added `features/exchange/complete-sets.feature`: a split pays one unit per set to the pool and gives one share of every outcome
- added `features/exchange/complete-sets.feature`: a split larger than the player's available money is refused
- added `features/exchange/complete-sets.feature`: a merge gives one share of every outcome and receives one unit per set from the pool
- added `features/exchange/complete-sets.feature`: a merge of more sets than the player holds of every outcome is refused
- added `features/exchange/complete-sets.feature`: the pool holds one unit for every set outstanding
- added `features/exchange/complete-sets.feature`: in a two-outcome market a buy of one outcome matches a buy of the other whose prices make one unit or more by splitting a set between them
- added `features/exchange/complete-sets.feature`: in a two-outcome market a buy matches a resting sell of its own outcome before a buy of the other at the same price
- added `features/exchange/complete-sets.feature`: in a two-outcome market a sell of one outcome matches a sell of the other whose prices make one unit or less by merging a set into the pool

---

### User Story 4 - A market closes and its positions are settled when it is final (Priority: P1)

When a market closes, every resting order on it is cancelled and every reservation released, and
no order is accepted. When the market is final, each share of the resolved outcome is paid one
unit from the pool and every other share nothing; when a void is final, every share of every
outcome is paid an equal part of one unit. A payout is made once per position, and the pool is
empty when every position is paid.

**Why this priority**: It is where money is decided; it is proved before anything is shown.

**Independent Test**: Close a market with resting orders; finalise a resolution and a void;
confirm the cancellations, the releases, every payout and the pool after.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/close-and-settlement.feature`: a market's close cancels every resting order and releases every reservation
- added `features/exchange/close-and-settlement.feature`: an order on a closed market is refused
- added `features/exchange/close-and-settlement.feature`: a final resolution pays one unit per share of the resolved outcome, nothing for the other outcomes, and empties the pool
- added `features/exchange/close-and-settlement.feature`: a final void pays every share of every outcome an equal part of one unit
- added `features/exchange/close-and-settlement.feature`: a position is paid once however many times the final fact is read
- added `features/exchange/close-and-settlement.feature`: a resolution that is not yet final pays nothing

---

### User Story 5 - Prices, depth and trades for the interface and the graph (Priority: P2)

For every outcome a player reads the last price, the best bid, the best ask, the depth (the
quantity resting at each price), the recent trades and the price over time; and holds a stream of
changes. Each time an outcome's last price moves, the service records a price observation in the
reasoning graph, at most once a minute per outcome.

**Why this priority**: The interface (spec 005) shows these; the reasoning graph compares them
with beliefs.

**Independent Test**: Trade and read; confirm each figure and the stream; count observations in
the graph against trades in a minute.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/prices.feature`: a trade sets the outcome's last price
- added `features/exchange/prices.feature`: the best bid is the highest resting buy price and the best ask the lowest resting sell price
- added `features/exchange/prices.feature`: the depth lists the quantity resting at each price
- added `features/exchange/prices.feature`: a player holding the price stream sees a trade within a second
- added `features/exchange/prices.feature`: a moved last price is recorded in the reasoning graph as a price observation at most once a minute per outcome

---

### User Story 6 - Every order, trade and price is a fact (Priority: P2)

Every order's life (placed, filled in part, filled, cancelled, expired at close) is published on
`exchange.orders`, every trade on `exchange.trades`, and every change of last price on
`exchange.prices`. The wallet's settlement of a trade is driven from the trade's fact, keyed by
the trade, at least once.

**Why this priority**: Settlement, reporting and the player's history read facts; it comes after
the engine because the engine is proved first.

**Independent Test**: Trade; fold the facts; compare to the books, positions and wallets.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/exchange/facts.feature`: every change to an order is published as one fact naming the order and its state
- added `features/exchange/facts.feature`: every trade is published as one fact naming both orders, the price and the quantity
- added `features/exchange/facts.feature`: a trade's money is settled in the wallet once however many times its fact is read
- added `features/exchange/facts.feature`: folding a market's facts gives its books and positions

---

### Edge Cases

- A buy at 60 and a resting sell at 55 trade at 55; the buyer's reservation releases 5 per share
  (spec 001).
- A buy order's quantity times price exceeds the product's maximum order cost, or the player's
  position would exceed the maximum position: refused naming the limit.
- The wallet's reservation succeeds but the order is then refused by the book (market closed in
  between): the reservation is released and the player sees the refusal.
- The wallet cannot be reached: the order is refused as unavailable and nothing rests.
- A trade's settlement is refused by the wallet (which the reservation should make impossible):
  the trade stands in the book and the refusal is raised to an operator; positions are not
  unwound. [NEEDS CLARIFICATION: whether a trade whose settlement the wallet refuses is reversed
  in the book, or stands and is settled by an operator's adjustment; the former keeps the ledger
  exact, the latter keeps the book deterministic.]
- Two players' orders arrive at one book at once: they are matched in the order the book accepted
  them, and the facts say which was first.
- A market with more than two outcomes: only direct matches and explicit splits and merges; no
  cross-outcome matching (assumption).
- A cancel arrives after the order was filled: refused as filled.
- A price observation when the graph is unavailable: dropped; the next move is recorded.

## Requirements *(mandatory)*

### Functional Requirements

**Orders**

- **FR-001**: A player MUST be able to place an order naming a market, an outcome, buy or sell, a
  price from 1 to 99 minor units per share, a quantity of whole shares above zero, a key, and one
  of three durations: rest until the market closes (the default), fill what can be filled at once
  and cancel the rest, or fill whole at once or not at all. A field outside these MUST be refused
  naming it.
- **FR-002**: Before a buy order is accepted the service MUST confirm from its view of the players'
  facts that the account is active, MUST ask the KYC play gate and the responsible-gambling bet
  gate with the order's cost, and MUST reserve quantity times price in the player's wallet under
  the order's key; any refusal MUST stop the order, naming its source, and a gate or the wallet
  that cannot be reached MUST refuse as unavailable. Before a sell order is accepted the service
  MUST confirm the player holds the quantity in that outcome beyond what rests under other sell
  orders, and MUST reserve those shares.
- **FR-003**: An order MUST be refused when its cost exceeds the product's maximum order cost or
  the player's position in the outcome would exceed the maximum position, both operator settings
  per market or by default.
- **FR-004**: A player MUST be able to cancel their own resting order; the remaining reservation
  or reserved shares MUST be released. A cancel of another player's order, or of an order that is
  filled, MUST be refused.
- **FR-005**: A repeat of an order under the same key MUST be answered as the first was.

**The book and matching**

- **FR-006**: Each outcome of an open market MUST have one book of resting buy and sell orders,
  ordered by price and then by the time the book accepted them. A buy MUST match resting sells at
  or below its price, lowest price first and oldest first within a price, at the resting order's
  price, until filled or none matches; a sell MUST match resting buys at or above its price the
  same way, highest first. What remains of a rest-until-close order MUST rest; of a fill-at-once
  order MUST be cancelled; a fill-whole order that cannot fill whole MUST be refused and MUST
  leave the book unchanged.
- **FR-007**: An order MUST never match an order of the same player; the book MUST skip it and
  match the next.
- **FR-008**: The same sequence of accepted orders MUST produce the same trades, and a book MUST
  be reconstructable from its orders alone.
- **FR-009**: In a two-outcome market, a buy of one outcome at price p MUST be matched, after the
  resting sells of its own outcome at or below p, against resting buys of the other outcome at
  price q where p plus q is one unit or more, oldest first, by splitting a set from the pool: each
  buyer receives their share and pays their own price, and the surplus (p plus q less one unit)
  MUST be returned to the taker. A sell of one outcome at p MUST likewise be matched, after
  resting buys of its own outcome, against resting sells of the other at q where p plus q is one
  unit or less, by merging the set into the pool, each seller receiving their own price and the
  shortfall taken from no one (the pool pays one unit, split as the prices say, with the
  remainder to the taker).

**Positions and complete sets**

- **FR-010**: A position MUST hold, per player, market and outcome, the quantity, the quantity
  reserved under resting sells, and the cost; a trade MUST move the quantity from seller to buyer
  and update both costs.
- **FR-011**: A player MUST be able to split: reserve and settle one unit per set to the market's
  pool and receive one share of every outcome; and merge: give one share of every outcome and be
  paid one unit per set from the pool. Both MUST be keyed and refused when the player lacks the
  money or the shares.
- **FR-012**: The pool MUST hold exactly one unit for every complete set outstanding, where sets
  outstanding is the smallest quantity held of any outcome across all positions plus every share
  beyond it; stated plainly, the pool MUST equal the sum over outcomes of shares held, divided by
  the number of outcomes, times one unit, and this MUST hold after every trade, split, merge and
  payout.

**Close and settlement**

- **FR-013**: On the market's closed fact, the service MUST cancel every resting order on it and
  release every reservation and reserved share, and MUST refuse any order from then on.
- **FR-014**: On the market's final fact, the service MUST pay each position: one unit per share
  of the resolved outcome, nothing for the others; for a void, an equal part of one unit per
  share of every outcome, rounded down to the minor unit with the remainder staying in the pool
  and recorded. Each payout MUST be keyed by market and position and made once. A resolution not
  yet final MUST pay nothing.

**Prices and the graph**

- **FR-015**: For every outcome the service MUST answer the last price, the best bid, the best
  ask, the depth as quantity per price, the recent trades and the price over time, and MUST
  stream changes to a holder within one second of a trade.
- **FR-016**: Each time an outcome's last price moves, the service MUST record a price
  observation in the reasoning graph for the market and the outcome, at most once a minute per
  outcome; an unavailable graph MUST lose the observation and nothing else.

**Facts**

- **FR-017**: Every change to an order MUST be published on `exchange.orders` (placed, filled in
  part, filled, cancelled, expired), every trade on `exchange.trades` (both orders, both players,
  the outcome, the price, the quantity, the kind: direct, split or merge, and the sequence in the
  book), and every change of last price on `exchange.prices`; all keyed by the market, with stable
  fact ids.
- **FR-018**: The wallet's settlement of a trade MUST be driven from the trade's fact, keyed by
  the trade, at least once, and MUST produce the same ledger however many times the fact is read.

### Key Entities *(each a term in the project glossary)*

- **order**: a player's instruction: market, outcome, buy or sell, price, quantity, duration,
  key; its state: resting, filled in part, filled, cancelled, expired.
- **book**: one outcome's resting orders, by price and time.
- **trade**: one match: two orders, a price, a quantity, a kind, a sequence number.
- **position**: a player's shares of an outcome: quantity, reserved quantity, cost.
- **pool**: the market's wallet holding one unit per complete set outstanding.
- **complete set**: one share of every outcome.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: An order is accepted or refused within 50 ms at the median and 150 ms at the 99th
  percentile, gates and reservation included, while one book takes 1,000 orders a second and the
  service 10,000 a second across books.
- **SC-002**: A trade is visible on the price stream within 1 second in 99% of cases.
- **SC-003**: 100% of replays of a recorded sequence of orders produce the same trades.
- **SC-004**: After any sequence a test produces, the pool equals one unit per set outstanding,
  every position's quantity is non-negative, and the sum of all players' reservations on a market
  equals the cost of its resting buys.
- **SC-005**: Every position of a final market is paid within 60 seconds of the final fact, once.
- **SC-006**: With 100 open markets and 10,000 players, a player's positions and a book's depth
  answer within 200 ms at the 99th percentile.

## Assumptions

- Prices tick in one minor unit and shares are whole; a stake is turned into a quantity by the
  interface, rounding down.
- No short selling: a sell needs shares. A player wanting the other side of a two-outcome market
  buys the other outcome.
- Cross-outcome matching (FR-009) applies to two-outcome markets only; markets with more outcomes
  match directly and through explicit splits and merges.
- The KYC play gate and the bet gate are asked per buy order; a sell order takes no money and
  asks neither.
- Maximum order cost and maximum position default to 1,000 units and 10,000 shares; operators set
  them per market.
- A void pays each share one unit divided by the number of outcomes, rounded down.
- Fees are none in this feature; a later spec names them and where they are taken.
- The exchange's domain (book, matching, positions, sets) is a module with no dependency on the
  rest of the product, so that it can be reused for another exchange.

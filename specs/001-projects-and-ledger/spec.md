# Feature Specification: Projects and the Ledger

**Feature Branch**: `001-projects-and-ledger`

**Created**: 2026-10-20

**Status**: Superseded

*Superseded on 2026-10-21 (decision D1): the projects and the ledger are eitheror's, carried by ankka-modules specs 021 and 022. This spec is kept as written and is no longer checked against the living features.*

**Input**: User description: "Projects and the ledger. The product runs as two ankka projects on one installation: `predictions`, which holds the product's own services and the casino's player-facing services deployed from ankka-modules's images unchanged; and `payments`, ankka-modules's shared payments project, deployed from its manifests unchanged, to which `predictions` is a merchant with the brand slug `predictions`. [...] The `wallet` service is the one ledger of the product: one wallet per player per currency, holding available money and reserved money. [...] Every change to a wallet is a fact on the topic `money.transactions`." (The full description is the conversation that produced this spec; `docs/architecture.md` carries the map it describes.)

## Context

The product settles real money through the payments stack eitheror's casino is building on ankka.
The casino's `merchant` credits a brand's wallet on one granted route when a deposit is captured,
and its `payouts` places, releases, confirms and reverses holds on that wallet while a withdrawal
is reviewed and paid. Both call a service named `wallet` at paths the casino's spec 002 fixes.

The casino's own wallet is a ledger of rounds: a bet opens one and a single win, loss or rollback
closes it, with bonus money, wagering and turnover around it. An exchange needs something else:
money reserved against a resting order for as long as it rests, moved from one player to another
when a trade settles, released when an order is cancelled or filled at a better price, and paid
out per position when a market resolves. So this product keeps its own `wallet`, honouring the
casino's routes where the casino calls it, and serving the exchange's primitives within the
project. The casino's `players`, `kyc`, `responsible-gambling` and `payouts` are deployed here
unchanged, as is the shared `payments` project; see `notes/casino-requests.md` for what that asks
of ankka-modules.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A deposit arrives and is credited after the responsible-gambling gate (Priority: P1)

A player deposits through the payments stack. When the payment is captured, `merchant` calls the
wallet's deposit route with the payment id as the key. The wallet asks `responsible-gambling`
whether the player may deposit this amount, passing the player's deposit sums over the last 24
hours, 7 days and 30 days, and credits available money or refuses with the casino's reason. A
player's wallet in a currency comes into being with their first deposit in it. A repeat of the
same payment is answered as the first was.

**Why this priority**: Nothing else in the product moves until money can enter it, and the
deposit route is the first seam with the casino; if its shape is wrong, `merchant` cannot be
deployed unchanged.

**Independent Test**: Stand the wallet up with a scripted `responsible-gambling`; call the deposit
route as `merchant` would, with allowed, excluded, cooling-off, over-limit and unavailable answers
from the gate; confirm the balances, the refusals and the repeat.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/money/deposits.feature`: a first deposit creates the player's wallet in that currency and credits available money
- added `features/money/deposits.feature`: a deposit the deposit gate allows is credited and answers the balances and the transaction
- added `features/money/deposits.feature`: a deposit by an excluded player is refused as excluded and credits nothing
- added `features/money/deposits.feature`: a deposit by a player cooling off is refused naming the end of the break
- added `features/money/deposits.feature`: a deposit over a deposit limit is refused naming the period, the limit, the amount used and the amount remaining
- added `features/money/deposits.feature`: a deposit when the deposit gate cannot be reached is refused as unavailable and credits nothing
- added `features/money/deposits.feature`: a repeat of a deposit under the same payment is answered as the first was and credits nothing more
- added `features/money/deposits.feature`: the deposit sums the deposit gate is asked cover the last day, week and month of credited deposits
- added `features/money/deposits.feature`: a deposit whose amount is not a whole number of minor units is refused naming the amount
- added `features/money/deposits.feature`: a later outcome of a payment is recorded against the payment and answers whether it was the first time

---

### User Story 2 - The exchange reserves, releases, settles and pays (Priority: P1)

Before an order reaches a book, `exchange` reserves the order's cost from the player's available
money, keyed by the order id; the reservation is refused when available money is short, and the
order never reaches the book. When an order is cancelled or expires, the remaining reservation is
released. When a trade settles, the buyer's reservation gives up quantity times the trade's price
to the seller's available money, and any part of the reservation the trade did not use, because
the trade's price was better than the order's, is released. When a market resolves, each winning
position is paid into available money, keyed by the market and the position.

**Why this priority**: This is the ledger the exchange is built on; the book (spec 003) cannot be
planned without it.

**Independent Test**: Drive reserve, release, settle and pay against two wallets with no exchange
present; confirm every balance after each step, the refusals, and that each key repeated is
answered identically.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/money/reservations.feature`: a reservation moves the amount from available money to reserved money
- added `features/money/reservations.feature`: a reservation larger than available money is refused as insufficient and moves nothing
- added `features/money/reservations.feature`: a release returns part of a reservation to available money and keeps the rest reserved
- added `features/money/reservations.feature`: a release of a whole reservation leaves nothing reserved under that order
- added `features/money/reservations.feature`: a release larger than the reservation is refused and moves nothing
- added `features/money/reservations.feature`: a repeat of a reservation under the same order is answered as the first was and reserves nothing more
- added `features/money/settlement.feature`: a trade moves quantity times price from the buyer's reservation to the seller's available money
- added `features/money/settlement.feature`: a trade at a price better than the buyer's order releases the difference to the buyer's available money
- added `features/money/settlement.feature`: a trade larger than the buyer's reservation is refused and moves nothing
- added `features/money/settlement.feature`: a repeat of a trade under the same key is answered as the first was and moves nothing more
- added `features/money/settlement.feature`: a payout at resolution credits the position's payout to available money
- added `features/money/settlement.feature`: a repeat of a payout for the same market and position pays nothing more
- added `features/money/settlement.feature`: a trade between a player and themselves is refused

---

### User Story 3 - A withdrawal holds money while payouts reviews and pays it (Priority: P2)

The casino's `payouts` asks the wallet for a hold preview, places a hold keyed by the withdrawal
request, and later releases it (the request was cancelled, rejected or failed), confirms it (the
money left) or reverses a confirmed hold (a paid payout was reversed). The product has no bonus
money, so a preview answers zero for every bonus and clawback field and never requires the
player's confirmation; the most withdrawable amount is the available money.

**Why this priority**: Money must be able to leave the product for it to be a product; it comes
after deposits and the exchange primitives because `payouts` is not yet built in the casino.

**Independent Test**: Call the five hold routes as `payouts` would, in each order the casino's spec
007 allows; confirm the balances and refusals, and that a hold and reservations share available
money correctly.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/money/holds.feature`: a hold preview answers the available money as the most withdrawable amount and no confirmation required
- added `features/money/holds.feature`: a hold moves the amount from available money to reserved money under the request
- added `features/money/holds.feature`: a hold larger than available money is refused as insufficient
- added `features/money/holds.feature`: a hold placed while reservations rest takes only from available money
- added `features/money/holds.feature`: a release returns the held amount to available money naming the outcome
- added `features/money/holds.feature`: a confirmation removes the held amount from the wallet
- added `features/money/holds.feature`: a reversal of a confirmed hold returns the amount to available money
- added `features/money/holds.feature`: a release of a hold already confirmed is refused
- added `features/money/holds.feature`: a repeat of a hold under the same request is answered as the first was

---

### User Story 4 - A player reads their wallet; an operator reads and adjusts (Priority: P2)

A signed-in player reads their own balance and transactions, shown in major units. An operator
reads any player's balance, transactions, lifetime totals and deposit sums, and applies a keyed
adjustment with a signed amount, a reason and a category, which is recorded with the operator as
its actor. An adjustment that would take available money below zero is refused.

**Why this priority**: Without it the interface (spec 005) has no account page and support has no
way to correct a wallet.

**Independent Test**: Read as a player with a token for another player's id; adjust as an
operator; confirm the refusals and the recorded actor.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/money/reads.feature`: a player reads their own balance as available and reserved money in major units
- added `features/money/reads.feature`: a player's request about another player's wallet is refused as a mismatch
- added `features/money/reads.feature`: a player reads their transactions by type and period, paged
- added `features/money/adjustments.feature`: an operator's adjustment with a reason and a kind changes available money and records the operator
- added `features/money/adjustments.feature`: an adjustment that would take available money below zero is refused
- added `features/money/adjustments.feature`: a repeat of an adjustment under the same key is answered as the first was

---

### User Story 5 - Every change is a fact and a wallet is reconstructable from its facts (Priority: P2)

Every change to a wallet is published as a fact on `money.transactions`, keyed by player and
currency, carrying the balances after it. A trade produces two facts, one per wallet. A reader
that folds a wallet's facts arrives at the wallet's balances. When `players` files a player's
erasure, the wallet marks nothing but the personal fields it holds: amounts, ids and timestamps
stay, so the ledger survives.

**Why this priority**: Reporting, the affiliate protocol later, and the player's own history read
facts, never the wallet's store; and the regulator's ledger must survive an erasure.

**Independent Test**: Run a sequence of deposits, reservations, trades, holds, payouts and
adjustments; fold the facts; compare to the balances. File an erasure; confirm balances and facts
unchanged except personal fields.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/money/facts.feature`: every change to a wallet is published as one fact carrying the balances after it
- added `features/money/facts.feature`: a trade publishes one fact for the buyer's wallet and one for the seller's
- added `features/money/facts.feature`: folding a wallet's facts in order gives the wallet's balances
- added `features/money/facts.feature`: a refused deposit is published as a fact naming the refusal and leaves the balances as they were
- added `features/money/erasure.feature`: a player's erasure leaves every amount, id and time of their wallet readable

---

### User Story 6 - The two projects and the grants between them (Priority: P3)

The product is deployed as two projects, `predictions` and `payments`, from manifests. The
`predictions` project holds this product's services and the casino's `players`, `kyc`,
`responsible-gambling` and `payouts`; the `payments` project is the casino's, unchanged. The
grants are the casino's: `merchant` may call the wallet's deposit and payment-outcome routes;
`psp-gateway` may call the payouts outcome route; `payouts` may initiate a withdrawal on
`psp-gateway`; `players` may file an erasure in `payments`. Nothing else crosses between them,
and a service in either project calling a route it was not granted is refused.

**Why this priority**: It is the shape everything deploys into; it has no value to a player on its
own and is proved by the integration run, not by a scenario a player observes.

**Independent Test**: Apply both manifests to a local installation; call each granted route from
its grantee and from a service that is not; list the grants.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/projects/grants.feature`: the payments project's merchant may credit a deposit on the wallet
- added `features/projects/grants.feature`: a service that was not granted the deposit route is refused
- added `features/projects/grants.feature`: a service within the predictions project may reserve money without a grant
- added `features/projects/grants.feature`: a request naming a brand other than the product's matches no route

---

### Edge Cases

- A deposit arrives for a player who has no wallet in that currency: the wallet is created and
  credited (`deposits.feature`).
- The responsible-gambling gate answers after the wallet's wait: refused as unavailable; a retry
  of the same payment is answered from the first attempt's outcome once there is one
  (`deposits.feature`).
- A settle names a buyer reservation smaller than quantity times price: refused; the exchange's
  book never produces this, and if it does the trade is not settled (`settlement.feature`).
- A hold is placed while reservations rest and available money suffices for the hold alone: the
  hold is placed; reservations and holds both draw on available money and neither on the other
  (`holds.feature`).
- A payout at resolution for a position of zero shares: nothing is credited and nothing is
  published.
- A refund or a lost dispute after a deposit was credited and spent: see FR-008 and the open
  question.
- An operator's adjustment names a currency the player has no wallet in: refused as wallet not
  found; adjustments never create wallets.
- A key reused with a different body: answered as the first was, which the caller must treat as a
  mismatch (the casino's `KEY_REUSED` is reserved for this).

## Requirements *(mandatory)*

### Functional Requirements

**Money rules**

- **FR-001**: Every amount at every boundary MUST be a whole number of minor units of one ISO 4217
  currency named with it; a decimal, a negative amount where none is allowed, or a currency the
  product does not offer MUST be refused naming the amount. Nothing MUST convert between
  currencies, and no read MUST sum across them.
- **FR-002**: Every command that changes a wallet MUST carry a key chosen by the caller; a repeat
  under the same key MUST be answered as the first was and change nothing. [NEEDS CLARIFICATION:
  how many recent keys a wallet remembers, and whether the lifetime record of a key lives in the
  caller (the casino keeps it in the caller's workflow and the wallet keeps its last 1,000) or in
  the wallet; the exchange's order and trade keys are far more numerous than a casino's bets.]
- **FR-003**: A wallet MUST hold available money and reserved money, both never negative. Reserved
  money MUST at every moment equal the sum of the wallet's open reservations and open holds.

**Deposits, on the casino's granted routes**

- **FR-004**: The wallet MUST serve the casino's deposit route at the casino's path and with the
  casino's body (`paymentId` as the key, `amount`, `terminal`, `psp`, `scheme`, `paymentProvider`)
  for `service:payments/merchant` alone, under a grant the `predictions` project makes.
- **FR-005**: Before crediting, the wallet MUST ask `responsible-gambling` `may-deposit` with the
  player, the currency, the amount and the player's credited deposit sums over the last 24 hours,
  7 days and 30 days, and MUST answer the casino's refusals: `EXCLUDED`; `COOLING_OFF` with the
  end; `OVER_LIMIT` with the period, the limit, the amount used and the amount remaining;
  `RG_UNAVAILABLE` when the gate cannot be reached, which refuses.
- **FR-006**: A deposit the gate allows MUST credit available money, create the wallet if the
  player has none in that currency, and answer the balances and the transaction id, the deposit's
  ordinal and the deposit sums.
- **FR-007**: A deposit refused MUST credit nothing and MUST be published as a fact naming the
  refusal.
- **FR-008**: The wallet MUST record later outcomes of a payment (refunded, dispute opened,
  dispute resolved, with the casino's body) against the payment on the casino's payment-outcome
  route, answering whether this outcome was recorded for the first time. [NEEDS CLARIFICATION:
  whether a refund or a lost dispute debits available money, and what happens when the money was
  already spent; the casino's wallet records the outcome and this spec needs its rule.]

**Holds, for `payouts`**

- **FR-009**: The wallet MUST serve hold preview, hold, release, confirm and reverse within the
  project for `payouts`, keyed by the withdrawal request id, with the casino's bodies and answers.
  A preview MUST answer available money as the most withdrawable amount, no confirmation required,
  and zero for every bonus, forfeit and clawback field. A hold MUST move the amount from available
  to reserved money and be refused as insufficient when available money is short. A release MUST
  return the held amount to available money naming the outcome. A confirmation MUST remove the
  held amount from the wallet. A reversal of a confirmed hold MUST return the amount to available
  money. Any step out of order MUST be refused naming the hold's state.

**The exchange's primitives, within the project**

- **FR-010**: Reserve (key: the order id) MUST move an amount from available to reserved money
  under that order, refused as insufficient when available money is short.
- **FR-011**: Release (key: the order id and a release ordinal) MUST return all or part of a
  reservation to available money, refused when it names more than the reservation holds.
- **FR-012**: Settle (key: the trade id) MUST name the buyer's order, the seller, a quantity and a
  price; MUST move quantity times price from the buyer's reservation to the seller's available
  money; MUST release to the buyer any part of the reservation the filled quantity would have cost
  at the order's price above what it cost at the trade's price; MUST be refused when the
  reservation is short or when buyer and seller are one wallet; and MUST be answered as one
  outcome for both wallets.
- **FR-013**: Pay (key: the market id and the position id) MUST credit a payout to available
  money from the market's pool, creating no wallet: a player with a position has a wallet. A
  market's pool is a wallet of its own in the market's currency, owned by the market and not by any
  player, which only the exchange's settlement of complete sets and payouts touch (spec 003); it
  is never read through a player route and never holds reservations.
- **FR-014**: The wallet MUST answer, within the project, balance (available, reserved, currency),
  transactions (paged, by type and period), lifetime totals (deposited, reserved, settled, paid
  out) and deposit sums.

**Players, operators, erasure**

- **FR-015**: Player routes under the casino's player prefix MUST answer the signed-in player's
  own balance and transactions in major-unit strings, admitted by the player realm's token whose
  subject is the player; a request about another player MUST be refused as a mismatch.
- **FR-016**: Operator routes under the casino's backoffice prefix MUST serve the reads and a
  keyed adjustment with a signed amount, a reason, a category and the operator as actor; an
  adjustment that would take available money below zero MUST be refused, and one naming a wallet
  that does not exist MUST be refused as wallet not found.
- **FR-017**: The wallet MUST serve `players` the erasure handler for a player's wallets, marking
  only the personal fields it holds (an adjustment's reason, a payment's customer fields if any);
  amounts, ids, timestamps and balances MUST stay readable.

**Facts**

- **FR-018**: Every change to a wallet MUST be published as one fact on `money.transactions`,
  keyed by player and currency, with a stable fact id and the balances after it, of one of these
  families: deposit credited, deposit refused, payment outcome recorded, hold placed, hold
  released, hold confirmed, hold reversed, reservation placed, reservation released, trade settled
  (one per wallet), payout paid, adjustment applied. Families MUST change by addition only.
- **FR-019**: Folding a wallet's facts in order MUST give the wallet's balances.

**Projects**

- **FR-020**: The product MUST deploy as two projects from manifests: `predictions`, holding this
  product's services and the casino's `players`, `kyc`, `responsible-gambling` and `payouts`
  unchanged; and `payments`, the casino's shared project unchanged, in which `predictions` is a
  merchant under the brand slug `predictions`.
- **FR-021**: The grants MUST be exactly the casino's that apply: made by `predictions` to
  `service:payments/merchant` for the wallet's deposit and payment-outcome routes and to
  `service:payments/psp-gateway` for the payouts outcome route; received from `payments` by
  `service:predictions/payouts` for withdrawal initiation and by `service:predictions/players` for
  erasure. A route granted MUST refuse any caller but its grantee; a route within the project MUST
  admit the project's services by identity and nothing from outside.
- **FR-022**: Every route MUST keep its `{brand}` segment and MUST match no route for a brand
  other than `predictions`.

### Key Entities *(each a term in the project glossary)*

- **wallet**: a player's ledger account in one currency: available money, reserved money, the open
  reservations and holds, recent keys.
- **reservation**: reserved money held under one order; released or settled.
- **hold**: reserved money held under one withdrawal request; released, confirmed or reversed.
- **deposit**: money credited from a payment, with its ordinal among the player's deposits.
- **payout**: money credited to a position at a market's resolution.
- **key**: the caller's identifier for a money command.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A deposit is credited or refused within 2 seconds of `merchant`'s call, including
  the responsible-gambling gate, in 99% of calls.
- **SC-002**: Reserve, release, settle and pay are answered within 20 ms at the median and 100 ms
  at the 99th percentile while the wallet takes 500 such commands a second across wallets.
- **SC-003**: A repeat of any keyed command within the key window is answered identically, in
  100% of a test's repeats.
- **SC-004**: No sequence of commands, in any interleaving a test produces, leaves available money
  negative or reserved money different from the sum of open reservations and holds.
- **SC-005**: A wallet's balances after any sequence equal the balances folded from its facts.
- **SC-006**: The casino's `merchant` and `payouts`, deployed unchanged, complete a deposit and a
  withdrawal against this wallet on a local installation.

## Assumptions

- A hold and reservations coexist; each draws on available money alone and neither on the other.
- A voided market pays every share of every outcome an equal part of one unit; spec 003 owns the
  rule, the wallet only pays what it is told.
- Adjustments never take available money below zero and never create a wallet.
- The product has no bonus or restricted money; every field the casino's routes have for them is
  answered zero or empty.
- One currency is offered at launch; the wallet is written for any number.
- `responsible-gambling` is asked within the wallet's deposit flow, as the casino's wallet does,
  and the wallet waits at most the time SC-001 allows before answering unavailable.
- The casino's `players`, `kyc`, `responsible-gambling`, `payouts`, `psp-gateway` and `ingest` are
  built to their specs before this product launches; the wallet is tested against scripted
  stand-ins for them until then.
- Personal data erasure is ankka 042 when it ships; until then personal fields are sealed under a
  key per player, as voice does, and erasure destroys the key.

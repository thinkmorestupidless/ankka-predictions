# Research: the exchange domain

The decisions this plan rests on. R1 to R9 are the rules' shape in code; R10 to R13 the tests and
the fixtures; R14 the components; R15 what the plan asked and was answered. "Verify at
implementation" lists what is asserted and not yet run. "Found during implementation" is appended
as the work goes.

## R1. The shape stands; this spec fills `apply`

**Decision.** Spec 004's `Exchange`, `ExchangeCommand`, `ExchangeEvent`, `Refusal`, `Refusals`,
`Decision`, the ids, the serializers and `replay` stay as they are, by name and by field, with
three additions: `Position.restingToBuy: Long = 0` (R6), `Level(price, quantity)` in the domain
with the reads of FR-020 (R9), and `Quantity.of` (R10). `Refusals.notBuilt` is removed; every
command has a body. `replay` gains the upkeep of `restingToBuy` and nothing else.

**Rests on.** Spec 004 FR-009 (a field is only added, with a default) and its `contracts/artefacts.md`.
`Order.sequence`, `Order.releases`, `Leg.releaseOrdinal`, `TradeMade.prices` and `Through` were
put there by spec 004's R14 for this spec's rules.

**Considered.** A `Trade` record in the value: FR-001 does not list trades, and the consumer
reads them from the events; keeping them would grow the snapshot for nothing.

## R2. A book is two sorted vectors, inserted by binary search

**Decision.** `Book.buys` and `Book.sells` stay `Vector[Resting]`, best first (buys by price
descending then sequence, sells ascending then sequence). `Book.rest` inserts at the index
`search` finds instead of appending and sorting; `Book.remove` filters by order id. Matching takes
from the head. With 10,000 resting orders an insert copies one vector of references (tens of
microseconds) and a removal walks it once; a place that matches walks the head of one or two
vectors. The orders map (`SortedMap[OrderId, Order]`) and the positions map are `O(log n)`.

**Rests on.** SC-003's budget: a place is bounded by one vector copy and a handful of tree
lookups, well under a millisecond on a laptop (verify V4). `Vector`'s structural equality keeps
`==` as replay equality (spec 004 R4).

**Considered.** `SortedMap[(Long, Long), Resting]` keyed by price and sequence: `O(log n)` insert,
but jsoniter-scala writes a map's key as a string, so a tuple key needs a custom codec, and the
snapshot's JSON would change shape. A price-level map of queues: faster depth reads, slower
everything else, more state to keep equal under replay. Neither is needed at 10,000.

## R3. One matcher, driven by the taker, choosing the better price across outcomes

**Decision.** `Matching.place(exchange, order)` returns the trades a place makes and the
exchange after them, or the reason it cannot. It loops: find the best candidate, trade the
smaller of the two remainders, repeat until the taker is filled or no candidate is within its
price. A candidate is the head of the opposite side of the taker's own book (skipping orders of
the taker's owner, which stay where they are), and in a two-outcome market the head of the *same*
side of the other outcome's book (likewise skipping), each with the price the taker would pay or
be paid:

| Taker | Own book | Other book (two outcomes only) | Taker's price through the pool |
|---|---|---|---|
| buy at p | resting sell at s ≤ p | resting buy of the other at q with p + q ≥ unit | unit − q |
| sell at p | resting buy at b ≥ p | resting sell of the other at q with p + q ≤ unit | unit − q |

The better price for the taker wins (lower for a buy, higher for a sell); equal, the own book;
within a book, the head is already oldest first at the best price. A direct trade is at the
resting order's price; a pool trade charges (or pays) the resting order its own price and the
taker the rest of the unit. A fill-whole order runs the same loop without applying it, and is
refused `not fillable whole` when the fillable quantity is short. A market of more than two
outcomes has no other-book candidate (FR-012).

The events of one accepted place, in order: `OrderPlaced` (the whole quantity, the cost,
the sequence), then per trade `TradeMade` followed by `OrderFilled` for each order whose remaining
reached zero (the resting one first, then the taker), then for a fill-at-once remainder
`OrderCancelled` with its release. `replay` rests the taker on `OrderPlaced` and removes it on
`OrderFilled` or `OrderCancelled`, so the next value is the fold of the events (FR-002) with no
special case.

**Rests on.** FR-008 to FR-012 and the clarification of 2026-10-31; the nineteen scenarios of
`matching.feature` and `complete-sets.feature`, in particular "a buy matches a resting sell of its
own outcome before a buy of the other at the same price" (equal: own first) and "a buy takes the
better price of a resting sell of its own outcome and a buy of the other" (58 against 55 through
the pool: the pool wins).

**Considered.** Matching own book to exhaustion, then the other: spec 003's rule, superseded by
the clarification. Removing a same-owner order from the book while skipping it: the order is not
cancelled, it rests; it is passed over.

## R4. Every money figure is on the event, under the key the ledger will use

**Decision.** The amounts and keys, as the settlement consumer reads them (spec 004 R7, R14):

| Event | Amount | Key |
|---|---|---|
| `OrderPlaced` (a buy) | `cost` = quantity × price, reserved by the consumer before the place | the order |
| `TradeMade`, a buy leg | `charge` = quantity × the leg's price; `released` = quantity × (order price − leg price), under `releaseOrdinal` (the order's next, from one; 0 and nothing released when the prices are equal) | the trade; the release by the order and ordinal |
| `TradeMade`, a sell leg | `paid` = quantity × the leg's price; releases nothing | the trade |
| `OrderCancelled`, `OrderExpired` (a buy) | `released` = remaining × price under the order's next ordinal; a sell releases 0 under ordinal 0 | the order and ordinal |
| `SetsSplit` | `charge` = sets × unit | the key |
| `SetsMerged` | `paid` = sets × unit | the key |
| `PositionPaid` | `paid` = quantity × unit (resolved outcome), quantity × ⌊unit / outcomes⌋ (void), 0 (a losing outcome; R6) | the market and the position key |
| `RemainderKept` | what a void left undivided | none; no money moves |

An order's `releases` counter in the value is the last ordinal used, kept by `replay` from the
events, so a release ordinal is never guessed. A sell's cost is 0 and it never releases.

**Rests on.** FR-005, FR-016, FR-018; `domain.feature`'s money scenario; ankka-modules spec 022's
release keyed by order and sequence.

## R5. A repeat under a known id is the first answer; a refused command is not remembered

**Decision.** `place` under an order id the value holds, with the same owner, outcome, side,
price, quantity and duration, answers `Right(Decision(exchange, Vector.empty))`: accepted as the
first was, no event, no change. With any field different: `Refusals.id(order)`. `split` and
`merge` likewise by `SetKey` against `setMoves` (kind, owner, sets): `Refusals.key(key)`. A command
the value refused is not in `orders` or `setMoves`, so its repeat is decided afresh and refused
the same way, which is also "answered as the first was".

**Rests on.** FR-007; the two repeat scenarios; spec 004 R7's placement workflow, which repeats
`place` after a crash and needs the second answer to be the first.

**Considered.** Remembering refusals: more state in the snapshot for a guarantee the purity of
`apply` already gives.

## R6. Positions keep what rests to buy; a split's cost is the unit's equal part; every position is paid at finality

**Decision.** `Position` gains `restingToBuy`, kept by `replay` as `restingUnderSells` is: up on
a buy's `OrderPlaced`, down by the quantity on each of its trade legs and by the remaining on its
cancel or expiry. The maximum-position check for a buy is `quantity + restingToBuy + order
quantity ≤ maxPosition` in the order's outcome; for a split, the same per outcome with the sets.
A sell is refused `quantity held` when `order quantity > quantity − restingUnderSells`. The
maximum order cost applies to a buy's cost alone. `PositionMoves` is unchanged for trades, splits
and merges (a buy leg adds its charge to the cost; a sale removes the sold part rounded down; a
split adds ⌊unit / outcomes⌋ per share).

At finality every position with shares, in every outcome, gets one `PositionPaid`: the resolved
outcome's at one unit per share, a void's at the equal part per share, a losing outcome's with
`paid` 0, so every position is settled and zeroed in the value and in the positions view. The
settlement consumer asks the ledger to pay only when `paid > 0` (R14). *Decided by Trevor,
2026-10-31, over "no event for a losing position".*

**Rests on.** FR-004, FR-013, FR-014, FR-017; the clarification's edge case (a share received
through a split trade counts as bought: it is in `quantity` once the trade is replayed).
`restingToBuy` is a stored field added with a default, which spec 004 FR-009 allows (verify V6).

**Considered.** Computing what rests to buy from the book at each place: a walk of up to 10,000
resting orders with a map lookup each, against SC-003. Tracking it in `Order` alone: the check
is per owner and outcome, which is the position's key.

## R7. Finality: resolved pays the unit, void the equal part, the remainder is kept; a different finality on a final exchange is refused

**Decision.** `finalise` is refused `state` unless closed. Resolved to an outcome: one
`PositionPaid` per position (R6), then `Final(Resolved(outcome))`. Void: per position
`quantity × ⌊unit / n⌋` with n the outcomes; the undivided part per set, `unit − n × ⌊unit / n⌋`,
times the sets outstanding, is one `RemainderKept` when it is more than zero; then `Final(Void)`.
With no sets outstanding: `Final` alone. `replay` of `Final` sets the state, zeroes the sets
outstanding and records the finality. On a final exchange, `finalise` with the same finality
answers no event and no change (the repeat); with a different one, `Refusals.state(final)`.
*Decided by Trevor, 2026-10-31.* A close on a closed exchange is likewise no event; on a final
one, `state`.

**Rests on.** FR-016, FR-017; the nine scenarios of `close-and-settlement.feature` ("a void whose
equal part does not divide keeps the remainder in the pool and records it": 10 sets of three
outcomes at unit 100 pay 330 each and keep 10).

## R8. Suspension and the close

**Decision.** `suspend` is refused `state` unless open; `resume` unless suspended. Suspended, a
`place`, `split` or `merge` is refused `state` naming `suspended`; a `cancel` and a `close` are
applied as when open. `close` expires every resting order of every book, buys and sells, in the
books' order (outcomes sorted, buys then sells, best first), each with its release (R4), then
`Closed`; a close with nothing resting is `Closed` alone. Neither suspend nor resume moves money.

**Rests on.** FR-015a, FR-016; the three suspension scenarios of `orders.feature` and the two of
`close-and-settlement.feature`.

## R9. The reads are the domain's; the components wrap them with the sequence

**Decision.** `Exchange.lastPrice(outcome): Option[Long]`, `bestBid(outcome)`, `bestAsk(outcome)`,
and `depth(outcome): (Vector[Level], Vector[Level])` (buys best first, sells best first, quantity
per price from each resting order's remaining) are functions of the value, with `Level` in the
domain. `components.Depth.of` becomes `Depth(outcome, buys, sells, sequence)` from them; its JSON
is unchanged (verify V5). A trade through the pool writes both outcomes' prices in
`TradeMade.prices`, the taker's outcome at the taker's price and the other at the resting
order's; a direct trade writes one.

**Rests on.** FR-011, FR-020; the five scenarios of `prices.feature`.

## R10. The features are bound against a world of values; seeds are splits; the quantity is parsed by the domain

**Decision.** `ExchangeFeatures extends GherkinSuite("features/exchange")` binds every scenario of
the six files; `ExchangeDomainFeatures` binds the three scenarios spec 004 left to this spec and
keeps the market's in `ranElsewhere`. Both use `ExchangeWorld`, the step definitions' state:
exchanges by market id, the commands applied to each in order, the last answer, and the answers
by order id and set key. Its rules:

- "an open exchange … with the outcomes … and a unit of …" opens with the defaults of spec 003
  (maximum order cost 1,000 units, maximum position 10,000); "a closed" or "a suspended exchange"
  opens and then closes or suspends. "the maximum order cost (position) of m-1 is N" reopens the
  exchange with that limit and reapplies the commands applied so far, since limits are fixed at
  open (the clarification).
- "X holds N shares of Y" is one definition, since the runner matches a step by its text and
  not by its keyword (V8): while no When has run in the scenario it seeds, by a split of N sets
  keyed `seed-X-n` applied with the maximum position lifted for that one command (a given holding
  is a premise, not a thing the scenario's limits produced; the pool invariant stays exact, and a
  reopen reapplies it lifted); once a When has run it asserts the position's quantity. "at a cost of C" is checked
  against the split's cost (⌊unit / outcomes⌋ per share; 100 shares at unit 100 and two outcomes
  cost 5,000, as `positions.feature` says).
- "X sold N shares of Y to Z at price p" places Z's buy then X's sell, keyed `seed-…`.
- "X is released / charged / paid N" reads the last answer's events (legs, cancels, expiries,
  splits, merges, payouts) for X and sums; "is paid nothing" is a sum of zero.
- "the answer is the one the first order O received" compares the last answer to the one
  recorded under O: both accepted, and the repeat's events empty (R5).
- Every step's time is the scenario's clock, one second later per command, so out-of-order never
  trips a scenario that is not about it.
- The quantity in "places a … order … for {string} shares" is read as text and given to
  `Quantity.of(BigDecimal)`, which answers a positive whole number as a `Long` or
  `Refusals.quantity(text)`; the binding applies the place only with a `Long`. The consumer's
  endpoint decodes the field the same way, so "not a whole number" is one rule in one place.
  *Decided by Trevor, 2026-10-31, over dropping the `2.5` row: the scenario stands as written.*

**Rests on.** `GherkinSuite` 0.14.2 (one test per scenario and per outline row; a `{string}`
value is a `String`; a `{int}` read through `BigDecimal.longValue`, which would turn 2.5 into
2); the Background steps of the six files; spec 004 V4a (no bound scenario without a rule
behind it).

**Considered.** Seeding holdings by writing the value's positions directly: cheaper, but it
breaks FR-015 for the seeded value and proves the step definitions can do what the domain
cannot. A `{long}` parameter for the quantity: silently truncates.

## R11. The property suite: 100,000 sequences by default, four invariants after every command

**Decision.** `ExchangeProperties extends munit.ScalaCheckSuite` with
`minSuccessfulTests = sys.props.get("predictions.sequences").fold(100_000)(_.toInt)`. A
generator makes a world (two or three outcomes, unit 100, limits large or tight) and a sequence
of up to 40 commands drawn from: places (three owners, both sides, prices 1 to 99, quantities 1
to 20, the three durations, ids fresh or a known one repeated or reused with a change), cancels
of known orders by their owner or another, splits and merges (fresh or repeated keys), suspend,
resume, and at the tail a close and a finalise (resolved or void). The property folds the sequence
and asserts after every command: (1) FR-015, every outcome's total held equals the sets
outstanding and the pool equals sets × unit + remainder; (2) no position's quantity is negative and
`restingUnderSells ≤ quantity`; (3) for every order, `cost − Σ released − Σ charged = remaining ×
price` while it rests, and 0 once filled, cancelled or expired, so the resting buys' costs equal
what the events say is reserved; (4) the events replayed onto `Exchange.empty` equal the value, and
a refusal leaves the value identical. Every money event's amount is non-negative, a placed buy's cost
among them (the owners and keys are the generator's, never empty, so they are not asserted), and
quantities near `Long.MaxValue` are drawn so an overflowing limit check is seen. A failure prints
the sequence; it is not shrunk.

**Rests on.** SC-002; munit-scalacheck 1.2.0's `scalaCheckTestParameters` (verify V2). At a few
microseconds per command the default run is well under a minute (verify V3).

## R12. The perf measure is the domain's, in its own suite

**Decision.** `ExchangePerfSuite` in `exchange-domain`'s tests: open, rest 10,000 buys across both
books at prices that never meet a sell (two hundred owners, so the maximum position never
refuses), warm up with 1,000 more places, then time 200 places that rest and 200 that match a
resting sell, and assert the median of each under 1 ms. No runtime, no Docker. The components'
`ExchangePerfSuite` keeps spec 004 SC-002's 10 ms through the entity.

**Rests on.** SC-003; R2's budget. Verify V4 for the CI runner.

## R13. The `0.0.0` fixtures are regenerated once, from the real domain

**Decision.** `modules/exchange-components/src/test/resources/journal/0.0.0/` is deleted and
`JournalFixtureWriter 0.0.0` run again: `every-event.jsonl` (every case through the serializer,
replayed to its recorded value, which now carries `restingToBuy`) and `opened.jsonl` as before,
plus `traded.jsonl`: an exchange opened, a split, a resting sell, a buy that trades at a better
price, a two-outcome trade through the pool, a cancel, a close and a finality, so the first
release's fixture replays a journal with money on it. The markets' fixtures are untouched.

**Rests on.** Spec 004 R12 and its plan ("when the domains land, the fixtures are regenerated
once before the first tag"); no tag exists, so `0.0.0` is nobody's contract yet. After the first
tag a fixture is never rewritten.

## R14. The components change only where they stood on the stub

**Decision.** Within SC-004 (no rule in the components):

- `Reads.Depth.of` calls `exchange.depth(outcome)` (R9); `Level` is imported from the domain.
- `Settlement` calls `ledger.payout` only for a `PositionPaid` with `paid > 0` (R6), as it
  already skips a leg that releases nothing.
- `PlacementSuite`: the buys that must reach the exchange and be refused (so the reservation is
  released under ordinal 0) carry a price of the unit, refused `price`; the patience test's buy
  is accepted and the assertion becomes "o-2 rests"; the crash tests keep their keys.
- `ComponentsFeatures` lifts the trade scenario: "bob holds 10 shares" is a split through the
  entity keyed `seed-bob`, the resting sell a place, and the ledger is asked to settle `t-1`.
- The components' `ExchangePerfSuite` drops its `assume`.
- `JournalFixtureWriter` writes `traded.jsonl` (R13).

**Rests on.** The grep of `notBuilt`, `not built` and `Depth.of` across the modules on
2026-10-31; spec 004's research ("the crash tests").

## R15. What the plan asked, and the answers

- The `2.5` example row stays; the domain parses the quantity (R10).
- A finalise with a different finality on a final exchange is refused naming the state (R7).
- A losing position gets a `PositionPaid` with `paid` 0; the settlement consumer skips it (R6).
- A cancel of an order the exchange never accepted is refused naming the id (analyze, U1).

All four by Trevor, 2026-10-31, through the design run.

## Verify at implementation

- **V1.** `GherkinSuite` gives a `{string}` parameter to a `String` definition untouched (so
  `"2.5"` arrives as text) and runs each outline row as its own test.
- **V2.** munit-scalacheck 1.2.0 takes `override def scalaCheckTestParameters =
  super.scalaCheckTestParameters.withMinSuccessfulTests(n)`.
- **V3.** The default 100,000-sequence run finishes in under a minute on a laptop; if not, the
  generator's sequence length is cut before the count is.
- **V4.** The domain perf median holds on the GitHub runner; if it flakes there, the assertion
  stays and the suite is tagged to run on `just perf` and not in `sbt test`, with the reason in
  the suite's doc comment.
- **V5.** `Depth`'s JSON is byte-for-byte what it was once `Level` comes from the domain.
- **V6.** A `0.0.0` snapshot written before `Position.restingToBuy` existed decodes with the
  field at 0 under ankka's codec configuration (jsoniter-scala uses the case class default for
  a missing field); checked by replaying the old `every-event.jsonl` before it is regenerated.
- **V7.** `-Werror` and scalafmt pass on the matcher; `Vector.search` is available on
  `scala.collection.immutable.Vector` in 3.9.0.
- **V8.** `GherkinSuite.run` matches a step against every definition's expression and fails on
  two matches, whatever keyword either was declared with (read on 2026-10-31); so no expression
  is bound twice, and "holds" seeds or asserts by whether a When has run.

## Found during implementation

- **2026-10-31, the baseline (T001).** `sbt exchangeDomain/test` before any rule: 6 passed, 69
  ignored (65 of `features/exchange/`, 4 of `domain.feature`), `ExchangeStubSuite` 5 green.
- **2026-10-31, V6.** Holds: with `Position.restingToBuy` added and kept by `replay`, the `0.0.0`
  `every-event.jsonl` still replays to its recorded state (the snapshot without the field decodes
  it as 0). The decrement on a buy leg is in `PositionMoves`, beside a sell leg's
  `restingUnderSells`, so the two are kept by one rule; the positions view ignores both.
- **2026-10-31, "X is released N".** A fill-at-once buy that trades at a better price releases
  twice: the improvement under the trade's ordinal and the remainder under the next
  (`matching.feature`: 50, then 900). The step holds when one release to the owner in the last
  answer is N, as each release is its own keyed move; the scenario is unchanged.
- **2026-10-31, the place's id is checked first.** Before the state and the fields, so a repeat
  after the exchange moved on (closed, say) is still answered as the first was (R5); the
  data model's order of refusals otherwise stands.
- **2026-10-31, six values a step.** `GherkinSuite` binds at most six values, so the place steps
  are bound once per side with the side in the text. The steps every feature shares are in
  `ExchangeSteps` (an abstract `GherkinSuite`), the world in `ExchangeWorld`.
- **2026-10-31, one commit for the command bodies.** T005, T009 to T012, T014, T016, T017 and T019
  all write `Exchange.scala` or the matcher; they were written together and committed as one,
  with the bindings (T006 to T008, T013, T015, T018, T020, T021) as the next. 65 scenarios of
  `features/exchange/` and the four exchange scenarios of `domain.feature` pass against them.
- **2026-10-31, V1 to V4.** V1: a `{string}` reaches a `String` definition as written, so `"2.5"`
  is refused by `Quantity.of`. V2: `scalaCheckTestParameters.withMinSuccessfulTests` takes; the
  suite counts and prints 100,000 sequences. V3: the default run takes about seven seconds, and
  prints what the sequences did (every event kind occurs; merges through the pool are the rarest,
  about 200). V4: the domain's medians on a laptop are 0.03 ms for a resting place and 0.17 ms for
  a matching one, on 11,000 resting orders.
- **2026-10-31, T026 was already done.** `Settlement` matched `PositionPaid` only when `paid > 0`
  from spec 004, so a losing position's paid 0 never reaches the ledger; nothing changed there.
- **2026-10-31, V5.** `Depth`'s JSON is unchanged with its levels from the domain's `Level`
  (`ReadsSuite` pins it).
- **2026-10-31, T027.** With the real domain a buy at the unit's price is refused `price` after
  the reservation is made, so `o-1`, `o-5` and `o-6` keep their release-under-ordinal-0
  assertions; `o-2`, at 60, rests.
- **2026-10-31, R13.** `traded.jsonl` (21 events): a split, a sell, a direct buy at a better
  price, a pool trade through a split, a cancel, a close and a resolved finality whose positions
  include two paid 0.
- **2026-10-31, V7.** `-Werror` and scalafmt pass. `Book.insert` is a binary search of its
  own over the book's ordering, not `Vector.search`, which needs an `Ordering` of the orders
  and not the side's price-then-time rule.
- **2026-10-31, the quickstart's second check.** Without `restingToBuy` on `OrderPlaced` only the
  property suite goes red; the maximum position scenario holds shares and rests no buy, so it
  cannot see it. The quickstart says so. The first check (the pool winning ties and worse
  prices) turns two `complete-sets.feature` scenarios red, as it said.

## Requests

None new. Spec 004's two to ankka stand (a `CommandError`'s details across the component client;
a scheduler on an entity's context), and its one to ankka-modules (`notes/casino-requests.md`
item 9: a split charged from a reservation, which the pool trade of R3 needs on the ledger side).

## Design finished

2026-10-31: analyze reported nothing above LOW.

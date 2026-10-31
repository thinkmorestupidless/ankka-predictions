---
description: "Task list for spec 005: the exchange domain"
---

# Tasks: The Exchange Domain

**Input**: `specs/005-exchange-domain/` (plan, research R1 to R15, data-model, contracts/domain-api.md, quickstart)

**Tests**: requested. The spec names its scenarios in `features/exchange/` and `features/libraries/domain.feature`; constitution II says a rule is in its feature before it is in code and IX holds one scenario to one test. Each binding task is written and seen failing before the rule it holds; a scenario that waits on a later phase is named at the checkpoint, not skipped.

**Scope (plan, "What this feature builds")**: every command body of `Exchange.apply`, the matcher, the reads, the step definitions, the property and perf suites, the components changed only where they stood on the stub, the `0.0.0` fixtures regenerated once. Nothing in the protocol moves (research R1).

Paths: `EX` = `modules/exchange-domain/src/main/scala/com/thinkmorestupidless/predictions/exchange/`, `EXT` = `modules/exchange-domain/src/test/scala/com/thinkmorestupidless/predictions/exchange/`, `EXC` = `modules/exchange-components/src/main/scala/com/thinkmorestupidless/predictions/exchange/components/`, `EXCT` = `modules/exchange-components/src/test/scala/com/thinkmorestupidless/predictions/exchange/components/`.

## Phase 1: Setup

- [X] T001 Run `sbt exchangeDomain/test` on the branch and record the baseline: `ExchangeStubSuite` green, every scenario of `features/exchange/` reported ignored naming spec 005, three of `domain.feature` likewise. Then replay `modules/exchange-components/src/test/resources/journal/0.0.0/every-event.jsonl` once more after T004 (V6) before it is regenerated in T030

## Phase 2: Foundational (blocks every story)

**Purpose**: what every Background step and every command body stands on: the quantity parser, the position field, the book's insert, the split that seeds holdings, and the step definitions' world.

- [X] T002 [P] Write `EX/Quantity.scala`: `Quantity.of(text: String)` and `of(n: BigDecimal)` answering a positive whole number as a `Long` or `Refusals.quantity(text)` (contracts/domain-api.md); change `Refusals.quantity` in `EX/Refusal.scala` to take the text it names. Hold it in `EXT/QuantitySuite.scala`: `"10"`, `"0"`, `"-1"`, `"2.5"`, `"ten"`
- [X] T003 [P] In `EX/Model.scala`: add `restingToBuy: Long = 0` as the last field of `Position`; add `final case class Level(price: Long, quantity: Long)`; make `Book.rest` insert at the index `Vector.search` finds for `(−price, sequence)` on buys and `(price, sequence)` on sells instead of appending and sorting (R2, V7)
- [X] T004 In `EX/Exchange.scala` `replay`: keep `restingToBuy` as `restingUnderSells` is kept, up on a buy's `OrderPlaced`, down by the quantity on each buy leg of `TradeMade`, down by the remaining on a buy's `OrderCancelled` or `OrderExpired` (R6, data-model "What replay does"). Replay the `0.0.0` `every-event.jsonl` and confirm it still equals its recorded state (V6)
- [X] T005 In `EX/Exchange.scala`: the `split` body (R5, R6; data-model "The commands"): refused `state` unless open, `quantity` when sets < 1, `limit` naming `maximum position` when any outcome's `quantity + restingToBuy + sets` passes it, `key` for a known key with other fields; a known key with the same fields answers no event; else `SetsSplit(key, owner, sets, sets × unit, at)`
- [X] T006 Write `EXT/ExchangeWorld.scala` (R10): exchanges by `MarketId`, each with its open command and the commands applied since; a clock from `2026-10-31T10:00:00Z` advancing one second per command; `apply(market, command)` recording the last answer and, by order id and set key, the first answer each received; `events` of the last answer; `reopen(market, maxOrderCost, maxPosition)` that opens afresh with the limits and reapplies the commands; `seedHolding(market, owner, shares)` as a split keyed `seed-<owner>-<n>` applied with `maxPosition` lifted to `Long.MaxValue` for that command alone, recorded as lifted so `reopen` reapplies it the same way; `acted(market)`, true once a When has applied a command in the scenario; `sold(market, seller, shares, outcome, buyer, price)` as the buyer's place then the seller's; sums of `released`, `charge` and `paid` for an owner over the last answer's events (legs, cancels, expiries, splits, merges, payouts)
- [X] T007 Rewrite `EXT/ExchangeFeatures.scala` as `GherkinSuite("features/exchange")` on `ExchangeWorld` with no `ranElsewhere`, and delete `EXT/Pending.scala`. Bind the steps every file shares: `an open exchange {string} with the outcomes {string} and {string} and a unit of {string}` (spec 003's limits: maximum order cost 1,000 units, maximum position 10,000), the three-outcome variant, `a closed exchange …`, `a suspended exchange …`, `{string} holds {string} shares of {string} in {string}` as one definition, since the runner matches by text and not by keyword (V8): it seeds while `acted` is false and asserts the position's quantity once it is true; `… at a cost of {string}` likewise, `the maximum order cost of {string} is {string}`, `the maximum position of {string} is {string}`, `the order {string} rests in the book of {string} in {string} at price {string} for {string} shares`, `nothing rests in the book of {string} in {string}`, `the order is refused naming the price|quantity|outcome`, `… naming the state {string}`, `… naming the maximum order cost|position`, `… naming the quantity held`, `… naming the id {string}`, `no event is produced`, `{string} is released|charged|paid {string}`. A place step reads the quantity as text through `Quantity.of` (R10). Run the suite: every scenario fails on a step with no definition or on `not built`, none passes

**Checkpoint**: `sbt exchangeDomain/compile` passes; the world opens, seeds and splits.

## Phase 3: User Story 1, an order is accepted, refused or cancelled (P1) 🎯 MVP

**Goal**: `place` validates and rests, `cancel` releases, a repeat is the first answer, suspend and resume.

**Independent test**: `sbt "exchangeDomain/testOnly *ExchangeFeatures"` passes every scenario of `features/exchange/orders.feature` but the two that need a trade (T013 lifts them).

- [X] T008 [US1] Bind the rest of `orders.feature` in `EXT/ExchangeFeatures.scala`: `{string} places a buy|sell order {string} for {string} shares of {string} in {string} at price {string}` with the optional ` to fill at once and cancel the rest` and ` to fill whole or not at all` (three definitions or one with a `{string}` tail), `{string} placed a … order …` (Given: applied and must be accepted), `{string} cancels the order {string}`, `the order {string} is cancelled`, `the cancel is refused naming the owner`, `the cancel is refused naming the state {string}`, `the answer is the one the first order {string} received` (R5: both accepted, the repeat's events empty), `the exchange {string} is suspended`, `the exchange {string} resumes`. See them fail
- [X] T009 [US1] Write the `place` body in `EX/Exchange.scala` in the order data-model gives: `state` (open only; `suspended`, `closed`, `final`, `empty` by their word), `outcome`, `price`, `quantity`, a buy's `limit` for the cost and for `quantity + restingToBuy + order quantity` in the outcome, a sell's `quantity held` against `quantity − restingUnderSells`, then the id: known with the same fields answers `Right(Decision(exchange, Vector.empty))`, known with other fields `Refusals.id`. An accepted order is `OrderPlaced(order, owner, outcome, side, price, quantity, duration, cost, nextOrder, at)` followed by what `Matching.place` adds. Create `EX/Matching.scala` here with `place(exchange, order): Either[Refusal, Vector[ExchangeEvent]]` returning no trade, so every order rests and a fill-whole order is refused `not fillable whole`; T012 fills it
- [X] T010 [US1] Write the `cancel` body: an order the exchange never accepted → `Refusals.id(order)`; one not resting or filled in part → `state` naming the order's state word; caller not the owner → `owner`; else `OrderCancelled(order, owner, released, releaseOrdinal, at)` with a buy's `remaining × price` under `releases + 1` and a sell's 0 under 0 (R4). Allowed while suspended (R8)
- [X] T011 [US1] Write `suspend` and `resume` (R8): `state` unless open / unless suspended; `Suspended(at)`, `Resumed(at)`. Confirm `place`, `split` and (later) `merge` refuse `state` naming `suspended`

**Checkpoint**: `orders.feature` green but "a cancel of an order that is filled is refused naming the state" and "a resumed exchange trades again with the orders that rested", which need T012.

## Phase 4: User Story 2, orders match by price and then by time (P1)

**Goal**: the matcher, direct trades, partial fills, the three durations, self-match skipped, sequence numbers, determinism.

**Independent test**: `features/exchange/matching.feature` passes, and the two US1 scenarios with it.

- [X] T012 [US2] Write `EX/Matching.scala` (R3): `Matching.place(exchange, order): Either[Refusal, Vector[ExchangeEvent]]` returning the events after `OrderPlaced`. The loop: the candidate is the head of the opposite side of the order's book skipping the owner's own orders, within the taker's price; trade the smaller remainder at the resting order's price; `TradeMade(TradeId.of(n), n, Direct, quantity, SortedMap(outcome → price), legs, at)` with the buy leg's `charge = quantity × price`, `released = quantity × (buy price − price)` under the buy order's next ordinal (0 and nothing when equal), the sell leg's `paid = quantity × price`; `OrderFilled` for the resting order when its remaining reaches 0, then for the taker; repeat until filled or no candidate. Durations: rest-until-close leaves the remainder resting; fill-at-once ends with `OrderCancelled` for the remainder (a buy's `remaining × price` released under the next ordinal); fill-whole runs the loop without applying and returns `Refusals.notFillableWhole` when the fillable quantity is short. The loop advances through a working copy of the value by `Exchange.replay` so each candidate is read from the value after the last trade. Leave a seam for the other outcome's candidate (T016)
- [X] T013 [US2] Bind `matching.feature`: `one trade is made for {string} shares at price {string} between the orders {string} and {string}` (one `TradeMade` in the last answer with those orders, in either leg order), `no trade is made`, `the order {string} is filled`, `the order {string} is cancelled for {string} shares`, `the trade charges {string} {string} and pays {string} {string}`, `the order is refused as not fillable whole`, `the orders {string}, {string}, {string} and {string} were placed in that order and made the trades {string} and {string}` (bob and cy sell 10 at 55 and 58, ada buys 15 at 60 and 5 at 58, recorded), `the same orders are placed again in the same order on a fresh exchange`, `the trades made are the same as {string} and {string} in price, quantity and orders`, `the trade between {string} and {string} carries the sequence number {string}`. See them fail, then pass with T012

**Checkpoint**: `orders.feature` and `matching.feature` green; the engine is done.

## Phase 5: User Story 3, positions, complete sets and the pool (P1)

**Goal**: positions move with cost, merges, the pool invariant, the two-outcome match through the pool.

**Independent test**: `features/exchange/positions.feature` and `complete-sets.feature` pass.

- [X] T014 [US3] Write the `merge` body in `EX/Exchange.scala`: `state` unless open, `quantity` when sets < 1, `quantity held` naming the smallest `quantity − restingUnderSells` over the outcomes when it is under the sets, `key` for a known key with other fields, the repeat as the first; else `SetsMerged(key, owner, sets, sets × unit, at)` (R5, R6)
- [X] T015 [US3] Bind `positions.feature` and `complete-sets.feature`: `{string} sold {string} shares of {string} in {string} to {string} at price {string}` (the world's `sold`), `{string} cancelled the order {string}`, `the position of {string} in {string} of {string} is read`, `the position of {string} in {string} of {string} shows {string} shares at a cost of {string}`, `{string} splits|merges {string} sets in {string} under the key {string}`, `{string} split {string} sets in {string}` (keyed `seed-split-<n>`) and `… under the key {string}`, `the split|merge is refused naming the maximum position|quantity held`, `the answer is the one the first split {string} received`, `the pool of {string} is read`, `the pool of {string} holds {string}`, `{string} sets are outstanding in {string}`, `the positions of {string} are read`, `{string} shares of {string} are held in {string} in total`, `one trade is made for {string} shares between the orders {string} and {string} by splitting|merging a set` (`through` Split or Merge), `the trade charges {string} {string} and charges {string} {string}`, `the trade pays {string} {string} and pays {string} {string}`. See the pool scenarios fail
- [X] T016 [US3] Add the other outcome's candidate to `EX/Matching.scala` (R3's table) when the market has exactly two outcomes: for a buy at p, the head buy of the other book at q (skipping the owner's own) with p + q ≥ unit, the taker's price `unit − q`; for a sell at p, the head sell of the other book at q with p + q ≤ unit, the taker's price `unit − q`. Choose the better price for the taker, the own book when equal. A split trade: two buy legs, the resting charged `q × quantity`, the taker `(unit − q) × quantity` with `(p − (unit − q)) × quantity` released under its next ordinal; `through = Split`; `prices` both outcomes; sets outstanding up by the quantity on replay. A merge trade: two sell legs, paid likewise; `through = Merge`; sets down. The fill-whole dry run counts both candidates. A market of three or more outcomes never offers the candidate (FR-012)

**Checkpoint**: `positions.feature` and `complete-sets.feature` green; the pool invariant holds in every scenario (assert it in the world after every command, as a guard).

## Phase 6: User Story 4, a close expires every order and a final exchange pays every position once (P1)

**Goal**: `close` and `finalise`.

**Independent test**: `features/exchange/close-and-settlement.feature` passes.

- [X] T017 [US4] Write `close` and `finalise` in `EX/Exchange.scala` (R7, R8): `close` refused `state` on empty or final, no event when closed, else `OrderExpired` per resting order in the books' order (outcomes sorted, buys then sells, best first) with each buy's `remaining × price` under its next ordinal and each sell's 0 under 0, then `Closed`; `finalise` refused `state` unless closed, or final with another finality; no event when final with the same; else one `PositionPaid(PositionKey.of(owner, outcome), owner, outcome, quantity, paid, at)` per position with `quantity > 0` over owners and outcomes in sorted order, `paid` = `quantity × unit` for the resolved outcome, 0 for the others, `quantity × ⌊unit / outcomes⌋` for a void; a void's `RemainderKept(setsOutstanding × (unit − outcomes × ⌊unit / outcomes⌋), at)` when above 0; then `Final(finality, at)`
- [X] T018 [US4] Bind `close-and-settlement.feature`: `the exchange {string} closes` (When) and `the exchange {string} closed` (Given), `the exchange {string} is made final resolved to {string}`, `… again`, `the exchange {string} is made final void`, `the order {string} is expired`, `{string} is paid {string} for the position in {string} of {string}`, `{string} is paid nothing` (no `PositionPaid` for the owner with `paid > 0` in the last answer), `the pool of {string} holds {string} as its remainder`, `the command is refused naming the state {string}`. See them fail, then pass

**Checkpoint**: four feature files green; money is decided.

## Phase 7: User Story 5, prices are read from the value (P2)

**Goal**: the reads of FR-020 as functions of the value.

**Independent test**: `features/exchange/prices.feature` passes.

- [X] T019 [P] [US5] Add to `EX/Exchange.scala`: `lastPrice(outcome)`, `bestBid(outcome)`, `bestAsk(outcome)`, `depth(outcome): (Vector[Level], Vector[Level])` from each resting order's remaining, best first (R9)
- [X] T020 [US5] Bind `prices.feature`: `the last price of {string} in {string} is {string}`, `the prices of {string} in {string} are read`, `the best bid of {string} in {string} is {string} and the best ask is {string}`, `the depth of {string} in {string} is read`, `the depth shows {string} shares to buy at {string} and {string} shares to sell at {string}`, `{string} in {string} has no last price`

**Checkpoint**: all six files green: 65 scenarios and rows.

## Phase 8: User Story 6, the value, its events and replay (P1)

**Goal**: the three scenarios spec 004 left here, the property suite, the perf measure, the stub gone.

**Independent test**: `sbt exchangeDomain/test` passes with `ExchangeProperties` at 100,000 sequences and `ExchangePerfSuite` under 1 ms.

- [X] T021 [US6] In `EXT/ExchangeDomainFeatures.scala` lift the three scenarios from `ranElsewhere` (keep the market's) and bind them on `ExchangeWorld`: `an exchange {string} that has accepted {string} orders and made {string} trades` (the counts arrive as the words `twenty` and `six`, mapped by the binding; twenty orders over two owners and both outcomes making six trades, counted), `the exchange's events are replayed onto an empty exchange in sequence`, `the rebuilt exchange equals the exchange that produced them`, `the rebuilt book of {string} in {string} equals the book that produced them`, `an exchange {string} with {string} resting orders in the book of {string}` (`three`, a word), `a buy order at price {string} is applied to the exchange`, `the command is refused naming the price`, `the trade event names {string} charged {string} keyed by the order {string} and the trade`, `the trade event names {string} paid {string} keyed by the trade`, `the trade event names {string} released {string} keyed by the order {string}` (the leg's `releaseOrdinal` above 0)
- [X] T022 [US6] Write `EXT/ExchangeProperties.scala` as a `munit.ScalaCheckSuite` (R11, V2): a world generator (two or three outcomes, unit 100, limits wide or tight) and a command-sequence generator of up to 40 commands (places by three owners, both sides, prices 1 to 99, quantities 1 to 20, the three durations, ids fresh, repeated or reused with a change; cancels by the owner or another; splits and merges with fresh or repeated keys; suspend and resume; a close and a finalise at the tail); the property folds the sequence through `Exchange.apply` and after every command asserts data-model's invariants 1 to 6; `minSuccessfulTests` from `-Dpredictions.sequences`, default 100,000; a failure prints the shrunk sequence. Time the default run (V3)
- [X] T023 [US6] Write `EXT/ExchangePerfSuite.scala` (R12, V4): open with wide limits, rest 10,000 buys across both books from two hundred owners at prices that meet no sell, warm up with 1,000 more, time 200 places that rest and 200 that match a resting sell placed for them, assert each median under 1 ms, print both
- [X] T024 [US6] Replace `EXT/ExchangeStubSuite.scala` with `EXT/ExchangeSerializerSuite.scala` keeping the serializer round trip of every event case and the value, the re-open refusal and `cost`; remove `Refusals.notBuilt` from `EX/Refusal.scala` and the `case other => notBuilt` arm of `Exchange.apply` (every command has a body now); `-Werror` clean

**Checkpoint**: `sbt exchangeDomain/test` green, no scenario ignored but the market's.

## Phase 9: The components on the real domain (R14)

**Purpose**: what stood on the stub stands on the rules, with no rule added (SC-004).

- [X] T025 [P] In `EXC/Reads.scala` make `Depth.of(exchange, outcome, sequence)` call `exchange.depth(outcome)` and import `Level` from the domain; check the JSON of a `Depth` is unchanged (V5) in `EXCT/ComponentsFeatures.scala` or a one-test suite
- [X] T026 [P] In `EXC/Settlement.scala` call `ledger.payout` only for a `PositionPaid` with `paid > 0` (R6)
- [X] T027 In `EXCT/PlacementSuite.scala` give the buys the exchange must refuse (`o-1`, `o-5`, `o-6`) a price equal to the unit so the domain refuses `price` and the reservation is released under ordinal 0; let `o-2` be accepted and assert it rests; keep every key assertion. Update the class comment
- [X] T028 In `EXCT/ComponentsFeatures.scala` lift the trade scenario from `ranElsewhere`: `{string} holds {string} shares of {string} in {string}` as a `split` through the entity keyed `seed-<owner>`, `a resting sell order by {string} for {string} shares of {string} in {string} at price {string}` as a `place`, `the ledger port is asked to settle the trade {string} keyed by {string}`, `asking the ledger port to settle the trade {string} keyed by {string} again is answered as the first was` against `ScriptedLedger`'s call log
- [X] T029 [P] In `EXCT/ExchangePerfSuite.scala` drop the `assume` and the `Refused` catch; the test runs
- [X] T030 In `EXCT/JournalFixtureWriter.scala` add `traded.jsonl` (R13: open, a split, a resting sell, a buy that trades at a better price, a two-outcome trade through the pool, a cancel, a close, a finality resolved) and refresh the comment; delete `modules/exchange-components/src/test/resources/journal/0.0.0/` and run the writer for `0.0.0`; update `JournalCompatibilitySuite`'s comment on `every-event.jsonl`. `sbt "exchangeComponents/testOnly *JournalCompatibilitySuite"` green
- [X] T031 `sbt exchangeComponents/test` green (Docker)

## Phase 10: Polish

- [X] T032 [P] Update `README.md` (the exchange's rules are in; the markets' wait on spec 006), `CLAUDE.md`'s spec table (005 building; 004 built) and `docs/limitations.md` (what the exchange does and does not: no fees, no short selling, no fractional shares, no market maker; markets not built)
- [X] T033 [P] Append to `specs/005-exchange-domain/research.md` "Found during implementation" what V1 to V7 showed and anything the plan said otherwise
- [X] T034 `just fmt`, `sbt test`, `just check` (0 findings); run the quickstart's two checks that can fail and put the code back

## Dependencies

- Phase 2 before everything: T002 and T003 in parallel, then T004, T005, T006, T007 in order.
- US1 (T008 to T011) before US2 (T012, T013); US2 before US3's T016; T014 and T015 need only Phase 2 and T009.
- US4 (T017, T018) needs T009 and T005; US5 (T019, T020) needs T012 for the last price.
- US6: T021 needs T012; T022 needs every command body (T017); T023 needs T012 and T016; T024 last in the domain.
- Phase 9 needs Phase 8 complete (the stub gone); T025, T026, T029 in parallel; T027 and T028 after T026; T030 after T028; T031 last.
- Phase 10 after Phase 9.

## Parallel example

After T007: T008 (bindings) and T009 (the place body) are one person's; T019 (the reads) can go beside them. After T013: T014, T015 and T019 in parallel. In Phase 9: T025, T026 and T029 in parallel.

## Implementation strategy

MVP is Phase 2 plus US1 and US2: an exchange that accepts, rests, cancels and matches, with the
features as the proof. US3 and US4 bring the pool and the money's end. US5 is small. US6 is where
SC-002 and SC-003 are held and the stub is removed; nothing in Phase 9 starts before it, so the
components meet a finished domain once. Phase 10 is the pages.

# Review: spec 005, the exchange domain (origin/main...HEAD, 18 commits)

**Verdict:** Request changes. One blocking finding: the domain accepts a buy, and a split, whose
money figure has overflowed to a negative number. The rest is should-fix and below.
**What it does:** It fills `Exchange.apply` for every command after `Open`: placing with the
refusals and limits, the matcher (the own book and, with two outcomes, the pool, at the better
price), cancel, split, merge, suspend, resume, close and finality. It adds the FR-020 reads to the
value and `Position.restingToBuy` with a default. It binds every `features/exchange/` scenario
against the value, adds a 100,000-sequence property suite and a domain perf measure, and moves the
components off the stub.
**Should it exist:** Yes. Spec 004 built the shape and left `apply` stubbed. This change is the
book itself, and it sits where spec 004 and the plan say it should: a pure value in
`exchange-domain`, with no rule added to the components (SC-004 holds; `Reads.Depth.of` now
delegates and nothing else in `src/main` of the components changed).
**Read:** In full: `Exchange.scala`, `Matching.scala`, `Model.scala`, `PositionMoves.scala`,
`Quantity.scala`, the `Refusal.scala` diff, `ExchangeSteps.scala`, `ExchangeWorld.scala`,
`ExchangeFeatures.scala`, `ExchangeDomainFeatures.scala`, `ExchangeProperties.scala`, the domain's
`ExchangePerfSuite.scala`, `QuantitySuite.scala`, `ExchangeSerializerSuite.scala`, every
components test diff, `ReadsSuite.scala`, `traded.jsonl`, `build.sbt`, and the README, CLAUDE.md,
GLOSSARY and limitations diffs. Also in full: spec.md, plan.md, tasks.md, research.md,
data-model.md, contracts/domain-api.md and quickstart.md. Opened outside the diff:
`features/exchange/matching.feature` and `features/libraries/domain.feature` (for the bound
scenarios), and the 0.0.0 fixture directory. Not reviewed: `Settlement.scala` (unchanged; R14 says
it already skipped `paid` 0). `just check` and the Docker-backed components suites were not run.
**Size:** 37 files, +2579 −194. About 420 lines are production Scala, about 1100 are tests and
about 980 are spec documents. The summary script says to split, but that number is mostly the
design record of one spec, and the production change is one coherent unit. No split asked.

## Blocking

- `modules/exchange-domain/src/main/scala/com/thinkmorestupidless/predictions/exchange/Exchange.scala:134-143`
  issue (blocking): the limits are checked with arithmetic that overflows. An owner who holds one
  share can place a buy for `Long.MaxValue` shares at price 2 and have it accepted with
  `cost = -2`. `Exchange.cost` wraps to −2, which passes `cost > maxOrderCost`.
  `held.quantity + held.restingToBuy + c.quantity` wraps negative, which passes the
  maximum-position check. `split` (lines 198-206) has the same flaw:
  `p.quantity + p.restingToBuy + c.sets` wraps, and the event says `charge = -100`. Both were
  confirmed by running them: `OrderPlaced(..., quantity = 9223372036854775807, cost = -2, ...)` and
  `SetsSplit(..., sets = 9223372036854775807, charge = -100, ...)`. This breaks the money rules the
  spec is built on. FR-004's limits are the domain's to keep. Constitution I and FR-018 say the
  event's amount is what the ledger moves, so a negative reservation or charge is a negative money
  move. SC-002's invariants all pass while this happens, because the generator never draws a
  quantity above 20. `Quantity.of` (`Quantity.scala:18`) passes anything up to `Long.MaxValue` to
  this code, so the consumer's endpoint gets no protection from it either.
  Suggestion: compare in a form that cannot overflow, and check the limits before computing any
  product. For example:
  ```scala
  else if c.side == Side.Buy && c.quantity > x.maxOrderCost / c.price then limit(maximumOrderCost)
  else if c.side == Side.Buy && c.quantity > x.maxPosition - held.quantity - held.restingToBuy then
    limit(maximumPosition)
  ```
  and `c.sets > x.maxPosition - p.quantity - p.restingToBuy` in `split`. These are equivalent to
  the current checks for every non-overflowing input, and both subtractions are bounded because a
  position never exceeds `maxPosition`. If any money product remains, use
  `Math.multiplyExact`/`addExact`. Then add a scenario or property row with a quantity near
  `Long.MaxValue`, plus a `Gen` branch in `ExchangeProperties` that draws large quantities and set
  counts, so the suite can see this kind of defect.
  Resolved: the limits divide and subtract; `LimitsSuite` holds the three cases (red without the fix), and the property suite draws quantities near `Long.MaxValue` and checks a placed buy's cost, fadec9d

## Should fix

- `modules/exchange-domain/src/test/scala/com/thinkmorestupidless/predictions/exchange/ExchangePerfSuite.scala:51,61-66`
  issue (non-blocking): the "matching place" median does not time a direct match with the resting
  sell placed for it. The resting buys of `no` at up to 39 combine with a buy of `yes` at 95 to
  more than one unit, and the pool price (100 − 39 = 61) beats the sell at 90. Every timed buy is
  therefore a split trade, and the 200 sells at 90 pile up unmatched. This was confirmed by a probe
  of the same shape, which returned `(Split, Vector(b, n))`. The comments ("no sell is below 40",
  "one that trades with a resting sell") describe a run that does not happen. SC-003 still holds,
  but the direct path, the common one, is not measured. Suggestion: rest the 10,000 buys on
  `yes` only, or keep the `no` buys below `unit − 95`, so the timed buy meets the sell directly.
  Time the pool path as a third median if it is worth keeping.
  Resolved: the no buys rest at 1 to 4 so the timed buy trades directly; a third median times the pool, and each timed place asserts how it traded (0.03, 0.21, 0.17 ms), 65fbce1
- `docs/limitations.md` issue (non-blocking): T032 says the page lists "no fees, no short
  selling, no fractional shares, no market maker", and `quickstart.md:57` says the page does. The
  page has only "no fees" from that list. Short selling, fractional shares and the market maker
  are missing, though the spec's Assumptions and `Quantity` make the first two true limits.
  Suggestion: add the three lines.
  Declined: the three are on the page already, under "Deliberately not" (market maker, short selling, fractional shares); a second copy under the exchange's section would say the same twice
- `modules/exchange-domain/src/test/scala/com/thinkmorestupidless/predictions/exchange/ExchangeProperties.scala:143-151`
  suggestion (if-minor): R11 says the suite asserts that every money event's "owner and key
  non-empty", and data-model invariant 6 includes order acceptance sequences 1, 2, 3, ….
  `amounts` checks only non-negativity, and only trade numbers are checked in turn. Add both
  checks, or narrow R11's sentence to what is asserted.
  Resolved: order acceptance sequences are checked in turn; R11 now says owners and keys are not asserted (the generator never makes them empty) and that failures are not shrunk, fadec9d

## Nits

- `Exchange.scala:176` nitpick (non-blocking): `Exchange.resting` is public, but it is not in
  `contracts/domain-api.md`, which the contract page says is published with the first tag. Make
  it `private[exchange]` (the tests are in the package), or add it to the contract.
  Resolved: `resting` is `private[exchange]`, fadec9d
- `ExchangeSteps.scala:258-260, 270-272, 279, 286-291` nitpick (non-blocking): "refused naming the
  price / quantity / outcome / quantity held / owner" asserts `reason` only, not the `names` the
  refusal carries. FR-004 says "naming the field". Comparing whole refusals, as the `state` and
  `id` steps already do, would hold that.
  Resolved: the bare naming steps check the reason and that the refusal carries the named field, 3022c6e
- `PlacementSuite.scala:110` nitpick (non-blocking): `isInstanceOf[PlacementOutcome.Accepted]`.
  A `match`, or `assertEquals` on the outcome, says what was received when it fails.
  Resolved: a match that fails with what was received, 3022c6e
- `specs/005-exchange-domain/spec.md:7` nitpick (non-blocking): **Status** still says Draft, but
  CLAUDE.md now says 005 is building.
  Resolved: Building, b9370c7

## Questions

- `Exchange.scala:166-170` question: the cancel's checks run id → owner → state. T010 says
  id → state → owner, and the data model's table lists `state` before `owner`. The code's order
  matches FR-006's wording and does not tell a non-owner the state of someone else's order, which
  seems the better choice. Is that intended? If so, T010 and data-model.md should say so, as was
  done for the place's id-first check.
  Answered: intended, so a caller learns nothing of another's order; data-model.md and T010 now give id → owner → state, b9370c7
- `Exchange.scala:107-115` and `Matching.scala:64-66` question: `Open` accepts any outcomes, unit
  and limits. That is pre-existing spec 004 code, but the matcher now depends on it. Two equal
  outcomes match the `Vector(a, b)` case and make "the other book" the taker's own, so a buy would
  split against resting buys of its own outcome. A unit below 2 leaves no valid price, and a
  negative limit refuses everything. Should `open` refuse duplicate outcomes, fewer than two
  outcomes, more than twenty (the spec's stated maximum), a unit below 2 and non-positive limits?
  Or is that the markets domain's guarantee (spec 006)? If it is the latter, a sentence in the
  contract saying so would resolve this.
  Answered: spec 006's guarantee (Trevor's call, 2026-10-31). The contract now says `Open` checks none of its arguments, that an approved market has two to twenty distinct outcomes and the unit and limits are the consumer's, and that a caller opening an exchange otherwise owns the checks, b9370c7
- `ExchangeDomainFeatures.scala` "the exchange is unchanged" thought: as bound, the step cannot
  fail once the command is refused. `ExchangeWorld.apply` updates the value only on `Right`, and a
  `Left` carries no value. That is correct, since refusal purity follows from the type. It does
  mean the scenario proves "refused", not "unchanged", which is worth knowing when it is cited for
  SC-002.
  Answered: agreed; refusal purity follows from the type, and the scenario is cited for "refused". The property suite's replay check is what holds the value unchanged across a sequence

## Praise

- `Exchange.scala:286-287` praise: `decide` builds `next` as the fold of the events onto the value,
  so FR-002's "the next value is its events replayed" holds by construction for every command.
  The property suite still checks it independently.
- `PositionMoves.scala:26` praise: the buy leg's `restingToBuy` decrement sits beside the sell
  leg's `restingUnderSells`, so replay and the positions view share one rule and cannot drift.
- `ExchangeProperties.scala:83-105` praise: the suite keeps its own ledger of what the events say
  (pool money and each buy's reservation) and checks the value against it after every command.
  That is the right shape for SC-002. Printing the event counts at the end, so that a run which
  traded nothing would be visible, is a good guard.
- `Model.scala:128-141` praise: the binary insert fits the side's price-then-time ordering
  directly, and R2/V7 record why `Vector.search` was not used.
- `JournalFixtureWriter.scala` and `traded.jsonl` praise: the first release's fixture now carries
  money: a better-price release, a split trade, a cancel and paid-0 positions. The untouched
  `every-event.jsonl` and `opened.jsonl` confirm V6, since regeneration produced identical bytes.

## Checks applied

General checklist groups applied: Correctness, Design and scope, Error handling, Data and
migrations (stored types: `Position.restingToBuy` added with a default; no event, manifest or
wire name moved; the fixtures checked), Performance, Tests, API and compatibility (the published
domain API against `contracts/domain-api.md`), Dependencies and build (`build.sbt`'s
`Test / javaOptions` forwarding of `predictions.sequences` to the forked JVM: correct),
Documentation, and Commit and PR hygiene. Skipped: Concurrency (the domain is pure; the
components' concurrency is unchanged), Security beyond input validation at the domain's edge (no
endpoint, credential or CI change), and Observability (a library with no logging path).

Language skill: `pr-review-scala` (the bundled one; the repository has no `pr-review-scala` of
its own). Applied: types and totality (partial `orders(taker)` in `Matching` checked: the taker
was just placed, so it is safe), error handling (refusals in `Either`), collections and
performance, tests, build and API design. Serialization was checked through the fixtures and
`ReadsSuite`. Skipped: actors and Futures (none in the diff).

Project rules read: `CLAUDE.md` (the journal is a contract; money in minor units as `Long`;
every money command keyed; the traps, including that macOS has no `timeout`), the parent
`CLAUDE.md`, and the constitution gates as plan.md states them. No `.claude/rules/` and no
`REVIEW.md` in the repository. Enforced by tooling and not reviewed: formatting (scalafmt 3.9.4,
`runner.dialect = scala3`), unused code and warnings (`-Wunused:all -Werror`).

Tests run: `sbt exchangeDomain/test` on a `git archive HEAD` copy in the reviewer's scratchpad
(the checkout was not touched). Result: 77 of the change's tests passed, with 1 ignored (the
market's scenario, as intended). That includes `ExchangeFeatures` 65/65, `ExchangeProperties` at
100,000 sequences in 3.8 s, and the domain perf suite. Two probe suites added only to that copy
confirmed the overflow and the perf suite's pool path. Not run: `exchangeComponents/test` (needs
Docker) and `just check` (needs `uv`). For these the review relies on the commit `7cbc7d4`
message's report (77 + 16 + 9 + 5 passed, 0 findings).

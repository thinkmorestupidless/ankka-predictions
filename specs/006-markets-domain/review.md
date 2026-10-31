# Review: spec 006, the markets domain (origin/main...HEAD)

**Verdict:** Request changes
**What it does:** Fills `Market.apply` for all twelve commands: the draft's field rules and the change rules by state (`Drafting.scala`), approve/open on the open time, suspend/resume, close on time or early, move close, resolve, correct, void (now also from resolved within its window) and finalise. It also binds every scenario of `features/markets/` and the market scenario of `domain.feature`, adds a 100,000-sequence property suite, and regenerates the `0.0.0` fixtures with a resolved and a voided market. No event, field, manifest or wire name changes.
**Should it exist:** yes. It is spec 004's promised domain made real, with the rules where spec 004 put them and nothing added to the components' main sources (SC-003).
**Read:** in full: `Market.scala`, `Drafting.scala`, `Refusal.scala`, `Model.scala`, `MarketsWorld.scala`, `MarketProperties.scala`, `DraftingSuite.scala`, `MarketSerializerSuite.scala`, `MarketsDomainFeatures.scala`, the `JournalFixtureWriter` diff, both new fixtures, the feature-file diffs, `build.sbt`, README, `docs/limitations.md`, `notes/casino-requests.md`, spec, plan, tasks, research, data-model, `contracts/domain-api.md`, quickstart. Skimmed: `MarketsFeatures.scala` (the bindings: the world setup, the seeds and their reset per scenario), `GLOSSARY.md` (21 `*Proposed.*` markers removed, nothing else). Opened outside the diff: `MarketCommand.scala`, `MarketEvent.scala`, and `markets-components`' `MarketTimers.scala`/`MarketSchedule` and `MarketEntity.scala`, to check how a consumer acts on `nextDue` and which commands ask the port.
**Size:** 33 files, +1830 -185. About 260 lines of main Scala, about 990 of tests, and the rest is spec documents, features and fixtures. Over the 1000-line threshold, but most of it is design documents and generated fixtures. The logic is one module's `apply`, so a split is not asked for.

## Blocking

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:40` — issue (blocking): an approved market that is waiting for its open time can be changed to have no open time. `Changed` is accepted, the state stays `approved`, and `nextDue` becomes `None`. `MarketSchedule` (markets-components) then deletes the market's timer, so nothing ever opens the market. Probed on a `git archive HEAD` copy: `Change(ops, d.copy(openTime = None), now)` on `an approved market` gives `Right(Vector(Changed(...)))`, `state=Approved`, `nextDue=None`. FR-005 allows the change, and FR-013's "the open time while approved and ahead, none otherwise" literally yields `None`. R4's reasoning ("the timer path already exists") does not hold when there is no open time to time. The property suite cannot catch it, because its `due` oracle copies `nextDue` (see below). Its generator does produce this case: `Change` draws definitions with `openTime = None`.
  Suggestion: choose one rule and write its scenario first (constitution II):
  - refuse removing the open time of an approved market, naming the state (smallest);
  - or open in the same change when the new open time is none or has passed (`Changed` then `Opened`; R4 declined this);
  - or have `nextDue` answer `lastTime` for an approved market with no open time.
  Then amend FR-013 and `contracts/domain-api.md` to match.

## Should fix

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:50` — issue (non-blocking): a change to an approved market can move its open time and close both into the past, which gets around FR-006's "a close that has passed cannot be approved". `check(d, at)` compares the close only with the open time when there is one. Probed: an approved market changed to open 5 days ago and close 4 days ago is accepted. `Open(now)` is then accepted and the market is open with its close behind it. `MarketTimers` closes it on the next tick, and the exchange opens and closes with it. R3's own rationale says the close is compared with the command's time so that a market does not "open already closed"; this path does exactly that.
  Suggestion: in `Drafting.change`, for `MarketState.Approved`, refuse `Refusals.close(d.close)` when `!d.close.isAfter(at)`, the same check `approve` makes. Add the scenario "an approved market's close may not be changed to a time that has passed".
- `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/MarketProperties.scala:138` — issue (non-blocking): invariant 5 cannot fail independently, because `due(m)` is a line-for-line copy of `Market.nextDue`. The test computes its expectation the way the code under test does, which is how the blocking finding above passed 100,000 sequences. Suggestion: assert the property FR-013 is for instead, for example "an approved, open, suspended, resolved or void market always has a `nextDue`", or that applying the timer's command at `nextDue` is accepted (`contracts/domain-api.md`, "What the consumer relies on"). Either one would have gone red here.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:113` — issue (non-blocking): `market.definition.get` (here and at lines 149 and 162) and `market.nextDue.get` (line 239) are partial. They hold for every value built by `replay`, but `Market` is a public case class with defaults, so `Market(state = MarketState.Draft)`, or a snapshot from elsewhere, makes `apply` throw. The plan promises "every command total and pure". Not blocking, because no event sequence reaches it. Suggestion: match on `market.definition` / `nextDue` and answer `Refusals.state(market.state)` on `None`, as `Drafting.change` already does at line 36.

## Nits

- `Commit 54586da` — nitpick (non-blocking): `docs(005): built` marks spec 005 as built (CLAUDE.md, spec 005's status). It belongs to 005's close-out rather than this branch. Harmless if it is left on purpose; otherwise land it on `main` first so this range is 006 alone.

## Questions

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:173` — question: `resolve` accepts a blank `authority` and records it in the `Resolution`. R6 deliberately does not compare it with the definition's, but constitution V ("provenance over plausible prose") and the non-blank checks on every evidence part suggest a blank authority should be refused naming the authority, as the draft refuses one. Either answer settles it: a refusal plus a scenario, or a line in R6 saying a blank authority is the consumer's to prevent.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:19` — thought: outcome names are distinct by exact string, so `"yes"` and `"Yes"` (or `"yes "`) are two outcomes. Fine if the consumer normalises; worth a line in R3 if it does not.

## Praise

- `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/MarketsWorld.scala:64` — praise: every seed is reached by applying commands to `Market.empty` and fails loudly on a refusal. So every scenario tests a market that can exist, and every seed replays.
- `specs/006-markets-domain/research.md` "Found during implementation" — praise: the checks that can fail were actually run, and the quickstart was corrected when one of them proved not to fail. The property suite's reach was measured and the generator fixed: corrections went from 9 to 632 in 100,000. That is the evidence a property suite needs.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:49` — praise: one fixed order (finalise-of-final, then out of order, then state, then fields), stated once in the doc comment and in data-model's table, which the code follows line for line.

## Checks applied

General groups applied: Correctness, Design and scope, Error handling, Data and migrations (journal fixtures; no protocol change; `opened.jsonl` and `every-event.jsonl` are unchanged in the diff, consistent with V2), Tests, API and compatibility, Documentation, and Commit and PR hygiene. Skipped: Concurrency (pure functions, no threads), Security (no entry point, input handling, credential or workflow touched), Performance (a few comparisons per command), Dependencies (none added; `munit-scalacheck` already declared), Observability (no runtime path changed).

Language: `pr-review-scala` (bundled; the repository has no `pr-review-scala` of its own). Applied: types and totality, error handling, collections, tests (the `-D` forwarded to the forked JVM, `beforeEach` world reset), build, and API. Concurrency/actors and serialization were not relevant beyond the unchanged serializers. Already enforced and not reviewed: `-Werror -Wunused:all -source:3.7` and scalafmt.

Project rules read: `CLAUDE.md` (the journal-as-contract rule and the traps) and the parent workspace `CLAUDE.md`. `specs/005-exchange-domain/REVIEW.md` sits outside every directory this diff changes except spec 005's status line, so it was not applied. No `.claude/rules/`.

Tests run on a `git archive HEAD` copy in the scratchpad: `sbt marketsDomain/test` passed 57, ignored 4 (55 of the branch's own plus 2 probes; the 4 ignored are the exchange's scenarios of `domain.feature`). The property suite ran 100,000 sequences and every event kind occurred. `marketsComponents/test` was not run (needs Docker).

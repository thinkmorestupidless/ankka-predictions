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
  Resolved: a change to an approved market must keep an open time after the change's time, else refused naming the open time (Trevor's choice); scenario "an approved market's open time may not be removed", FR-005, R4, the data model and the contract amended, 7c02b4e. The independent oracle then found a second path to the same state (an open applied after the close), now refused naming the close, be39169.

## Should fix

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:50` — issue (non-blocking): a change to an approved market can move its open time and close both into the past, which gets around FR-006's "a close that has passed cannot be approved". `check(d, at)` compares the close only with the open time when there is one. Probed: an approved market changed to open 5 days ago and close 4 days ago is accepted. `Open(now)` is then accepted and the market is open with its close behind it. `MarketTimers` closes it on the next tick, and the exchange opens and closes with it. R3's own rationale says the close is compared with the command's time so that a market does not "open already closed"; this path does exactly that.
  Suggestion: in `Drafting.change`, for `MarketState.Approved`, refuse `Refusals.close(d.close)` when `!d.close.isAfter(at)`, the same check `approve` makes. Add the scenario "an approved market's close may not be changed to a time that has passed".
  Resolved: by the blocking fix. The open time must stay after the change's time and the close after the open time (R3), so the close is after the change too; `DraftingSuite` covers both moved into the past, 7c02b4e. A late open past the close is refused too, be39169.
- `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/MarketProperties.scala:138` — issue (non-blocking): invariant 5 cannot fail independently, because `due(m)` is a line-for-line copy of `Market.nextDue`. The test computes its expectation the way the code under test does, which is how the blocking finding above passed 100,000 sequences. Suggestion: assert the property FR-013 is for instead, for example "an approved, open, suspended, resolved or void market always has a `nextDue`", or that applying the timer's command at `nextDue` is accepted (`contracts/domain-api.md`, "What the consumer relies on"). Either one would have gone red here.
  Resolved: the oracle now works the next due time out from the journal alone, and asserts that an approved market is always due and that nothing opens past its close. With the open-time rule taken out it fails within 20,000 sequences, 2d511a0.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:113` — issue (non-blocking): `market.definition.get` (here and at lines 149 and 162) and `market.nextDue.get` (line 239) are partial. They hold for every value built by `replay`, but `Market` is a public case class with defaults, so `Market(state = MarketState.Draft)`, or a snapshot from elsewhere, makes `apply` throw. The plan promises "every command total and pure". Not blocking, because no event sequence reaches it. Suggestion: match on `market.definition` / `nextDue` and answer `Refusals.state(market.state)` on `None`, as `Drafting.change` already does at line 36.
  Resolved: `defined(market)` and `nextDue.toRight` refuse naming the state; no `.get` is left in `apply`, fff2b42.

## Nits

- `Commit 54586da` — nitpick (non-blocking): `docs(005): built` marks spec 005 as built (CLAUDE.md, spec 005's status). It belongs to 005's close-out rather than this branch. Harmless if it is left on purpose; otherwise land it on `main` first so this range is 006 alone.
  Declined: on purpose. 005 merged without its close-out, and the next spec's branch is where the repository's status lines move on, as 004's did on 005's branch.

## Questions

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:173` — question: `resolve` accepts a blank `authority` and records it in the `Resolution`. R6 deliberately does not compare it with the definition's, but constitution V ("provenance over plausible prose") and the non-blank checks on every evidence part suggest a blank authority should be refused naming the authority, as the draft refuses one. Either answer settles it: a refusal plus a scenario, or a line in R6 saying a blank authority is the consumer's to prevent.
  Answered: yes, refused naming the authority; scenario "a resolution naming no authority is refused", FR-009 and R6, aef76c0.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:19` — thought: outcome names are distinct by exact string, so `"yes"` and `"Yes"` (or `"yes "`) are two outcomes. Fine if the consumer normalises; worth a line in R3 if it does not.
  Answered: the consumer normalises; R3 now says so, in the review commit.

## Praise

- `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/MarketsWorld.scala:64` — praise: every seed is reached by applying commands to `Market.empty` and fails loudly on a refusal. So every scenario tests a market that can exist, and every seed replays.
- `specs/006-markets-domain/research.md` "Found during implementation" — praise: the checks that can fail were actually run, and the quickstart was corrected when one of them proved not to fail. The property suite's reach was measured and the generator fixed: corrections went from 9 to 632 in 100,000. That is the evidence a property suite needs.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:49` — praise: one fixed order (finalise-of-final, then out of order, then state, then fields), stated once in the doc comment and in data-model's table, which the code follows line for line.

## Checks applied

General groups applied: Correctness, Design and scope, Error handling, Data and migrations (journal fixtures; no protocol change; `opened.jsonl` and `every-event.jsonl` are unchanged in the diff, consistent with V2), Tests, API and compatibility, Documentation, and Commit and PR hygiene. Skipped: Concurrency (pure functions, no threads), Security (no entry point, input handling, credential or workflow touched), Performance (a few comparisons per command), Dependencies (none added; `munit-scalacheck` already declared), Observability (no runtime path changed).

Language: `pr-review-scala` (bundled; the repository has no `pr-review-scala` of its own). Applied: types and totality, error handling, collections, tests (the `-D` forwarded to the forked JVM, `beforeEach` world reset), build, and API. Concurrency/actors and serialization were not relevant beyond the unchanged serializers. Already enforced and not reviewed: `-Werror -Wunused:all -source:3.7` and scalafmt.

Project rules read: `CLAUDE.md` (the journal-as-contract rule and the traps) and the parent workspace `CLAUDE.md`. `specs/005-exchange-domain/REVIEW.md` sits outside every directory this diff changes except spec 005's status line, so it was not applied. No `.claude/rules/`.

Tests run on a `git archive HEAD` copy in the scratchpad: `sbt marketsDomain/test` passed 57, ignored 4 (55 of the branch's own plus 2 probes; the 4 ignored are the exchange's scenarios of `domain.feature`). The property suite ran 100,000 sequences and every event kind occurred. `marketsComponents/test` was not run (needs Docker).

## Second round

# Review: spec 006, the markets domain, the fixes (83250ed...HEAD)

**Verdict:** Approve with comments. The comments are documentation and are expected before merge; none changes behaviour.
**What it does:** Answers the first round in five commits. An approved market's change must keep an open time after the change's time, else it is refused naming the open time (7c02b4e). An open at or after the close is refused naming the close, and the market stays approved (be39169). `apply` has no partial `.get` left (fff2b42). The property suite works the next due time out from the journal and asserts that an approved market is always due and that nothing opens past its close (2d511a0). A resolve with a blank authority is refused naming the authority (aef76c0). Each rule has a scenario and the spec, research, data model and contract were amended to match, with the first round's resolutions recorded in this file (e1aaf74).
**Should it exist:** yes. Each commit closes one first-round finding, and nothing else is in the range.
**Read:** in full: the whole diff, `Drafting.scala` and the changed functions of `Market.scala` at HEAD, `Refusal.scala`, the `MarketProperties` oracle and assertions, the new `DraftingSuite` test, the new bindings in `MarketsFeatures.scala`, and the spec, research, data-model and contract hunks. Opened outside the diff: `Market.nextDue`, `MarketsWorld.comes`, the `naming` helper in `MarketsFeatures.scala`, and `markets-components`' `MarketTimers`/`MarketSchedule`, to see what the timer does with the new refusal of a late open.
**Size:** 12 files, +226 -38. About 30 lines of main Scala and 110 of tests; the rest is spec documents, features and this file. Reviewable in one pass.

### Resolutions of the first round

- Blocking (an approved market changed to have no open time is never opened): **holds.** `Drafting.change` refuses `field("open time")` when an approved market's new open time is none or at or before the change's time. The scenario "an approved market's open time may not be removed" and `DraftingSuite` cover it. With the check taken out (`&& false`, on a `git archive HEAD` copy), both the scenario and the property suite go red (`MarketProperties.scala:203`, "approved with nothing due").
- Should fix (open time and close both moved into the past): **holds.** The open time must be after the change's time and R3's close must be after the open time, so the close is after the change too. `DraftingSuite` covers both moved into the past. be39169 closes the other path to the same state, a late open: with its check taken out (`|| true`), the scenario "an approved market is not opened after its close" and the property suite (`:208`, "opened past its close") both go red.
- Should fix (the `due` oracle copied `nextDue`): **holds.** The oracle now folds the journal by event kind and does not read the market's state, so it can disagree with `nextDue`. The two new assertions state the property FR-013 is for. Both mutants above were caught by it, which confirms the commit message's claim.
- Should fix (partial `.get` in `apply`): **holds.** `defined(market)` and `nextDue.toRight(Refusals.state(...))` replace all four uses, and no `.get` is left in `apply`. No test builds a market by hand without a definition. The fix is mechanical and the types now show it, so no test is asked for.
- Nit (`docs(005): built` on this branch): declined with a reason (status lines move on the next spec's branch, as 004's did). That stands.
- Question (blank authority): **holds** in code and scenario (`Market.scala:186`; the binding passes `" "`, so the scenario exercises white space and not only the empty string). The data model's order table was not updated (see Should fix).
- Thought (outcome names by exact text): **holds.** R3 now says normalising is the consumer's.

### Blocking

None.

### Should fix

- `specs/006-markets-domain/data-model.md:43` — issue (non-blocking): the `Resolve` row still lists its refusals as state, outcome, evidence and review window. It leaves out the new blank-authority refusal, which the code checks between the evidence and the window (`Market.scala:186`). This table is what the first round praised as "one fixed order … which the code follows line for line", so it no longer matches the code. `contracts/domain-api.md:25`'s `field` row has the same gap: its "when" column names R3, a blank reason and the open time, but not a blank authority at resolve. Suggestion: `Resolve | not closed (state); the outcome not the market's (outcome); no evidence or a blank part (evidence); the authority blank (field authority); a negative window (review window)`, and add "or a resolve's authority is blank (R6)" to the contract row.
  Resolved: the `Resolve` row and the contract's `field` row name the blank authority, in the second-round commit.
- `specs/006-markets-domain/spec.md:304` — issue (non-blocking) (if-minor): FR-013 still says the next due time is "the open time while approved and ahead". be39169 now makes it lasting for an approved market to have an open time behind it. A market whose timer came after its close stays approved, and `nextDue` answers its passed open time (`Market.scala:30`). So the requirement and the code disagree in a state the design now produces on purpose. Suggestion: "the open time while approved (which a late open past the close leaves behind it)". Also add one line in the contract's "What the consumer relies on": the open is then refused naming the close, and `MarketTimers` logs it and does not retry.
  Resolved: FR-013 says the open time while approved, passed when an open was refused for its close, in the second-round commit.

### Nits

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Refusal.scala:20` — nitpick (non-blocking): the doc comment of `Refusals.field` lists the field names but not the new `open time`. The contract's table now lists it.
  Resolved: the doc comment lists `open time`, in the second-round commit.
- `specs/006-markets-domain/spec.md:272`, `specs/006-markets-domain/research.md:91` — nitpick (non-blocking): the amended FR-007 and R6 paragraphs were not rewrapped. One line runs to about 120 characters and another breaks after "it carries;". The formatter does not own Markdown here.
  Resolved: both rewrapped, in the second-round commit.

### Questions

- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Market.scala:137` — question: an approved market whose open was refused naming the close can only be recovered by an operator, who voids it or changes both times ahead (R5). `MarketTimers` logs the refusal at WARNING once and does not retry (`MarketTimers.scala`). `MarketSchedule` sets no further timer until another event arrives. What brings this market to an operator's attention? Either answer settles it: a line in R5 or `docs/limitations.md` that the view or the log is how an operator finds approved markets past their close, or a later spec's task (for example, a view query for approved markets whose close has passed). Nothing is asked of this spec's domain.
  Answered: the consumer's, as who may approve is; spec 023's markets surface lists approved markets past their close. `docs/limitations.md` now says the domain raises nothing for it.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:51` — thought: the contract now has two refusals a consumer reads as "open time". One is `field` with `field: open time`, for a change. The other is the kind `open time` with the time, for an early open. `field` was chosen because the new open time may be none, which is reasonable. The bound scenario cannot tell them apart, because the `naming` helper (`MarketsFeatures.scala:310`) accepts either a reason or a field of that name. `DraftingSuite` pins the `field` shape, so it is covered, but only there. No change asked.
  Answered: deliberate. The open-time refusal is a field refusal of a change; `openTime(at)` stays the open's own refusal before its time. `DraftingSuite` pins which one, as it should.
- `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/Drafting.scala:48` — thought: the open-time check comes before the description-only branch and before "no change". So once an approved market's open time has passed and before its timer has applied the open, even a description-only change is refused naming the open time, where it was accepted before. That follows FR-005 as amended, and it is the operator's way out of the stuck state above (both times moved ahead). Noted only so that it is a known consequence.
  Answered: intended. A change to an approved market must leave it due to open; a market whose open time passed unopened is one whose open was refused for its close, and only a void or a change of both times moves it.

### Praise

- `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/MarketProperties.scala:143` — praise: the oracle folds the journal by event kind and never reads the market's state or `nextDue`. It found a second defect (be39169) the first round missed, and it catches both fixes when they are taken out. A property suite should earn its keep this way.
- `Commit 7c02b4e`, `Commit be39169` — praise: each fix carries its scenario, the spec clarification with who decided and when, and the R4/R5 rationale and alternatives. Each commit message names the review finding it resolves.

### Checks applied

General groups applied: Correctness, Design and scope, Error handling (the refusal paths and what the timer does with them), Tests (both mutants run), API and compatibility (the refusal shapes in the contract, with no event, field or wire name changed), Documentation, and Commit and PR hygiene (Conventional Commits headers and the `Co-Authored-By` trailer on every commit). Skipped: Concurrency, Security, Data and migrations (no stored format or fixture changed), Performance, Dependencies, Observability (no runtime path changed in this range).

Language: `pr-review-scala` (bundled; the repository has none of its own). Applied: types and totality (the `.get` removal), error handling (typed `Either` refusals), tests (oracle independence, assertions that can fail). Already enforced and not reviewed: `-Werror -Wunused:all` and scalafmt.

Project rules read: `CLAUDE.md` and the parent workspace `CLAUDE.md`. The gather script listed `specs/006-markets-domain/REVIEW.md` as the repository's review checklist, but on this case-insensitive file system that file is this `review.md`, so no checklist of the repository's own applies.

Tests run on `git archive HEAD` copies in the scratchpad. `sbt marketsDomain/test` passed 59, ignored 4. The property suite ran 100,000 sequences, and every event kind occurred, Voided after Resolved included. Two mutants were run with `testOnly *MarketProperties *MarketsFeatures`. The open-time check disabled failed the property suite and the scenario "an approved market's open time may not be removed". The late-open close check disabled failed the property suite and the scenario "an approved market is not opened after its close". `marketsComponents/test` was not run (it needs Docker, and no component source changed).

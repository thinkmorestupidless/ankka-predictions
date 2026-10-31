---
description: "Task list for spec 006: the markets domain"
---

# Tasks: The Markets Domain

**Input**: `specs/006-markets-domain/` (plan, research R1 to R14, data-model, contracts/domain-api.md, quickstart)

**Tests**: requested. The spec names its scenarios in `features/markets/` and `features/libraries/domain.feature`; constitution II says a rule is in its feature before it is in code and IX holds one scenario to one test. Each binding task is written and seen failing before the rule it holds.

**Scope (plan, "What this feature builds")**: every command body of `Market.apply`, the field and change rules, the step definitions, the property suite, the components' tests made valid under the field rules, the `0.0.0` fixtures regenerated once. Nothing in the protocol moves (research R1).

Paths: `MK` = `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/`, `MKT` = `modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/`, `MKCT` = `modules/markets-components/src/test/scala/com/thinkmorestupidless/predictions/markets/components/`.

## Phase 1: Setup

- [X] T001 Run `sbt marketsDomain/test marketsComponents/test` on the branch and record the baseline in research.md "Found during implementation": `MarketStubSuite` green, every scenario of `features/markets/` reported ignored naming spec 006, the market scenario of `domain.feature` likewise

## Phase 2: Foundational (blocks every story)

**Purpose**: the refusals, the field rules every draft and change uses, and the step definitions' world.

- [X] T002 [P] In `MK/Refusal.scala` remove `notBuilt`; keep `field(name)` and document the names it takes (contracts/domain-api.md); add a `reviewWindow(length: Duration)` overload for a negative length (R9)
- [X] T003 [P] Write `MK/Drafting.scala` (R3): `Drafting.check(definition, at): Option[Refusal]` with the field rules in the order R3 gives, lengths by code point, blank as empty; and `Drafting.change(market, definition, at): Option[Refusal]` with R4's rules by state (draft: any field; approved: not the question, outcomes or currency; open to void: only the description; final: state), then `check`, then `no change`. Hold it in `MKT/DraftingSuite.scala`: each field at its bounds and one past them, 2 and 20 outcomes and 1 and 21, a duplicate name, a close equal to the open time, an expected resolution date on and before the close's UTC date, a blank question
- [X] T004 Write `MKT/MarketsWorld.scala` (R10): markets by id; now `2026-10-21T10:00:00Z`; a seed draft time `2026-09-01T00:00:00Z`; the Background's operator and review window; `seed(id, state, …)` reaching each state by applying commands to `Market.empty` and failing on any refusal; `apply(id, command)` recording the last answer; `comes(id, t)` applying what `nextDue` calls for at `t` (open, a close with no operator, finalise); `refusal` and `events` of the last answer
- [X] T005 Rewrite `MKT/MarketsFeatures.scala` as `GherkinSuite("features/markets")` on `MarketsWorld` with no `ranElsewhere`. Bind the shared steps: `an operator {string}`, `the review window is {string} hours`, `the market {string} is a|an {word} market` (state by its word), `the time {string} comes`, `the {word} is refused naming the {…}` for draft, change, approval, opening, suspend, resolution, correction and void (the reason, or the `field` name), `… naming the state {string}`, `… as no change`, `… as out of order`. Run the suite: every scenario fails on a missing step or a refusal, none passes

**Checkpoint**: `sbt marketsDomain/compile` passes; `DraftingSuite` green.

## Phase 3: User Story 1, a market is drafted, changed and approved (P1) 🎯 MVP

**Goal**: draft with its field rules, change by state, approve with the close check.

**Independent test**: the drafting, change and approval scenarios of `features/markets/lifecycle.feature` pass.

- [X] T006 [US1] Bind the drafting and change steps of `lifecycle.feature` in `MKT/MarketsFeatures.scala`: the full draft step, the outcome-list, `{string} outcomes`, open-and-close, close-and-expected-date and `asking {string}` variants (each a valid sample definition with the one field the step names; the full draft step has nine values, so it is bound with `with the outcomes "yes" and "no" in "GBP"` as literal text, R10), `a draft|an approved market {string} asking {string}`, `{string} changes the question|resolution criteria of the market {string} to {string}`, `the market {string} asks {string}`, `the market {string} has the resolution criteria {string}`, `{string} approves the market {string}` with and without ` at {string}`. See them fail
- [X] T007 [US1] Write the `draft` body in `MK/Market.scala`: `state` unless empty, then `Drafting.check(definition, at)`, else `Drafted`
- [X] T008 [US1] Write the `change` body: `Drafting.change(market, definition, at)`, else `Changed(operator, definition, at)`
- [X] T009 [US1] Complete `approve` (R5): `state` unless draft; `Refusals.close(close)` when the close is at or before the command's time; else `Approved`, with `Opened` when the open time is none or at or before it

**Checkpoint**: the US1 scenarios green.

## Phase 4: User Story 2, a market opens and closes on its times (P1)

**Goal**: open, close on time and early, move close, suspend and resume.

**Independent test**: the opening, closing, moving, suspending and resuming scenarios of `lifecycle.feature` and the two close scenarios of `resolution.feature` pass.

- [X] T010 [US2] Bind: `a draft market {string} with no open time|opening at {string}|closing at {string}`, `{string} approved the market {string}`, `an approved market {string} opening at {string}` (and `… and closing at {string}`), `the market {string} is opened at {string}`, `an open|a suspended market {string} closing at {string}`, `an open market {string} with the outcomes {string} and {string}`, `{string} closes the market {string} with the reason {string}`, `{string} closes the market {string} at {string}`, `the market {string} records the reason {string}`, `{string} moves the close of the market {string} to {string} with the reason {string}`, `the market {string} closes at {string}`, `{string} suspends|resumes the market {string}` (with ` with the reason {string}`). See them fail
- [X] T011 [US2] Write `open` (R5, kept), `close` (R7), `suspend` and `resume` (R8) and `moveClose` (R8: the new close later than the current, the expected resolution date moved to its UTC date where earlier) in `MK/Market.scala`

**Checkpoint**: the US1 and US2 scenarios green.

## Phase 5: User Story 3, a market is resolved, corrected, voided and made final (P1)

**Goal**: resolve, correct, void (from resolved within the window too), finalise.

**Independent test**: `features/markets/resolution.feature` passes.

- [X] T012 [US3] Bind: `a closed market {string} with the outcomes {string} and {string}` (and `… and the authority {string}`), `{string} resolves the market {string} to {string} on the evidence {string} at {string} saying {string}`, `… on no evidence`, `the resolution of {string} names the outcome {string}, the evidence and the authority {string}`, `the resolution of {string} names the outcome {string}`, `the resolution of {string} revises the first`, `a resolved market {string} resolved to {string} {string} hours ago|at {string}`, `{string} corrects the resolution of {string} to {string} on the evidence {string} at {string} saying {string}` (with ` at {string}`), `the review window of {string} ends at {string}`, `the market {string} is made final at {string}`, `an approved|a draft|a final market {string}`, `a void market {string} voided at {string}`, `{string} voids the market {string} with the reason {string} on the evidence {string} at {string} saying {string}`. The correction with a time has seven values, so it is bound with `on the evidence "premierleague.com" at "https://premierleague.com/tables"` as literal text (R10). See them fail
- [X] T013 [US3] Write `resolve` and `correct` (R6, R9) in `MK/Market.scala`
- [X] T014 [US3] Write `void` (R9): from approved, open, suspended, closed, and resolved while its window is open; refused naming the review window once it has ended
- [X] T015 [US3] Write `finalise` (R9): a final market answered with no event first, then the state, then the window's end

**Checkpoint**: `features/markets/` green but the US4 scenarios.

## Phase 6: User Story 4, the record and the next due time (P2)

**Independent test**: "a market records who did each change and when" and "a market tells the time it is next due to change" pass.

- [X] T016 [US4] Bind `a draft market {string} drafted by {string} at {string}`, `the market {string} shows drafted|approved by {string} at {string}`, `the market {string} is read`, `the market {string} is next due at {string}`. They pass against `record` and `nextDue` as spec 004 built them; if one does not, fix `replay`'s entry, never the scenario

## Phase 7: User Story 5, the value, its events and replay (P1)

**Independent test**: the market scenario of `domain.feature` passes; `MarketProperties` passes 100,000 sequences.

- [X] T017 [US5] In `MKT/MarketsDomainFeatures.scala` bind `a market {string} that has been approved, opened, closed and resolved`, `the market's events are replayed onto an empty market in sequence`, `the rebuilt market equals the market that produced them`; keep the exchange's scenarios in `ranElsewhere` through `MKT/Pending.scala`
- [X] T018 [US5] Write `MKT/MarketProperties.scala` (R11): 100,000 sequences (`-Dpredictions.sequences` overrides, forwarded by `build.sbt`'s `Test / javaOptions` for `marketsDomain` as for `exchangeDomain`), the six invariants of data-model.md, the event counts printed in `afterAll`
- [X] T019 [P] [US5] Rename `MKT/MarketStubSuite.scala` to `MKT/MarketSerializerSuite.scala`, keeping only the serializer round trip; the stub's tests go

**Checkpoint**: `sbt marketsDomain/test` green: every scenario, 0 ignored but the exchange's of `domain.feature`.

## Phase 8: The components stand on the real domain

- [X] T020 In `MKCT/ComponentsFeatures.scala`, `MKCT/MarketsComponentsSuite.scala` and any other test that drafts, make each definition valid under R3 (V3). Nothing in `src/main` changes
- [X] T021 In `MKCT/JournalFixtureWriter.scala` add `resolved.jsonl` and `voided.jsonl` (R13); delete `modules/markets-components/src/test/resources/journal/0.0.0/` and run `sbt "marketsComponents/Test/runMain com.thinkmorestupidless.predictions.markets.components.JournalFixtureWriter 0.0.0"`; confirm `opened.jsonl` and `every-event.jsonl` are unchanged (V2)
- [X] T022 `sbt marketsComponents/test` green (Docker)

## Phase 9: Polish

- [X] T023 [P] Update `README.md` (both domains' rules are in), `docs/limitations.md` (what the markets domain does and leaves: who may act, the category and currency lists)
- [ ] T024 [P] Append to research.md "Found during implementation" what V1 to V3 showed and anything the plan said otherwise
- [ ] T025 `just fmt`, `sbt test`, `just check` (0 findings); confirm `marketsDomain` still depends on `ankka-core` alone (FR-003); run the quickstart's two checks that can fail and put the code back

## Dependencies

- Phase 2 blocks every story. US1 before US2 (its seeds draft and approve), US2 before US3 (seeds close), US4 and US5 after US3. Phase 8 after US5; Phase 9 last.
- [P]: T002 and T003; T019 with T018; T023 and T024.

## Implementation strategy

US1 is the MVP: a market drafted, changed and approved under its rules. Each later phase is green on its own before the next; the components and fixtures follow once the domain is whole.

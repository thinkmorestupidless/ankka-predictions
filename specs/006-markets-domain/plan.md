# Implementation Plan: The Markets Domain

**Branch**: `006-markets-domain` | **Date**: 2026-10-31 | **Spec**: `specs/006-markets-domain/spec.md`

**Input**: Feature specification from `specs/006-markets-domain/spec.md`, clarified 2026-10-31
(a resolved market voided within its window; fields locked once open; the 21 glossary terms).

## Summary

Spec 004 built the markets domain's shape: the value, the twelve commands, the twelve events,
`replay` for every event, the refusal catalogue, the serializers, and `apply` for draft, approve
and open with no field checks, every other command refused as not built. This feature fills
`apply`: the draft's field rules, the change rules by state, approval and opening on the open time,
suspend and resume, the close on time and early, move close, resolve, correct, void (now also from
resolved, within the window) and finalise, each rule written in `features/markets/` before code
(constitution II, IX). Nothing changes in the protocol: no event case, field, manifest, component
id or wire name moves, and `Refusals.notBuilt` goes (research R1). The components change only where
they stood on the stub (R12), so SC-003 holds. The domain's suites grow by the bound features and a
property suite over 100,000 random command sequences (SC-002); the `0.0.0` journal fixtures are
regenerated once, with a market resolved, corrected and made final (R13).

## Technical Context

**Language/Version**: Scala 3.9.0, JDK 21, sbt 1.12.x (unchanged)
**Primary Dependencies**: `ankka-core` 0.14.2 alone at compile time (FR-003); tests on
`ankka-testkit` 0.14.2 (`GherkinSuite`), munit 1.3.6, munit-scalacheck 1.2.0 (declared)
**Storage**: none; the value is a case class, journalled by the consumer's entity
**Testing**: `features/markets/*` and the market scenario of `features/libraries/domain.feature`
bound in `markets-domain`'s test scope; `MarketProperties` (ScalaCheck); the components' suites
adjusted where they stood on the stub
**Target Platform**: JVM library
**Project Type**: library (one of four modules in one build)
**Performance Goals**: none stated; a command is a few comparisons on a value of one market
**Constraints**: every command total and pure; `==` is replay equality; no clock, settings, list,
database or network (FR-003)
**Scale/Scope**: one module's `apply` (twelve command bodies and the draft's field checks), four
test files, a small components pass, the fixtures

## Constitution Check

| Principle | Gate | Result |
|---|---|---|
| I. Money is exact; the wallet is the ledger | no money in the markets domain | pass by construction |
| II. Deterministic and replayable; a rule in its feature before code | `Market.apply` pure over the value and the command; every rule names its scenario | pass; the feature files predate this plan, the four of 2026-10-31 included |
| III. The casino's services reused, never forked | no casino code | pass by construction |
| IV. Belief before price | product rule | not applicable to the library |
| V. Provenance over plausible prose | every resolution, correction and void carries at least one piece of evidence (a source, a locator, an excerpt), each part non-empty | pass (R6) |
| VI. Commands over HTTP, facts over topics | the consumer's | not applicable |
| VII. Personal data marked and erased | `OperatorId` opaque; no player data | pass |
| VIII. Scala by default | Scala | pass |
| IX. Specs in the domain's words; scenarios live once | `just check` at 0 findings | pass |

No violation; nothing to justify. Post-design re-check: the same.

## Project Structure

### Documentation (this feature)

```text
specs/006-markets-domain/
├── plan.md              # this file
├── research.md          # R1 to R14, verify list, found during implementation
├── data-model.md        # the value, the state machine, each command's refusals in order, the invariants
├── quickstart.md        # how to prove it
├── contracts/
│   └── domain-api.md    # what this spec fixes of the domain API beyond spec 004's artefacts.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/
├── Market.scala         # apply for every command (R2 to R9); replay unchanged
├── Drafting.scala       # new: the definition's field rules and the change rules by state (R3, R4)
├── Model.scala          # MarketState's comment (void from resolved); Definition unchanged
├── Refusal.scala        # notBuilt removed; field(name) for the draft's fields
├── MarketCommand.scala, MarketEvent.scala, ids.scala   # unchanged
modules/markets-domain/src/test/scala/com/thinkmorestupidless/predictions/markets/
├── MarketsWorld.scala         # new: markets by id, a fixed now, seeds by state, the last answer (R10)
├── MarketsFeatures.scala      # features/markets/*: every scenario bound
├── MarketsDomainFeatures.scala # the market scenario bound; the exchange's stay ranElsewhere
├── MarketProperties.scala     # new: SC-002 (R11)
├── MarketSerializerSuite.scala # MarketStubSuite's serializer test kept; the stub tests go
├── DraftingSuite.scala        # new: each field's bounds at their edges
└── Pending.scala              # kept for the exchange's scenarios of domain.feature
modules/markets-components/
├── src/test/scala/.../components/ComponentsFeatures.scala, MarketsComponentsSuite.scala  # definitions valid under the field rules (R12)
├── src/test/scala/.../components/JournalFixtureWriter.scala  # resolved.jsonl joins opened.jsonl (R13)
└── src/test/resources/journal/0.0.0/                         # regenerated once
notes/casino-requests.md   # item 10: who may change a market or move its close (R12)
README.md, CLAUDE.md, docs/limitations.md   # the markets' rules are in
```

**Structure Decision**: everything lands in the modules spec 004 laid out; the domain gains one
file (`Drafting.scala`), the tests four. No new module, no new dependency.

## What this feature builds and what it leaves

| Built here | Left |
|---|---|
| `Market.apply` for all twelve commands; the field and change rules | who may do each (the resolution port, the consumer's); the categories and currencies a product offers (ankka-modules 023) |
| the step definitions for `features/markets/*` and the market scenario of `domain.feature`; the property suite | a permission check before `change` and `move close` (ankka-modules 023, item 10) |
| the components' adjustments and the regenerated `0.0.0` fixtures | the first tag and its `0.1.0` fixtures (Trevor) |

## Complexity Tracking

No constitution violation; nothing to justify.

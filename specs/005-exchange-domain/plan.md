# Implementation Plan: The Exchange Domain

**Branch**: `005-exchange-domain` | **Date**: 2026-10-31 | **Spec**: `specs/005-exchange-domain/spec.md`

**Input**: Feature specification from `specs/005-exchange-domain/spec.md`, clarified 2026-10-31
(the better price across outcomes, limits fixed at open, the eight glossary terms).

## Summary

Spec 004 built the exchange domain's shape: the value, the nine commands, the fourteen events,
`replay` for every event, the refusal catalogue, the serializers, and `apply` for `Open` alone with
every other command refused as not built. This feature fills `apply`: placing and cancelling,
matching by price and then by time with the two-outcome match through the pool, positions and
complete sets, suspension, the close and finality, and the reads of FR-020, each rule written in
`features/exchange/` before it is written in code (constitution II). Nothing changes in the
protocol: no event case, manifest, component id or wire name moves; one field is added to a stored
type with a default (`Position.restingToBuy`, research R6), and `Refusals.notBuilt` goes. The
components module changes only where it stood on the stub (research R14), so SC-004 holds. The
domain's suites grow by the bound features, a property suite over 100,000 random command sequences
(SC-002) and a place-under-a-millisecond measure (SC-003); the pre-release `0.0.0` journal fixtures
are regenerated once from the real domain (R13).

## Technical Context

**Language/Version**: Scala 3.9.0, JDK 21, sbt 1.12.x (unchanged)
**Primary Dependencies**: `ankka-core` 0.14.2 and nothing else at compile time (FR-003); tests on
`ankka-testkit` 0.14.2 (`GherkinSuite`), munit 1.3.6, munit-scalacheck 1.2.0 (already declared)
**Storage**: none; the value is a case class, journalled by the consumer's entity
**Testing**: the six feature files and the exchange half of `features/libraries/domain.feature`
bound in `exchange-domain`'s test scope; `ExchangeProperties` (ScalaCheck); `ExchangePerfSuite`
in the domain; the components' suites adjusted where they stood on the stub
**Target Platform**: JVM library
**Project Type**: library (one of four modules in one build)
**Performance Goals**: SC-003, a place on an exchange with 10,000 resting orders under 1 ms at the
median on a laptop; spec 004 SC-002's 10 ms through the entity follows from it
**Constraints**: every command total and pure; `==` is replay equality; events carry every money
figure and key (FR-018); no clock, wallet, player, database or network
**Scale/Scope**: one module's `apply` (about nine command bodies and a matcher), seven test files,
one components adjustment pass, the fixtures

## Constitution Check

| Principle | Gate | Result |
|---|---|---|
| I. Money is exact; the wallet is the ledger; every money command keyed | every amount a `Long` of minor units; the domain keeps positions, never money; every money event names amount, owner and key | pass: `Leg.charge/paid/released` with `releaseOrdinal`, `SetsSplit.charge`, `SetsMerged.paid`, `PositionPaid.paid`, each keyed (`contracts/domain-api.md`) |
| II. The book is deterministic and replayable; one value per market; money reserved before the book, moved after; a rule in its feature before code | `Exchange.apply` is a pure function over ordered collections; the taker's cost is recorded on `OrderPlaced` and the consumer reserved it before `place` (spec 004 R7); every rule here names its scenario | pass; the feature files predate this plan |
| III. The casino's services reused, never forked | no casino code | pass by construction |
| IV, V. Belief before price; provenance | product rules | not applicable to the library |
| VI. Commands over HTTP, facts over topics | the consumer's | not applicable; the library exposes no endpoint |
| VII. Personal data marked and erased | `OwnerId` opaque; nothing else personal | pass; erasure is the consumer's |
| VIII. Scala by default | Scala | pass |
| IX. Specs in the domain's words; scenarios live once | every scenario the spec names is in `features/`; `just check` at 0 findings | pass; one scenario row is kept by a domain parser (R10), none changes |

No violation; nothing to justify. Post-design re-check: the same.

## Project Structure

### Documentation (this feature)

```text
specs/005-exchange-domain/
├── plan.md              # this file
├── research.md          # R1 to R15, verify list, found during implementation
├── data-model.md        # the value's fields, the events' money fields and keys, the state machines, the invariants
├── quickstart.md        # how to prove it
├── contracts/
│   └── domain-api.md    # what this spec fixes of the domain API beyond spec 004's artefacts.md
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
modules/exchange-domain/src/main/scala/com/thinkmorestupidless/predictions/exchange/
├── Exchange.scala         # apply for every command; replay keeps restingToBuy; the reads of FR-020
├── Matching.scala         # new: the matcher, one place against the books (R3)
├── Model.scala            # Position.restingToBuy (default 0); Level; Book.insert by binary search (R2)
├── Quantity.scala         # new: Quantity.of(BigDecimal): a positive whole number or the refusal (R10)
├── Refusal.scala          # notBuilt removed; quantity takes the text it names
├── ExchangeCommand.scala, ExchangeEvent.scala, PositionMoves.scala, ids.scala   # unchanged
modules/exchange-domain/src/test/scala/com/thinkmorestupidless/predictions/exchange/
├── ExchangeFeatures.scala       # features/exchange/*: every scenario bound; Pending.scala goes
├── ExchangeDomainFeatures.scala # the three scenarios spec 004 left to this spec are bound
├── ExchangeWorld.scala          # new: the step definitions' world (exchanges by id, seeds, answers) (R10)
├── ExchangeProperties.scala     # new: SC-002, munit-scalacheck (R11)
├── ExchangePerfSuite.scala      # new: SC-003 (R12)
├── ExchangeSerializerSuite.scala # ExchangeStubSuite's serializer test, kept; the stub tests go
├── QuantitySuite.scala          # new: Quantity.of over the texts the features use
└── Samples.scala                # a sample of every event, unchanged in shape
modules/exchange-components/
├── src/main/scala/.../components/Reads.scala          # Depth.of delegates to the domain (R9)
├── src/main/scala/.../components/Settlement.scala     # PositionPaid with paid 0 asks the ledger nothing (R6)
├── src/test/scala/.../components/PlacementSuite.scala # the orders the exchange must refuse are refused by price, not as not built (R14)
├── src/test/scala/.../components/ComponentsFeatures.scala # the trade scenario is bound
├── src/test/scala/.../components/ExchangePerfSuite.scala  # the assume goes
├── src/test/scala/.../components/JournalFixtureWriter.scala # a traded scenario joins the opened one
└── src/test/resources/journal/0.0.0/                      # regenerated once (R13)
features/, GLOSSARY.md           # unchanged by this plan
README.md, CLAUDE.md, docs/limitations.md   # the exchange's rules are in; the markets' wait on 006
```

**Structure Decision**: everything lands in the modules spec 004 laid out; the domain gains two
files (`Matching.scala`, `Quantity.scala`) and the tests four. No new module, no new dependency.

## What this feature builds and what it leaves

| Built here | Left |
|---|---|
| `Exchange.apply` for place, cancel, split, merge, suspend, resume, close, finalise; the matcher; the reads | the markets domain (spec 006); driving the exchange from the market (the consumer, ankka-modules 023) |
| the step definitions for `features/exchange/*` and the exchange half of `domain.feature`; the property and perf suites | fees (a later spec and event); market surveillance |
| the components' adjustments to the real domain (R14) and the regenerated `0.0.0` fixtures | the first tag and its `0.1.0` fixtures (Trevor) |

## Complexity Tracking

No constitution violation; nothing to justify.

# Implementation Plan: Markets and Exchange as Libraries

**Branch**: `004-libraries` | **Date**: 2026-10-22 | **Spec**: `specs/004-libraries/spec.md`

**Input**: Feature specification from `specs/004-libraries/spec.md`, amended by D1 and D2 in
`docs/decisions.md`. Planned as T1 of experiment 001
(`ankka-brain/experiments/001-multi-agent-predictions/`): this plan is the contract the workers
on specs 005, 006 and the components code against, and it is approved by Trevor before any of
them starts.

## Summary

The repository becomes four Scala libraries: two pure domains on `ankka-core` alone, where an
exchange or a market is a value and a command yields a refusal or the next value and its events
(research R4), and two components artefacts on `ankka-sdk` that a consumer constructs from three
ports and registers itself (R5 to R9). The template's stub, image and deploy job go; publishing
is ankka-contrib's release job on a `v*` tag to Maven Central (R13). The domains' rules are specs
005 and 006's and are built by their own plans; this feature builds the layout, the contracts,
the ports, the components and the journal fixtures, and leaves the domain functions as stubs
that specs 005 and 006 fill.

## Technical Context

**Language/Version**: Scala 3.9.0, JDK 21, sbt 1.12.x (the family's line)
**Primary Dependencies**: `ankka-core` 0.12.0 (domains); `ankka-sdk` 0.12.0 (components);
`ankka-testkit` 0.12.0 and munit 1.3.6, munit-scalacheck (tests); `sbt-ci-release` 1.12.1
**Storage**: none in the libraries; the components' suites use `AnkkaTestKit`'s throwaway Postgres
**Testing**: munit; the living features through the testkit's Gherkin runner; property suites;
journal fixtures
**Target Platform**: JVM libraries consumed by an ankka service
**Project Type**: libraries, four artefacts, one build
**Performance Goals**: spec 005 SC-003 (a place on 10,000 resting orders under 1 ms median) is
the domain's; this feature adds no path that would slow it
**Constraints**: the domains read no clock, wallet, player, database or network (FR-003); the
events, wire names, manifests and component ids are a published protocol from the first tag
(FR-009)
**Scale/Scope**: four modules, three port traits, seven components, two fixture suites, two
workflows

## Constitution Check

`.specify/memory/constitution.md`, which still describes the product (the brain notes I, III, VI
and VII predate D1). Checked against what applies to libraries:

| Principle | Gate | Result |
|---|---|---|
| I. Money is exact; every money command keyed | amounts `Long`, every ledger call keyed | pass: `Money(amount: Long, owner)`, keys are the domain's ids (`contracts/ports.md`) |
| II. The book is deterministic and replayable; one value per market | `Exchange` is the value; replay equality is `==` | pass (R4) |
| III. The casino's services reused, never forked | the libraries hold no casino code | pass by construction: ports carry no casino vocabulary (FR-012) |
| IV, V. Belief before price; provenance | product rules | not applicable to the libraries; the markets domain keeps evidence on resolution (spec 006) |
| VI. Commands over HTTP, facts over topics | the consumer's | not applicable; the libraries expose no endpoint and publish no fact |
| VII. Personal data marked and erased | owner ids are opaque; nothing personal is held | pass; erasure is the consumer's (spec 004 FR-014) |
| VIII. Scala by default | all Scala | pass |
| IX. Specs in the domain's words; scenarios live once in features | every scenario this plan names is in `features/` | pass; `just check` at 0 findings before tasks |

No violation; nothing to justify. The constitution itself needs amending for the library shape
(a follow-up outside this feature, noted in the brain).

## Project Structure

### Documentation (this feature)

```text
specs/004-libraries/
├── plan.md              # this file
├── research.md          # R1 to R13, verify list, requests
├── data-model.md        # what each module holds
├── quickstart.md        # how to prove it
├── contracts/
│   ├── artefacts.md     # names, packages, manifests, component ids, wire names, construction
│   └── ports.md         # the three port traits and the rules at the seam
└── tasks.md             # /speckit-tasks
```

### Source Code (repository root)

```text
build.sbt                              # four projects, root aggregates, publishes nothing
project/Dependencies.scala             # V.scala, V.ankka, V.munit, V.scalacheck
project/plugins.sbt                    # sbt-ci-release, sbt-scalafmt
.scalafmt.conf, .githooks/pre-commit   # from ankka-contrib
.github/workflows/ci.yml               # pull requests: fmt, compile, test, just check
.github/workflows/release.yml          # v*: Maven Central
Justfile                               # test, check, hooks, fmt
modules/
├── exchange-domain/src/main/scala/com/thinkmorestupidless/predictions/exchange/
│   ├── Exchange.scala                 # the value, apply, replay, serializers (rules: spec 005)
│   ├── ExchangeCommand.scala, ExchangeEvent.scala
│   ├── Refusal.scala                  # Refusal, Refusals, Decision
│   └── ids.scala
├── exchange-domain/src/test/scala/.../exchange/
│   ├── ExchangeFeatures.scala         # features/exchange/*, the exchange half of features/libraries/domain.feature
│   └── ExchangeProperties.scala       # spec 005 SC-002 (filled by spec 005)
├── markets-domain/...                 # the same shape for Market
├── exchange-components/src/main/scala/.../exchange/components/
│   ├── Ports.scala                    # LedgerPort, GatePort, ExchangePorts, answers, Money
│   ├── ExchangeComponents.scala       # the class: entity, positions view, settlement consumer, timers, descriptors
│   ├── ExchangeEntity.scala
│   ├── PositionRows.scala
│   ├── Settlement.scala
│   └── ExchangeTimers.scala
├── exchange-components/src/test/scala/.../exchange/components/
│   ├── ScriptedPorts.scala
│   ├── ComponentsFeatures.scala       # features/libraries/components.feature, the orchestration scenarios of orders.feature
│   └── JournalCompatibilitySuite.scala
├── exchange-components/src/test/resources/journal/0.0.0/*.jsonl
└── markets-components/...             # ResolutionPort, MarketPorts, MarketsComponents, MarketEntity, CatalogueRows, MarketTimers, suites, fixtures
features/                              # unchanged by this feature except what speckit-bdd-features already wrote
docs/, GLOSSARY.md, notes/             # README rewritten; casino-requests gains R7's entry
```

**Structure Decision**: four modules under `modules/` (R1), with the domains and the components
in separate modules so that "depends on `ankka-core` alone" is a fact of the build and the pom
(V1), not a discipline. The root is the aggregate the template's single project becomes; nothing
stays at `src/`.

## What this feature builds and what it leaves to specs 005 and 006

| Built here | Left to 005 and 006 |
|---|---|
| the modules, the build, CI, the release job, the formatter and hook | nothing |
| `Refusal`, `Refusals`, `Decision`, the ids, the serializers, the enums' case names and every field the components need (ids, owner, outcome, side, price, quantity, duration, time, the money fields of FR-018) | the remaining fields and every rule inside `Exchange.apply` and `Market.apply`; the property suites |
| `Exchange.apply` and `Market.apply` as stubs that refuse everything as "not built" with a named reason, and `replay` for every event case | the real bodies, in the same signatures |
| the step definitions for `features/libraries/domain.feature`, `components.feature`, `compatibility.feature` | the step definitions for `features/exchange/*` and `features/markets/*` |
| the three ports, the seven components, the scripted ports, the fixture suites, the `0.0.0` fixtures | nothing; when the domains land, the fixtures are regenerated once before the first tag |

The enum case names and the manifests are fixed here because they are the protocol; a worker on
005 or 006 adds fields and rules and never renames a case. Each worker's own plan says what it
adds and cites this one.

## Dependency order for experiment 001

```
T2 (this feature's base: build, ports, stubs, fixtures) ─┬─► T3 spec 005 (exchange rules)
                                                         ├─► T4 spec 006 (market rules)
                                                         └─► T5 components: real behaviour against the real domains
```

T5 can start its safe part (scripted ports, suites, fixture format) against the stubs; it cannot
pass `components.feature` until T3 has landed, and it must not write a matching rule to get there.

## Open at the gate

1. Artefact ids `ankka-predictions-<module>` (R2). Alternatives: `predictions-<module>`.
2. The ledger patience is the consumer's, with no default (R8); confirm or set a default.
3. Component ids `exchange`, `market`, ... and the wire names in `contracts/artefacts.md`: a
   published protocol from the first tag; read once.
4. The settlement consumer drives release, settle, charge, pay and payout from events; reserve
   is the one call in the request path (R7). A reservation the exchange never accepted is asked
   of ankka-modules to expire.
5. Whether the catalogue view belongs in the library at all (spec 004 FR-014 says the catalogue
   is the consumer's; this plan keeps a minimal one for the timers and names it
   `market-catalogue`). Alternative: no view here; the consumer builds its own.

## Complexity Tracking

No constitution violation; nothing to justify.

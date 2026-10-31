---
description: "Task list for spec 004: markets and exchange as libraries"
---

# Tasks: Markets and Exchange as Libraries

**Input**: `specs/004-libraries/` (plan, research R1 to R14, data-model, contracts/ports.md, contracts/artefacts.md, quickstart)

**Tests**: requested. The spec names its scenarios in `features/libraries/`, the quickstart names the suites, and constitution IX holds one scenario to one test. Each step-definition task is written and seen failing before the code it holds.

**Scope (plan, "What this feature builds")**: the build, the four modules, the domain types and serializers with `apply` as a stub that refuses as "not built", `replay` for every event case, the three ports, the seven components, the scripted ports, the journal fixtures, CI and the release job. The rules inside `Exchange.apply` and `Market.apply` are specs 005 and 006's. A scenario that needs a rule (a trade, a resolution) is bound here and marked pending, naming the spec that lifts it.

Paths: `EX` = `modules/exchange-domain/src/main/scala/com/thinkmorestupidless/predictions/exchange/`, `MK` = `modules/markets-domain/src/main/scala/com/thinkmorestupidless/predictions/markets/`, `EXC` = `modules/exchange-components/src/main/scala/com/thinkmorestupidless/predictions/exchange/components/`, `MKC` = `modules/markets-components/src/main/scala/com/thinkmorestupidless/predictions/markets/components/`. The test sources mirror them under `src/test/scala/`.

## Phase 1: Setup

- [ ] T001 Remove the template's stub in one change (R1): `src/`, `service.json`, `docker-compose.yml`, `.github/workflows/deploy.yml`, the Docker and `schema` settings in `build.sbt`
- [ ] T002 Create `project/Dependencies.scala` with `V.scala` 3.9.0, `V.ankka` (overridable by `-Dankka.version`), `V.munit` 1.3.6 and `V.scalacheck`, and `ankkaCore`, `ankkaSdk` and `ankkaTestkit`. Pin `V.ankka` at 0.14.2 (Trevor, 2026-10-31; research "Found during implementation")
- [ ] T003 Rewrite `build.sbt` from voice's and ankka-contrib's: the four projects of `contracts/artefacts.md`, then `ThisBuild` organisation `com.thinkmorestupidless`, the Apache 2.0 licence, developers, `versionScheme`, `common` settings with `Test / fork := true`, and a root that aggregates with `publish / skip := true`. The domains depend on `ankkaCore` plus `ankkaTestkit % Test` and munit-scalacheck. The components depend on their domain plus `ankkaSdk` and `ankkaTestkit % Test`
- [ ] T004 [P] Add `sbt-ci-release` 1.12.1 and `sbt-scalafmt` to `project/plugins.sbt`. Drop the packager plugin
- [ ] T005 [P] Copy `.scalafmt.conf` and `.githooks/pre-commit` from `../ankka-contrib`
- [ ] T006 [P] Update `Justfile`: add `fmt` (`sbt scalafmtAll scalafmtSbt`), and keep `test`, `check` and `hooks`
- [ ] T007 Check that `sbt projects` lists the four projects and root, and that `sbt compile` passes on empty modules

## Phase 2: Foundational (blocks every story)

- [ ] T008 [P] Write the opaque string ids in `EX/ids.scala`: `MarketId`, `OutcomeId`, `OwnerId`, `OrderId`, `TradeId`, `SetKey`, `PositionKey`, each serialised as a string (`contracts/ports.md`, "Ids")
- [ ] T009 [P] Write the ids in `MK/ids.scala`: `MarketId`, `OutcomeId`, `OperatorId` (R1: the two libraries share no module)
- [ ] T010 [P] Write `Refusal(reason, names)`, `Decision(next, events)` and the `Refusals` catalogue in `EX/Refusal.scala`. The catalogue includes `outOfOrder`, `notBuilt` and `ledgerUnavailable` (R8)
- [ ] T011 [P] Write the same three in `MK/Refusal.scala`, with the markets' own catalogue

**Checkpoint**: both domains compile with no dependency but `ankka-core`.

## Phase 3: User Story 1, the exchange domain as a value and its events (P1) 🎯 MVP

**Goal**: an `Exchange` value, `ExchangeCommand`, `ExchangeEvent`, `apply` and `replay`, with serializers under the manifests `exchange` and `exchange-event`.

**Independent test**: `sbt exchangeDomain/test` passes the exchange scenarios of `features/libraries/domain.feature` with no container started, and `exchangeDomain/makePom` names `ankka-core` alone (V1).

- [ ] T012 [US1] Bind the exchange scenarios of `features/libraries/domain.feature` (book rebuilt from events, refused command unchanged, time carried by the command, every money event names amount, owner and key) in `modules/exchange-domain/src/test/scala/.../exchange/ExchangeFeatures.scala` through the testkit's Gherkin runner, with no actor system (V4). See them fail
- [ ] T013 [US1] Bind every scenario of `features/exchange/*.feature` that names no gate or ledger in the same file, as pending with the reason "spec 005", so the runner counts them and spec 005 lifts them
- [ ] T014 [US1] Write `ExchangeCommand` in `EX/ExchangeCommand.scala`: `Open`, `Place`, `Cancel`, `Split`, `Merge`, `Close`, `Finalise`. Each carries `at: Instant` and the fields the components need (ids, owner, outcome, side, price, quantity, duration)
- [ ] T015 [US1] Write `ExchangeEvent` in `EX/ExchangeEvent.scala`, with spec 005 FR-019's twelve cases by the names in `contracts/artefacts.md`:
  - every money-moving case names amount, owner and key (FR-018);
  - the cases that release carry the release's `ordinal`;
  - `TradeMade` carries `through: Through` (Direct, Split, Merge) and `legs: Vector[Leg]`, each leg with owner, order, charge, paid and released (R14).
  `Through` and `Leg` live in the domain package, and the port reuses them
- [ ] T016 [US1] Write `Exchange` in `EX/Exchange.scala`:
  - the value, its `empty`, and `SortedMap`/`Vector` fields;
  - `apply`, which refuses an out-of-order `at` and otherwise every command as `Refusals.notBuilt`;
  - `replay` for every event case, and `lastTime`;
  - `given Serializer[Exchange]` and `given Serializer[ExchangeEvent]`. Give a fieldless case an explicit codec (V6)
- [ ] T017 [US1] Run `sbt exchangeDomain/test` until T012's scenarios pass. Hold the replay scenario with hand-built event sequences while `apply` is a stub
- [ ] T018 [US1] Confirm V1: `sbt exchangeDomain/makePom` lists `ankka-core` alone in compile scope. Record the result in research "Found during implementation"

**Checkpoint**: the exchange domain stands alone.

## Phase 4: User Story 2, the markets domain as a value and its events (P1)

**Goal**: the same shape for `Market`, with `nextDue` (spec 006 FR-013).

**Independent test**: `sbt marketsDomain/test` passes the market scenario of `domain.feature`. `features/markets/*` are bound and pending on spec 006.

- [ ] T019 [P] [US2] Bind the market scenario of `features/libraries/domain.feature`, and every scenario of `features/markets/lifecycle.feature` and `resolution.feature` as pending on "spec 006", in `modules/markets-domain/src/test/scala/.../markets/MarketsFeatures.scala`. See the bound scenario fail
- [ ] T020 [P] [US2] Write `MarketCommand` in `MK/MarketCommand.scala`: `Draft`, `Change`, `Approve`, `Open`, `Close`, `MoveClose`, `Resolve`, `Correct`, `Void`, `Finalise`. Each carries `at`, plus `operator` where someone acts
- [ ] T021 [P] [US2] Write `MarketEvent` in `MK/MarketEvent.scala`, with spec 006 FR-014's ten cases by the names in `contracts/artefacts.md`
- [ ] T022 [US2] Write `Market` in `MK/Market.scala`:
  - the value and its `empty`;
  - `apply`, which refuses an out-of-order `at` and otherwise refuses as `notBuilt`;
  - `replay` for every case;
  - `nextDue: Option[Instant]`;
  - serializers under `market` and `market-event`
- [ ] T023 [US2] Run `sbt marketsDomain/test` until T019's bound scenario passes, and confirm V1 for `marketsDomain/makePom`

**Checkpoint**: both domains stand alone.

## Phase 5: User Story 3, components with ports (P2)

**Goal**: `ExchangeComponents(ExchangePorts)` and `MarketsComponents(MarketPorts)`. They hold seven components with the ids of `contracts/artefacts.md` and register nothing.

**Independent test**: `sbt exchangeComponents/test marketsComponents/test` passes `features/libraries/components.feature` in `AnkkaTestKit` against scripted ports, for the scenarios a stub domain can hold. The trade scenario is pending on spec 005.

- [ ] T024 [P] [US3] Write `EXC/Ports.scala` exactly as `contracts/ports.md` gives it:
  - `Money`, `LedgerAnswer`, and `LedgerPort` with `reserve`, `release(market, order, ordinal, money)`, `settle(market, trade, through, legs)`, `charge`, `pay` and `payout`, every call naming the `MarketId`;
  - `GateAnswer` and `GatePort`;
  - `ExchangePorts(ledger, gate, ledgerPatience)`
- [ ] T025 [P] [US3] Write `MKC/Ports.scala`: `ResolutionAnswer`, `ResolutionPort` with `mayDraft`, `mayApprove`, `mayResolve`, `mayVoid` and `mayClose`, and `MarketPorts`
- [ ] T026 [P] [US3] Write scripted ports in `modules/exchange-components/src/test/scala/.../ScriptedPorts.scala`:
  - the gate allows or refuses with a reason;
  - the ledger answers done, insufficient or refused, or throws n times and then answers;
  - every call is recorded with its key, so a test can see a repeat answered as the first was.
  Write the same for the resolution port in `modules/markets-components/src/test/scala/.../ScriptedPorts.scala`
- [ ] T027 [US3] Confirm V2 (a companion as an anonymous subclass inside a class sees its handlers) and V5 (a library entity can schedule the consumer-hosted timed action), each with a throwaway test in `exchangeComponents`. Record the answers in research "Found during implementation". If V5 fails, expose scheduling as a function the consumer calls, as R9 allows
- [ ] T028 [US3] Bind `features/libraries/components.feature` in `modules/exchange-components/src/test/scala/.../ComponentsFeatures.scala` (exchange scenarios) and `modules/markets-components/src/test/scala/.../ComponentsFeatures.scala` (the resolution-port scenario), on `AnkkaTestKit` with a throwaway Postgres (R11):
  - the trade scenario is pending on "spec 005";
  - the orchestration scenarios of `features/exchange/orders.feature` are pending on "spec 005" where they need a resting order.
  See them fail
- [ ] T029 [US3] Write `ExchangeEntity` in `EXC/ExchangeEntity.scala`, with id `exchange` and the wire names of `contracts/artefacts.md`:
  - `place` asks the gate. On a buy it then reserves the cost keyed by the order id, retrying a throw with a short backoff within `ledgerPatience` and then refusing as `ledgerUnavailable` (R8). Only after `Done` does it apply the domain and persist;
  - a gate or ledger refusal reaches the caller as a `Refusal` carrying the port's reason, with no event (R7, FR-007);
  - every other command applies the domain;
  - the queries are `get`, `prices`, `depth` and `position`
- [ ] T030 [US3] Write `Settlement` in `EXC/Settlement.scala`, with id `exchange-settlement`: a consumer of the exchange's events that calls the ledger at least once, naming the entity id as the market.
  - `OrderCancelled` and `OrderExpired` → `release` with the event's ordinal;
  - `TradeMade` → `settle` with `through` and `legs`;
  - `SetsSplit` → `charge`, and `SetsMerged` → `pay`;
  - `PositionPaid` → `payout`;
  - a throw is retried, and a refusal is logged with the key and never retried for ever
- [ ] T031 [P] [US3] Write `PositionRows` in `EXC/PositionRows.scala`: id `exchange-positions`, one row per owner and outcome, query `by-owner`
- [ ] T032 [P] [US3] Write `ExchangeTimers` in `EXC/ExchangeTimers.scala`: id `exchange-timers`, handler `close`. It applies `Close` at the scheduled time, and returns done when the exchange is already closed
- [ ] T033 [US3] Write `ExchangeComponents` in `EXC/ExchangeComponents.scala`: built from `ExchangePorts`, holding `exchange`, `positions`, `settlement`, `timers` and `descriptors`, with no `registerAll` anywhere (R5)
- [ ] T034 [US3] Write `MarketEntity` in `MKC/MarketEntity.scala`, with id `market`:
  - `draft`, `approve`, `close`, `resolve`, `correct` and `void` ask the resolution port first;
  - a refusal changes nothing;
  - after every command it reschedules the market's timer for `nextDue` (R9);
  - the query is `get`
- [ ] T035 [P] [US3] Write `CatalogueRows` in `MKC/CatalogueRows.scala`, with id `market-catalogue` and queries `by-state` and `by-category`. Plan open item 5 still asks whether this view belongs here: keep it minimal, and note in the README that the consumer's product catalogue is its own
- [ ] T036 [P] [US3] Write `MarketTimers` in `MKC/MarketTimers.scala`: id `market-timers`, handler `due`. It applies `Open`, `Close` or `Finalise` by the market's state when it fires, and returns done when there is nothing to do
- [ ] T037 [US3] Write `MarketsComponents` in `MKC/MarketsComponents.scala`: `market`, `catalogue`, `timers` and `descriptors`
- [ ] T038 [US3] Write the "registered by the consumer" scenario: start a service with an empty descriptor list, ask for an exchange, and see none. Then start one with `descriptors`, and the exchange answers
- [ ] T039 [US3] Run `sbt exchangeComponents/test marketsComponents/test` until every non-pending scenario of T028 passes, including the patience test: the ledger throws twice, then allows, and the order is accepted; it throws past the patience, and the order is refused as `ledger unavailable` with no event

**Checkpoint**: a consumer can build and register both libraries. Trades wait on spec 005.

## Phase 6: User Story 4, the journal is a contract (P2)

**Goal**: fixtures for 0.0.0, replayed in CI, and an unknown manifest or `type` fails, naming it.

**Independent test**: `sbt 'exchangeComponents/testOnly *JournalCompatibilitySuite' 'marketsComponents/testOnly *JournalCompatibilitySuite'`.

- [ ] T040 [US4] Bind `features/libraries/compatibility.feature` in `modules/exchange-components/src/test/scala/.../JournalCompatibilitySuite.scala` and `modules/markets-components/src/test/scala/.../JournalCompatibilitySuite.scala` (R12), and see them fail
- [ ] T041 [US4] Add a fixture writer in test scope that runs a scripted scenario through the serializers. It writes `manifest<TAB>json` lines and a last `state<TAB>json` line to `modules/<x>-components/src/test/resources/journal/0.0.0/<name>.jsonl`. Write the 0.0.0 fixtures from hand-built event sequences that cover every event case
- [ ] T042 [US4] Make the suites replay every file of every version directory and compare the value. A `future.v9` manifest line and an unknown `type` line must each fail, naming it, with nothing after them applied (FR-010)
- [ ] T043 [US4] Confirm V3: the release job can refuse a tag whose fixture directory is missing. Otherwise move the check into a test the job runs. Record the result in research

## Phase 7: User Story 5, publishing by tag (P3)

**Goal**: CI on pull requests and Maven Central on a `v*` tag (D2, R13).

**Independent test**: `sbt publishLocal` puts four artefacts under the dynver version. The release job is not run in this feature.

- [ ] T044 [P] [US5] Rewrite `.github/workflows/ci.yml` as ankka-contrib's: `scalafmtCheckAll scalafmtSbtCheck compile test` on pull requests, plus a `features` job that runs `just check` with `uv`
- [ ] T045 [P] [US5] Write `.github/workflows/release.yml` from ankka-contrib's, minus the image:
  - on `v*`: a clean-tree check, and an "already on repo1" check against `ankka-predictions-exchange-domain_3/<version>/*.pom`;
  - `sbt ci-release` with `CI_SONATYPE_RELEASE: sonaBundle`, then the Central Portal upload, then the deployment status look;
  - secrets `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME` and `SONATYPE_PASSWORD` by name. Setting them is Trevor's
- [ ] T046 [US5] Run `sbt publishLocal` and confirm the four artefacts under `~/.ivy2/local/com.thinkmorestupidless/ankka-predictions-*_3/<dynver>/`

## Phase 8: Polish and cross-cutting

- [ ] T047 Remove `features/money/` and `features/projects/` (spec 004 edge case, D2). ankka-modules spec 022 carries them, merged on 2026-10-31. Remove any glossary terms only they used, until `just check` passes
- [ ] T048 [P] Rewrite `README.md`: the four libraries, how a consumer pins and constructs them (`contracts/artefacts.md`, "Construction"), how to test, and that the domain rules arrive with specs 005 and 006
- [ ] T049 [P] Update `CLAUDE.md`'s "Working here", since the template's stub is gone and modules live under `modules/`
- [ ] T050 Run `sbt scalafmtCheckAll scalafmtSbtCheck test` and `just check`, which must have 0 findings, and every quickstart step but publishing by tag. Fill in research "Found during implementation"

## Dependencies

- Phase 1 → Phase 2 → US1 and US2, which are independent of each other and can run in parallel.
- US3 needs US1 (the exchange components) and US2 (the market components). Its two halves are independent.
- US4 needs US3's modules and the domains' serializers.
- US5 needs Phase 1 only. It can run beside US1 to US4.
- Polish comes last.
- Specs 005 and 006 start once US1 and US2 (the stubs and their signatures) have merged. They lift the pending scenarios of T013, T019 and T028.

## Parallel examples

- After T011: T012 to T018 (US1) beside T019 to T023 (US2).
- In US3: T024, T025 and T026 together, then the exchange half (T029 to T033) beside the market half (T034 to T037).
- US5's T044 and T045 at any point after T007.

## Implementation strategy

MVP is US1: the exchange domain's types, serializers and replay, on `ankka-core` alone. That's what spec 005 needs to start writing rules. US2 follows, so spec 006 can start. US3 and US4 fix the published protocol (ports, component ids, the journal) before any tag. US5 is last, and the first tag is Trevor's.

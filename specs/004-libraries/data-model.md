# Data model: the libraries

What each module holds. The fields of the two values and their commands and events are specs
005 and 006's and are fixed by their plans; this page fixes the kinds of thing and where they
live. Signatures are in `contracts/`.

## exchange-domain

| Thing | Kind | Notes |
|---|---|---|
| `Exchange` | immutable value | spec 005 FR-001: outcomes, unit, limits, state, one `Book` per outcome, orders by id, positions by owner and outcome, sets outstanding, remainder, last prices, next sequence, last time. `Exchange.empty`. Collections `SortedMap` and `Vector`, so `==` is replay equality |
| `Book` | value inside `Exchange` | resting orders by price then sequence |
| `Order`, `Trade`, `Position` | values | spec 005 key entities |
| `ExchangeCommand` | enum | `Open`, `Place`, `Cancel`, `Split`, `Merge`, `Suspend`, `Resume`, `Close`, `Finalise`; each with `at: Instant` |
| `ExchangeEvent` | enum | spec 005 FR-019's fourteen; every money-moving case names amount, owner and key (FR-018) |
| `Refusal`, `Refusals` | value, catalogue | reason and the names (field, limit, state, owner, id); `Refusals.outOfOrder`, `.price`, ... |
| `Decision` | value | `(next, events)` |
| `Exchange.apply`, `Exchange.replay` | functions | the whole rule set |
| ids | opaque strings | `MarketId`, `OutcomeId`, `OwnerId`, `OrderId`, `TradeId`, `SetKey`, `PositionKey` |
| serializers | givens | manifests `exchange`, `exchange-event` |

State machine of the value: `empty → open ↔ suspended → closed → final` (spec 005 FR-002, FR-015a, FR-016, FR-017).

## markets-domain

| Thing | Kind | Notes |
|---|---|---|
| `Market` | immutable value | spec 006 FR-001; `Market.empty` |
| `Resolution`, `Void`, `Evidence`, `Change` (the record) | values | spec 006 key entities |
| `MarketCommand` | enum | `Draft`, `Change`, `Approve`, `Open`, `Suspend`, `Resume`, `Close`, `MoveClose`, `Resolve`, `Correct`, `Void`, `Finalise`; each with `at` and, where one acts, `operator` |
| `MarketEvent` | enum | spec 006 FR-014's twelve |
| `Refusal`, `Refusals`, `Decision` | as the exchange's | the markets domain has its own copies; the two libraries share no module (R1) |
| `Market.apply`, `Market.replay`, `Market.nextDue` | functions | FR-013's next due time is a function of the value |
| ids | opaque strings | `MarketId`, `OutcomeId`, `OperatorId` |
| serializers | givens | manifests `market`, `market-event` |

State machine: `empty → draft → approved → open ↔ suspended → closed → resolved → final`, with
`void` from approved, open, suspended or closed (and from resolved within the window if the gate says so), then `final`.

## exchange-components

| Thing | Kind | Notes |
|---|---|---|
| `LedgerPort`, `GatePort`, `ExchangePorts` | traits, value | `contracts/ports.md` |
| `ExchangeComponents(ports)` | class | holds the companions below; `descriptors` |
| `ExchangeEntity` | event sourced entity, state `Exchange`, events `ExchangeEvent` | id `exchange`; `place` asks the gate then reserves then applies; every other command applies the domain |
| `PositionRows` | view, source the entity's changes, one row per owner and outcome | id `exchange-positions` |
| `Settlement` | consumer of the entity's events | id `exchange-settlement`; release, settle, charge, pay, payout, keyed, at least once |
| `ExchangeTimers` | timed action | id `exchange-timers`; `close` applies `Close` at the time the consumer scheduled |
| scripted ports | test only | allow, insufficient, refuse with reason, throw n times then allow |

## markets-components

| Thing | Kind | Notes |
|---|---|---|
| `ResolutionPort`, `MarketPorts` | trait, value | `contracts/ports.md` |
| `MarketsComponents(ports)` | class | `descriptors` |
| `MarketEntity` | event sourced entity, state `Market`, events `MarketEvent` | id `market`; `draft`, `approve`, `suspend`, `resume`, `close` (early), `resolve`, `correct`, `void` ask the port first; after every command reschedules the market's timer for `nextDue` |
| `CatalogueRows` | view of the markets' changes | id `market-catalogue`; the consumer's product catalogue is its own (spec 004 FR-014); this one answers by state and category for the timers and the consumer's reads |
| `MarketTimers` | timed action | id `market-timers`; `due` applies `Open`, `Close` or `Finalise` according to the market's state at the time |

## Journal fixtures (test resources)

`modules/<x>-components/src/test/resources/journal/<version>/<scenario>.jsonl`: lines
`manifest<TAB>json`, last line `state<TAB>json`. Versions: `0.0.0` written by this feature; each
release adds its own and removes none.

## Build

| File | Holds |
|---|---|
| `build.sbt` | the four projects and the root; `common` settings as voice's; `publish / skip` on the root |
| `project/Dependencies.scala` | `V.scala`, `V.ankka` (overridable by `-Dankka.version`), `V.munit`, `V.scalacheck` |
| `project/plugins.sbt` | `sbt-ci-release`, `sbt-scalafmt` |
| `.scalafmt.conf`, `.githooks/pre-commit` | from ankka-contrib |
| `.github/workflows/ci.yml` | pull requests: format check, compile, test, `just check` |
| `.github/workflows/release.yml` | `v*`: Maven Central, four artefacts |
| `Justfile` | `test`, `check`, `hooks`, `fmt` |
| `README.md` | the libraries, how to consume them, how to test |

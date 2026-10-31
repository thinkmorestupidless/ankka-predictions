# Research: Markets and exchange as libraries

The decisions this plan rests on, each with what it rests on and what else was considered. R1 to
R4 are the build and the artefacts; R5 to R9 the shape of a domain and of the components; R10 to
R12 the tests and the journal fixtures; R13 the release. "Verify at implementation" lists what the
plan asserts and has not yet run here. "Found during implementation" is appended as the work goes.

Discovery (experiment 001, T0, 2026-10-22) established that ankka needs no change for this
feature: `ankka-core` depends on jsoniter alone and gives `Codecs.serializer`, `Serializer`,
`CommandError` and `ErrorCode`; `ankka-sdk` depends on core alone and holds every component
kind the components need. The seam with ankka is consumption only.

## R1. Four sbt modules under `modules/`, a root that aggregates and publishes nothing

**Decision.** `modules/exchange-domain`, `modules/markets-domain`, `modules/exchange-components`,
`modules/markets-components`. Each domain module has `libraryDependencies += ankkaCore` and
nothing else; each components module depends on its domain module and `ankkaSdk`, with
`ankkaTestkit % Test`. The root project aggregates the four with `publish / skip := true`. The
template's stub (`src/`, `Main`, the `Item` entity, endpoint and view, `service.json`,
`docker-compose.yml`, the `schema` task, the Docker settings, `.github/workflows/deploy.yml`) goes
in the same change: a library repository runs nowhere and has no image.

**Rests on.** voice's `build.sbt` (one `common` settings block, a `domain` on `ankkaCore` alone,
`publish / skip` on what is not published); ankka-contrib's `build.sbt` (the published modules,
`ThisBuild` organisation, licence, developers, `versionScheme`). Versions move to
`project/Dependencies.scala` as both do, with `-Dankka.version=<v>` overriding `V.ankka` for a
locally published ankka.

**Considered.** Two modules, one per library, with the components in a sub-package: rejected
because the domain's "depends on `ankka-core` alone" (FR-003) is then not a build-level fact.
Keeping the stub beside the libraries as an example consumer: rejected; the example consumer is
the components' test suite (R11), and a runnable service here would need the runtime, a database
and an image the repository has no reason to carry.

## R2. Artefact ids are prefixed `ankka-predictions-`

**Decision.** `com.thinkmorestupidless:ankka-predictions-exchange-domain_3`,
`ankka-predictions-exchange-components_3`, `ankka-predictions-markets-domain_3`,
`ankka-predictions-markets-components_3`. The sbt project names are the short forms
(`exchangeDomain`, `exchangeComponents`, `marketsDomain`, `marketsComponents`).

**Rests on.** The group `com.thinkmorestupidless` is shared by every family repository;
ankka-reasoning publishes `ankka-reasoning-graph` and ankka-contrib `ankka-contrib-neo4j-graph-store`.
A bare `exchange-domain` in that group says nothing about where it came from.

**Considered.** `predictions-exchange-domain`: shorter, but every other family artefact starts
with `ankka-`. The gate confirms the names; they are a published contract once tagged.

## R3. Packages: `com.thinkmorestupidless.predictions.{exchange,markets}` and `.components`

**Decision.** The exchange domain is `com.thinkmorestupidless.predictions.exchange`; its
components `com.thinkmorestupidless.predictions.exchange.components`. Likewise `markets`. The
consumer imports two packages per library and nothing from `com.example`.

**Rests on.** The template's `com.example.ankkapredictions` is a placeholder the README says to
replace. ankka-reasoning's layers are `com.thinkmorestupidless.reasoning.<layer>`.

## R4. A domain is a value, a command function and a replay function, with its serializers

**Decision.** Each domain exposes, in its package:

- a value type (`Exchange`, `Market`) as an immutable case class with an `empty`;
- a `Command` enum and an `Event` enum (`ExchangeCommand`, `ExchangeEvent`; `MarketCommand`,
  `MarketEvent`), every case carrying the time it applies at (`Instant`) and, where an operator or
  owner acts, who;
- a `Refusal` case class (`reason: String`, `names: Map[String, String]`) and a `Refusals`
  catalogue of the reasons the specs name (the price, the quantity, the outcome, the state, the
  limit, the owner, the id, out of order, and so on);
- `object Exchange { def apply(exchange: Exchange, command: ExchangeCommand): Either[Refusal, Decision] }`
  where `Decision(next: Exchange, events: Vector[ExchangeEvent])`, and
  `def replay(exchange: Exchange, event: ExchangeEvent): Exchange`; likewise `Market`;
- the serializers, as `given`s in the companion: `Codecs.serializer[Exchange]("exchange")`,
  `Codecs.serializer[ExchangeEvent]("exchange-event")`, `Codecs.serializer[Market]("market")`,
  `Codecs.serializer[MarketEvent]("market-event")`.

The serializers live in the domain, not in the components, because the manifests and the JSON
shape are the journal contract FR-009 makes the library's; a consumer that journals the events
without the components (a test, a tool) must get the same bytes.

Collections inside a value are ordered and structural (`Vector`, `SortedMap`, `TreeMap`) so that
`==` is replay equality (FR-001) with no custom equality to get wrong.

**Rests on.** voice's `ActionRecord`, `ActionEvent` and `Action.apply(state, event)` are the
replay half of this shape (`voice/modules/domain/src/main/scala/voice/domain/accountability/Action.scala`);
its `Refusal` and `Rules` catalogue are the refusal half. ankka's `Codecs.make` uses a `type`
discriminator, allows recursive types and keeps `None` fields, so an enum of case classes
serialises by case name.

**Considered.** voice's exact shape, where the entity assembles checks and `effects.persist`: it
spreads the rule across domain and entity and cannot answer "next value and events" to a test with
no runtime, which spec 004 US1 and US2 and FR-011 require. A command as a method on the value
(`exchange.place(...)`): fine for Scala, worse for the enum the serializers and the fixtures need.

## R5. The components are built by the consumer from the ports, and register nothing

**Decision.** Each components module exposes a class the consumer constructs with its ports:

```scala
final class ExchangeComponents(ports: ExchangePorts):
  val exchange:   EventSourcedEntity.Companion[ExchangeEntity, Exchange, ExchangeEvent]   // the entity
  val positions:  View.Companion[...]                                                      // a view
  val settlement: Consumer.Companion[...]                                                  // drives the ledger from events
  val timers:     TimedAction.Companion[...]                                               // close and expiry on their time
  def descriptors: Vector[ComponentDescriptor]
```

and `MarketsComponents(ports: MarketPorts)` likewise with `market`, `catalogue`, `timers`. The
consumer does `Ankka.service.registerAll(exchangeComponents.descriptors ++ marketsComponents.descriptors)`
in its own `Main`. Nothing in a library calls `registerAll`; `features/libraries/components.feature`'s
last scenario is held by a suite that constructs the components and starts a service that
registers none, then asks for a book.

The companions are anonymous subclasses of `EventSourcedEntity.Companion` and the others, created
inside the class so that `create(ctx)` closes over the ports. The component ids are fixed by the
library (`exchange`, `market`, `exchange-positions`, `exchange-settlement`, `exchange-timers`,
`market-catalogue`, `market-timers`) because the wire names, the component ids and the manifests
are the protocol (spec 004 FR-009; ankka's rule that a wire name is a versioning boundary).

**Rests on.** `EventSourcedEntity.Companion` is an abstract class with `create(ctx)` abstract and
`descriptor` a `def`, so an instance per ports works (`ankka/modules/sdk/.../EventSourcedEntity.scala`
lines 102 to 210). ankka's `registerAll` takes descriptors.

**Considered.** Ports as ankka components the consumer registers under known ids, called through
`ctx.componentClient`: it would make every port call a sharded call and the scripted ports of
the tests a second set of entities. Rejected for this release; it can be added behind the same
trait later without changing the journal.

## R6. Ports are synchronous Scala traits; a failure is an exception, a refusal is a value

**Decision.** `contracts/ports.md` has the signatures. Every call returns an answer type; a port
that cannot answer throws, and the components treat a throw as "unavailable". Handlers run on a
virtual thread, so a blocking port call costs nothing (ankka's `CLAUDE.md`, virtual threads).

**Rests on.** ankka's own `ComponentClient.invoke` is blocking for the same reason.

## R7. The reservation is made by a placement workflow keyed by the order; the entity never calls a port

**Decision.** *Rewritten 2026-10-31 after the analysis (finding C1): the first version reserved
inside the entity's `place` handler, which made the book wait on the ledger, against constitution
II.* `exchange-components` holds an `exchange-placement` workflow, one instance per order, id
`<market>/<order>`, in the pattern ankka-modules' wallet uses for every keyed command: `start` is
the atomic claim, the outcome lives in the workflow's state, and a repeat under the same id is
answered with the outcome the first received (the consumer's endpoint polls `outcome`). Its steps:

1. `gate`: ask the gate port; refused, record the refusal and end.
2. `reserve` (a buy only): ask the ledger to reserve the cost (quantity times price, a function
   of the domain) keyed by the order id; `Insufficient` or `Refused`, record and end.
3. `apply`: call the exchange entity's `place`, which applies the domain and persists its events
   and nothing else; a domain refusal of a buy transitions to `release`, else record and end.
4. `release`: give the reservation back under the order id and ordinal 0, which no event of the
   exchange uses, since the exchange produced none; record the refusal and end.

Every step may run twice: the ledger answers a repeat as the first was (spec 004 FR-008), and the
entity answers a repeated `place` as it answered the first (spec 005). A crash between `reserve`
and `apply` resumes at `apply`; one between `apply` and the end resumes with the entity's answer.
So a reservation the exchange never accepted is released by this workflow and is no longer asked
of ankka-modules. Release, settle, charge, pay and payout are driven by the `settlement` consumer
from the persisted events, each keyed as spec 005 FR-018 says, at least once. A sell goes
through `gate` and `apply` alone.

**Rests on.** Constitution II ("money is reserved before an order reaches the book ... the book
never waits on the ledger"); ankka's workflow rules (a command decides and transitions, a step
does the work, every step idempotent, recovery declared in settings); ankka-modules
`services/wallet` (`SettlementWorkflow`, `AccountMoveWorkflow`) as the precedent for start as the
claim.

**Considered.** Reserving inside the entity (the first version): one call per order, but the
market's entity is serialised per id, so every buy held every other command on that market for
the wallet's round trip, and the wallet's latency landed in spec 004 SC-002's 10 ms entity
budget. Reserving by a consumer after the fact: breaks "reserved before an order reaches the
book".

## R8. A ledger that fails rather than refuses: the `reserve` step is retried within a patience the consumer sets, then the order is refused

**Decision.** `ExchangePorts` carries `ledgerPatience: java.time.Duration`. The placement
workflow declares the `reserve` step's recovery from it: a throw is retried with the runtime's
backoff until the patience is spent, then the step fails over to a step that records the refusal
`ledger unavailable` and ends; no event is produced and the caller sees the refusal in the
outcome. The library has no default: the consumer passes one, as it passes the review window to
the markets domain. This answers spec 004's open clarification and was confirmed at the gate.

**Rests on.** Spec 006's rule that the domain has no defaults the consumer should own; ankka's
rule that recovery is declared in settings because the runtime enforces it. Since R7 moved the
call out of the entity, the retry holds up this order alone and not the market.

**Considered.** Persisting a pending order in the exchange and completing it later: a state the
domain (spec 005) has no rule for, and visible in the book.

## R9. Timers: one `TimedAction` per library, applying the market's next-due change

**Decision.** `markets.timers` is a `TimedAction` the consumer's timer runtime hosts. After every
market command the entity schedules (through `ctx`'s timer scheduler in the consumer's runtime) a
single timer named by the market id for the market's `nextDue` (spec 006 FR-013), which applies
`open`, `close` or `finalise` at that time. `exchange.timers` does the same for an exchange's
close, driven by the markets' events through the consumer. Driving the exchange from the market
is the consumer's (spec 006, assumptions); the library gives it the timer and the commands.

**Rests on.** `TimedAction.Companion` and `TimerScheduler` in `ankka-sdk`; the recurring-timer
trap in ankka's `runtime.md` does not apply to single timers.

## R10. Features run against the domains through the testkit's Gherkin runner, test scope only

**Decision.** The domain modules take `ankkaTestkit % Test` so `features/exchange/`,
`features/markets/` and `features/libraries/domain.feature` run through the family's living
features runner with step definitions that call the domain functions and never start a runtime.
The published domain artefact still depends on `ankka-core` alone: a test-scope dependency is not
in the pom's compile scope. Property tests (spec 005 SC-002, spec 006 SC-002) use
`munit-scalacheck`.

**Rests on.** FR-003 is about what a consumer pulls in; `publishLocal` and `sbt
"show exchangeDomain/makePom"` prove the scope. Verify V1.

**Considered.** Step tables in plain munit, no Gherkin: loses the one-scenario-one-test link the
checker and spec-kit rely on.

## R11. The components' suites are the example consumer

**Decision.** Each components module's test suite constructs the components with scripted ports
(allow, refuse as insufficient, refuse with a reason, throw) and runs them in `AnkkaTestKit` with
a throwaway Postgres, so `features/libraries/components.feature` and the orchestration scenarios
of `features/exchange/orders.feature` pass against real sharding and a real journal. The "registers
none" scenario starts a service with an empty descriptor list and asks for an exchange.

**Rests on.** `AnkkaTestKit` and `restartService()` (ankka `CLAUDE.md`, Testing).

## R12. Journal fixtures: one file per released version, one event per line, the final value last

**Decision.** `modules/<x>-components/src/test/resources/journal/<version>/<name>.jsonl`: each
line `manifest<TAB>json`, written by that release's serializers from a scripted scenario, and a
last line `state<TAB>json` with the value the scenario produced. `JournalCompatibilitySuite`
replays every file of every version through the current serializers and `replay` and compares
the value; it also writes a line with the manifest `future.v9` and asserts the failure names it,
and a line with an unknown `type` and asserts the same, which holds FR-010 and the third scenario
of `compatibility.feature`. Before the first tag there is no release; this feature writes the
`0.0.0` fixture from the run's `main`, and the first release rewrites it as `0.1.0` and keeps it
forever. The release job refuses a tag whose fixture directory is missing (V3).

**Rests on.** ankka's `JournalRecordCompatibilitySuite` (`ankka/modules/testkit/src/test/`), hex
lines in a text file read by the current release, the same idea with JSON because the events are
JSON. The manifest check is the runtime's: replay resolves by manifest and an unknown one fails;
the `type` check is jsoniter's.

## R13. Release copies ankka-contrib's job, minus the image

**Decision.** `sbt-ci-release` 1.12.1 in `project/plugins.sbt`; `.github/workflows/release.yml`
on `v*`: clean-tree check, "already on repo1" check against
`ankka-predictions-exchange-domain_3/<version>/*.pom`, `sbt ci-release` with
`CI_SONATYPE_RELEASE: sonaBundle`, the Central Portal upload, the deployment status look. Secrets
by name: `PGP_SECRET`, `PGP_PASSPHRASE`, `SONATYPE_USERNAME`, `SONATYPE_PASSWORD`; none is set on
this repository yet, and setting them is Trevor's, after the gate. `ci.yml` becomes contrib's
(`scalafmtCheckAll scalafmtSbtCheck compile test`) on pull requests, plus a `features` job running
`just check` with `uv`. `.scalafmt.conf` and `.githooks/pre-commit` come from ankka-contrib; the
template did not carry them.

**Rests on.** `ankka-contrib/.github/workflows/release.yml` and `build.sbt`, 2026-10-22. The
version is sbt-dynver's from the tag; the template's `version := SERVICE_VERSION` goes.

## R14. The ledger port is shaped by the wallet that will answer it

**Decision.** Checked on 2026-10-31 against ankka-modules spec 022 (merged as its PR #20), whose
wallet routes are the first `LedgerPort`. Three changes to `contracts/ports.md`: every call names
the `MarketId`; `release` carries the release's ordinal for its order, from one; and `settle`
takes how the trade was made (`Through`: direct, split, merge) and its legs, each with its order,
charge, payment and release. The domain's release-carrying events (a fill's price improvement, a
fill-at-once remainder, a cancel, an expiry) carry the ordinal, and `TradeMade` carries the legs,
so `settlement` drives the ledger from the events alone (spec 005 FR-018).

**Rests on.** Spec 005 releases from one order more than once (FR-005, FR-008, FR-016), so a
release keyed by the order alone answers the second as a repeat of the first; spec 022 keys its
release by order and sequence for that reason. Spec 005 FR-011 matches two buys through the pool
with no seller, which a buyer-to-seller settlement cannot say. The pool and the currency are the
consumer's and are found from the market.

**Considered.** Leaving the port as it was and letting the consumer compose keys: the consumer
cannot tell an order's releases apart from events that do not number them, and the port is a
published protocol from the first tag (FR-009), so the shape is fixed here. One port call per
leg: a split-matched trade would then be two calls a crash can separate, which the consumer can
still choose by deriving a key per leg.

**Requests.** ankka-modules spec 022's split debits available money and its settlement runs from
a buyer to a seller; neither charges a reservation into a pool. A trade through the pool needs
that (`notes/casino-requests.md`, item 9).

## Verify at implementation

- **V1.** `sbt exchangeDomain/makePom` lists `ankka-core` and nothing else in compile scope.
- **V2.** `EventSourcedEntity.Companion` can be instantiated as an anonymous subclass inside a
  class and its `descriptor` sees the handlers declared in the body (the `def`, not `val`, note
  in its doc comment says so).
- **V3.** The release job can read the fixture directory for the tag's version before
  `ci-release`, or the check moves into a test the job runs.
- **V4.** The Gherkin runner in `ankka-testkit` 0.14.2 runs a feature directory with step
  definitions and no actor system started (voice's domain suites are the precedent to read).
- **V4a.** The Gherkin runner can mark a bound scenario pending with a reason and count it, so
  the scenarios specs 005 and 006 lift are bound here and counted, not skipped (tasks T012, T013).
- **V5.** A `TimedAction` hosted by the consumer can be scheduled from an entity in a library
  module through `EventSourcedEntityContext` (or the components expose the scheduling as a
  function the consumer calls).
- **V6.** `Codecs.make` on a Scala 3 enum whose cases are case classes gives a `type`
  discriminator per case name; a fieldless case needs an explicit codec (ankka's `CLAUDE.md`
  trap).

## Found during implementation

- **2026-10-31, the ankka pin.** `V.ankka` is 0.14.2, the family's current release (Trevor),
  not the 0.12.0 this plan was written against; ankka-modules' predictions module moves to it when
  it is built. The gate's five open items were accepted as written.
- **2026-10-31, V1.** Holds: `exchangeDomain/makePom` and `marketsDomain/makePom` list
  `scala3-library` and `ankka-core` in compile scope and nothing else; munit, munit-scalacheck and
  `ankka-testkit` are test scope.
- **2026-10-31, V4 and V4a.** `GherkinSuite` (ankka-testkit 0.14.2) is a munit suite and starts no
  actor system. It has no pending status: a scenario named in `ranElsewhere` is reported ignored
  with the text given, never passed, and a name that is no scenario fails the suite. The scenarios
  waiting on specs 005 and 006 are listed there by title (each suite's `Pending.all`), reported as
  "run by spec 005, when its rules are built". A bound scenario with no rule behind it would fail,
  so nothing is passed that is not held.
- **2026-10-31, V6.** Confirmed: under ankka's codec a fieldless enum case is `{"type":"Sell"}`.
  Every status-word enum (`Side`, `OrderDuration`, `OrderState`, `ExchangeState`, `Through`,
  `SetKind`, `MarketState`) has a string codec in its companion, as ankka's own rule says; an
  unknown discriminator fails naming it.
- **2026-10-31, the event serializers live in the event enums' companions**, not in `object
  Exchange` and `object Market` as `contracts/artefacts.md` first said: implicit search for
  `Serializer[ExchangeEvent]` looks in `ExchangeEvent`'s companion, so a consumer's entity finds it
  with no import. The contract says so now.
- **2026-10-31, `Leg.releaseOrdinal`.** FR-018 keys every release by its order and an ordinal, the
  one a trade frees included, so a leg carries the ordinal its release used; the consumer may
  settle the release under the trade (spec 022's `unused`) and ignore it.
- **2026-10-31, US1's replay scenario waits on spec 005.** T017 said to hold it with hand-built
  events; but "the exchange that produced them" would then be the replay itself, a test that
  cannot fail. `ExchangeStubSuite` instead holds what spec 004 builds (open, cost, the not-built
  refusal, every event case through the serializer), and US4's fixtures hold replay against
  recorded values.
- **2026-10-31, the analysis.** Its C1 rewrote R7 and R8 (the reservation in a workflow, not the
  entity); C2 reworded the spec to one value per market; H1 builds `Open` and the cost here, since
  the components' scenarios start from an open market; the rest were wording.

## Requests

- To ankka-modules: none for an orphaned reservation; the placement workflow releases it (R7,
  rewritten 2026-10-31).
- To ankka-modules (`notes/casino-requests.md`, item 9): a split charged from a reservation, for a
  trade matched through the pool (R14).
- To ankka: none from this feature.

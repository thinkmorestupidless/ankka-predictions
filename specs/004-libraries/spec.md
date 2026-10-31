# Feature Specification: Markets and Exchange as Libraries

**Feature Branch**: `004-libraries`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "This repository becomes two private libraries, markets and
exchange, that eitheror's predictions module in ankka-modules composes. Each is a pure domain on
ankka-core alone, where a book or a market is a value, a command yields a refusal or the next
value and its events, and an event replays onto a value, plus a components artefact of event
sourced entities, views and timers written against the domain, with the outside world behind
ports: a ledger port for reserve, release, settle and pay, a gate port, a resolution port.
eitheror registers the components, supplies the ports and writes the endpoints, the facts and the
reasoning mirror. The libraries own their events, wire names and manifests, which evolve
additively because eitheror's journals hold them. Published privately by tag, never to Maven
Central. Spec 001 moves to eitheror's core module; the orchestration halves of specs 002 and 003
move to eitheror's predictions module; their domain halves stay here."

## Context

Until 2026-10-21 this repository was a product: two projects on one installation, the casino's
services deployed beside new ones. The product is now eitheror's, a module of ankka-modules, and
what stays here is what is not eitheror's and not gambling's: the rules of a limit order book over
outcomes that pay one unit, and the lifecycle of a market from draft to final. Both are already
written as rules the checker holds to contradictions (`features/exchange/`, `features/markets/`),
and both know nothing of a player, a brand, a wallet or a regulator. ankka-reasoning publishes its
layers as libraries and composes them in its service; voice keeps every pure domain in a module
depending on `ankka-core` alone. This feature gives markets and exchange that shape.

The split is the one the features already make. Matching, positions, complete sets, settlement
arithmetic and the market state machine are domain scenarios: they name no gate and no wallet.
Order gating, reservation, facts, the catalogue and the mirror into reasoning are orchestration:
they name the casino's services. The domain stays; the orchestration becomes eitheror's, cited by
version, and the ports are the seam between them.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - The exchange domain is a value and its events (Priority: P1)

A developer takes the `exchange-domain` library and holds a book of one outcome as a value. They
apply a command (place, cancel, expire, split, merge, close, resolve) and receive either a refusal
that names its reason or the next book and the events that led to it. They apply those events to
an empty book and get the same book back. Nothing in the library reads a clock, a wallet, a player
or a network; a time a rule needs is a value the command carries. The library depends on
`ankka-core` alone.

**Why this priority**: The book is the thing worth having and the thing hardest to get right; a
pure, replayable book is what constitution principle II asks for and what every consumer needs.

**Independent Test**: Property tests over random order sequences: every command either refuses
without change or yields events whose replay equals the result; two runs of one sequence are equal;
the pool invariant holds after every step.

**Acceptance Scenarios**:

- added `features/libraries/domain.feature`: a book rebuilt from its events alone is the book that produced them
- added `features/libraries/domain.feature`: a refused command leaves the book it was applied to unchanged
- added `features/libraries/domain.feature`: the time a rule needs is carried by the command, not read

The scenario "the same orders in the same sequence produce the same trades" in
`features/exchange/matching.feature` stands as written and runs against the value alone.

---

### User Story 2 - The markets domain is a value and its events (Priority: P1)

The same shape for a market: draft, approve, open, close, resolve, void, finalise, each a command
on a value, each refusal naming its reason, each event replaying. The review window and the
earliest open time are durations and instants the command carries. The library depends on
`ankka-core` alone.

**Why this priority**: The exchange domain's close and resolution commands are driven by the
market's; the two are released together.

**Independent Test**: Every scenario of `features/markets/lifecycle.feature` and
`features/markets/resolution.feature` runs against the value alone, with no service.

**Acceptance Scenarios**:

- added `features/libraries/domain.feature`: a market rebuilt from its events alone is the market that produced them

Every scenario of `features/markets/lifecycle.feature` and `features/markets/resolution.feature`
stands as written and runs against the value alone.

---

### User Story 3 - Components with ports (Priority: P2)

A consumer takes `exchange-components` and `markets-components`: one event sourced entity per
outcome book, one per market, the position and catalogue views, the timers for open, close and
expiry, written against the domains. The outside world is behind three ports the consumer
supplies: a ledger port (reserve, release, settle, pay, each keyed), a gate port (may this
player place this order, answering a refusal with its reason), and a resolution port (who may
resolve and void). The consumer registers the components explicitly in its own service and
supplies the ports; the library registers nothing by itself. A ledger that refuses a reservation
leaves the book unchanged; a trade is settled through the ledger keyed by its trade id, so a
repeat is answered as the first was.

**Why this priority**: Without the components every consumer rewrites the same entities; with
them eitheror writes endpoints, facts and ports and nothing else.

**Independent Test**: Run the components in the testkit with in-memory ports scripted to allow
and to refuse; every scenario of `features/exchange/orders.feature` passes with the gate port and
the ledger port standing in for the casino's services.

**Acceptance Scenarios**:

- added `features/libraries/components.feature`: a ledger that refuses the reservation leaves the book unchanged and the order resting nowhere
- added `features/libraries/components.feature`: a gate that refuses the order reserves nothing and names the gate's reason
- added `features/libraries/components.feature`: a trade is settled through the ledger keyed by the trade, and a repeat is answered as the first was
- added `features/libraries/components.feature`: a resolution the resolution port refuses changes nothing
- added `features/libraries/components.feature`: the components are registered by the consumer and never by the library

---

### User Story 4 - The journal is a contract (Priority: P2)

The libraries own their events, wire names and manifests, and a consumer's journal holds them.
Every release changes them additively: a journal written by an earlier release replays on a later
one and yields the same values. A release that cannot keep that promise is a new major version
with a migration the release notes name.

**Why this priority**: A library whose events change shape breaks the consumer's audit trail; the
promise is the cost of owning the events, taken knowingly.

**Independent Test**: A fixture journal written by each released version is replayed by the
current one in CI.

**Acceptance Scenarios**:

- added `features/libraries/compatibility.feature`: a journal written by an earlier release replays on a later one to the same values
- added `features/libraries/compatibility.feature`: an event a release no longer produces still replays

---

### User Story 5 - Publishing by tag (Priority: P3)

A tag `v*` publishes the four artefacts to Maven Central under `com.thinkmorestupidless` at the
tag's version, as ankka-contrib does; nothing is published by hand (D2 of 2026-10-22; D1 said a
private registry). A consumer pins a version, never a
branch. The version is the one sbt-dynver derives.

**Why this priority**: The family's rule; it has no behaviour to test beyond the release job.

**Independent Test**: Tag a release candidate on a branch and confirm the four artefacts appear
under that version and nowhere else.

**Acceptance Scenarios**: none. FR-013 and FR-014 are held by the release job, not by a scenario.

---

### Edge Cases

- A command carrying a time earlier than the book's last event: refused as out of order; the domain
  keeps a last time and never goes backwards.
- A ledger port that fails rather than refuses (unreachable): the component retries the keyed call
  and the order stays pending, neither resting nor refused, until the port answers; the consumer
  sees the pending state. *Resolved (research R8): the component retries within a patience the
  consumer sets, with no default in the library, then refuses the order as "ledger unavailable";
  no pending state is persisted.*
- An event whose manifest the current release does not know: replay fails loudly naming the
  manifest; it is never skipped.
- Spec 001's features (`features/money/`, `features/projects/`): they describe the wallet and the
  grants, which are eitheror's now. They stay here, unchanged, until the amending spec in
  ankka-modules carries them, and are removed in the same change.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: `exchange-domain` MUST model a book of one outcome as a value, with commands that
  yield a refusal naming its reason or the next value and its events, and events that replay onto
  a value; replaying a value's events onto an empty value MUST yield an equal value.
- **FR-002**: `markets-domain` MUST model a market the same way, over the lifecycle of spec 002.
- **FR-003**: Neither domain library MAY read a clock, a wallet, a player, a database or a
  network; a time a rule needs is carried by the command. Each MUST depend on `ankka-core` alone.
- **FR-004**: The domains MUST keep every rule specs 002 and 003 state as a domain rule: matching
  (FR-006 to FR-009 of spec 003), positions, complete sets and the pool invariant (FR-010 to
  FR-012), settlement arithmetic including void, and the market lifecycle and resolution rules.
- **FR-005**: `exchange-components` and `markets-components` MUST supply the entities, views and
  timers of specs 002 and 003 written against the domains, with the outside world behind a ledger
  port, a gate port and a resolution port the consumer implements.
- **FR-006**: The components MUST register nothing by themselves; the consumer registers each
  explicitly.
- **FR-007**: A ledger port refusal MUST leave the book unchanged and the order resting nowhere; a
  gate port refusal MUST reserve nothing and carry the gate's reason to the caller.
- **FR-008**: Every ledger port call MUST be keyed (order id, trade id, market id and position) so
  that a repeat is answered as the first was, and settlement MUST be at least once.
- **FR-009**: The libraries MUST own their events, wire names and manifests, and every release
  MUST keep a journal written by an earlier release replaying to equal values; a release that
  cannot MUST be a new major version with a named migration.
- **FR-010**: Replay of an event whose manifest is unknown MUST fail naming the manifest, never
  skip it.
- **FR-011**: The living features under `features/exchange/` and `features/markets/` that name no
  gate, wallet, fact or reasoning MUST pass against the domain values alone; those that do MUST
  pass against the components with scripted ports.
- **FR-012**: The components' ports MUST carry no casino vocabulary: a gate answers allow or a
  refusal with a reason; a ledger answers done, insufficient or a refusal with a reason.
- **FR-013**: Only a `v*` tag publishes; the four artefacts go to Maven Central under the tag's
  version, never by hand (amended by D2, 2026-10-22, from the private registry D1 named).
- **FR-014**: Spec 001 and the orchestration requirements of specs 002 and 003 (gating,
  reservation, facts, catalogue, the reasoning mirror, price observations, erasure) are eitheror's,
  carried by the predictions module's spec in ankka-modules, which cites this library's version.

### Key Entities

- **Book**: the value of one outcome's book: resting orders by price and time, positions, the
  pool, the last time.
- **Market**: the value of one market: its definition, state, outcomes, times, resolution.
- **Ledger port**: reserve, release, settle, pay; keyed; answers done, insufficient, or a refusal.
- **Gate port**: may this player place this order; answers allow or a refusal with its reason.
- **Resolution port**: may this caller resolve or void this market.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every domain scenario in `features/exchange/` and `features/markets/` passes against
  the values alone, with no runtime started.
- **SC-002**: A command on a book of 10,000 resting orders is answered by the domain in under
  1 ms at the median on a laptop; the entity wrapping it keeps spec 003's 10 ms median.
- **SC-003**: A fixture journal from every released version replays on the current release in CI,
  on every release.
- **SC-004**: eitheror's predictions module adds no matching, position, settlement or lifecycle
  rule of its own: its code is endpoints, facts, the mirror and the three ports.

## Assumptions

- The release identity is the family's, the one ankka-contrib publishes with.
- The library's version pins ankka-core at the family's current release; ankka-modules pins the
  same ankka.
- The pool module of ankka-modules implements the ledger port with clearing accounts; nothing in
  this library knows a brand.
- The glossary stays one, here; eitheror's module cites its terms rather than redefining them.

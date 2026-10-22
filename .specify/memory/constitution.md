# ankka-predictions Constitution

A prediction market on ankka: an exchange under a product about the future, beside a reasoning
graph. These are the rules every spec, plan and implementation in this repository is held to.

## Core Principles

### I. Money is exact and the wallet is the ledger
Every amount is a whole number of minor units (a `Long`) of one ISO 4217 currency, never a
decimal. A share pays one unit or nothing; a price is a whole number of minor units; an order's
cost is quantity times price, exactly. The `wallet` is the one ledger: `exchange` keeps positions
and never money, and no other service keeps a balance. Every money command is idempotent under a
key the caller supplies (an order id, a trade id, a payment id, a request id), and a repeat
returns the first outcome. Nothing is converted between currencies.

### II. The book is deterministic and replayable
A book matches by price and then by time, inside one value per market that holds every outcome's
book, the positions and the pool, so that two replays of the same commands produce the same
trades and the pool invariant is exact (amended from one entity per outcome by spec 005, because
complete sets and the two-outcome match span outcomes). Money is reserved before an order reaches
the book and moved after a trade is recorded; the book never waits on the ledger. A rule the book
applies is written in its feature file before it is written in code.

### III. The casino's services are reused, never forked
Players, KYC, responsible gambling, payouts and payments are ankka-casino's, consumed by their
images and their documented routes and facts. This repository depends on none of the casino's
code. Where the casino's model does not fit, this product either honours the casino's contract
from a service of its own (the `wallet` honours the routes `merchant` and `payouts` call) or asks
ankka-casino for the change in `notes/casino-requests.md`. The same holds for ankka-reasoning and
ankka: what is missing is a request there, not a workaround here.

### IV. Belief before price
A player's forecast is recorded before the market's prices are shown to them, and the record says
so. Nothing in the product reorders, ranks or nudges toward an outcome. The crowd's probability is
shown with its count and only above a minimum, and is never called public opinion.

### V. Provenance over plausible prose
A market's resolution names its evidence and its authority. Every explanation the product gives of
a price, a move or a disagreement is read from the reasoning graph and traces to records there, or
the product abstains. The reasoning graph is ankka-reasoning's; the product writes its markets,
prices and resolutions there and does not keep a graph of its own.

### VI. Commands over HTTP, facts over topics
A command that needs an answer in the request path is an HTTP call to the owning service, within
the project by identity, across projects by grant. A fact is a message on a topic, versioned, and a
reader builds its own view and never reads another service's store. A gate (`may-bet`,
`may-deposit`, the play gate) that cannot be reached refuses.

### VII. Personal data is marked and erased with the player
A player's forecasts, orders, positions and transactions are theirs. Every field that identifies a
person is marked personal under the player's subject, and erasure is filed by the casino's
`players` service and honoured here. Regulatory retention is a hold on erasure, never an unmarked
field. Nothing personal is written to the reasoning graph.

### VIII. Scala by default, with a reason for anything else
The money path, the book and anything that must be atomic is Scala. An interface or an adapter
that mostly speaks to an outside API may be TypeScript when the spec gives the reason. Every service
owns its database and shares nothing with another.

### IX. Specs in the domain's words
A spec describes what a player, an operator, the casino's services or the reasoning graph observe,
in the glossary's words. Acceptance scenarios live once, in the living features, and the spec names
them. A requirement is testable or it is not a requirement. Every repository page stands alone and
tells no history; the honest limitations page is kept as features land.

## Scope and dependencies

- The platform is ankka at the version `build.sbt` pins; features the product needs and ankka
  lacks are requests in `notes/ankka-requests.md` and then specs in ankka.
- ankka-reasoning is pinned at a released version; what it lacks is a request in
  `notes/reasoning-requests.md`.
- ankka-casino's services are pinned by image tag once they are released; until then by the specs
  they are built from.

## Workflow

- `/speckit-specify` → `/speckit-bdd-features` → `/speckit-clarify` → `/speckit-plan` →
  `/speckit-tasks` → `/speckit-implement`, in that order, one feature per branch named after its
  spec directory.
- `just check` (the feature checker) must pass before a spec is planned.
- A plan names the scenarios it will hold the service to and the integration tests it leaves behind.

## Governance

This constitution outranks any other practice in the repository. An amendment is a pull request
that states what changes and why, and every open spec is checked against it before merge. Where a
spec and the constitution disagree, the spec is wrong until the constitution is amended.

**Version**: 1.1.0 | **Ratified**: 2026-10-20 | **Last Amended**: 2026-10-21

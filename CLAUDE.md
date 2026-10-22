# ankka-predictions — agent context

A prediction market on ankka: a limit order book per outcome, settled in real money through the
payments stack eitheror's casino uses, beside a reasoning graph held in ankka-reasoning, under an
interface that asks what a person thinks before it shows them a price. The family context is loaded
from the parent directory's `CLAUDE.md`; this file adds what is specific to this repository.

## Read first

- `docs/decisions.md`: D1, 2026-10-21: this repository is two libraries, markets and exchange,
  that eitheror's predictions module composes; D2, 2026-10-22: the repository is public and the
  module private. Read them before the pages below, which predate them.
- `docs/product.md`: what is being built, the decisions taken and the assumptions still open.
- `docs/operating-model.md`: the hub-and-spoke proposal: brands as projects on spokes, modules
  enabled from one hub by replicated entities, and the liquidity pool shared by brands on one
  installation. The diagram is `docs/assets/diagrams/hub-and-spoke.svg`.
- `docs/architecture.md`: the two projects, the services reused from ankka-modules and the family,
  the services built here, the money model and the seams.
- `.specify/memory/constitution.md`: the rules every spec here is held to.
- `GLOSSARY.md` and `features/`: the domain's words and what the system does now.
- `prediction-market-reasoning-graph-and-interface.md`: the origin document. Where it and a spec
  disagree, the spec wins.
- `notes/`: what this product asks of ankka-modules, ankka-reasoning and ankka.

## The siblings

- ankka-modules: `~/Documents/projects/eitheror/ankka-modules`, not a sibling of this checkout and
  owned by eitheror. Its `specs/` are the contracts of the services reused here (001 layout and
  grants, 002 the wallet routes the `wallet` here honours, 007 payouts, 008 players, 009 kyc, 010
  responsible gambling, 004 to 006 payments). Read a spec there before writing a seam here; never
  depend on its code.
- ankka-reasoning: `../ankka-reasoning`, pinned at 0.2.0 (to be cut from `main`). Its
  `docs/reference/http-api.md` is the contract; its market layer is what `markets` and `exchange`
  write.
- ankka: `../ankka`, pinned in `build.sbt`. Its `CLAUDE.md` rules hold here: handlers return
  effects, wire names are protocol, registration is explicit, events evolve additively, every
  endpoint declares an ACL.

## Specs

| Spec | Status | What |
|---|---|---|
| 001 | superseded | projects and the ledger: eitheror's now, ankka-modules specs 021 and 022 |
| 002 | superseded | the `markets` service: the lifecycle restated in 006; the catalogue, facts and mirror are eitheror's (ankka-modules 023) |
| 003 | superseded | the `exchange` service: the book restated in 005; the gates, reservation, facts and stream are eitheror's (ankka-modules 022, 023) |
| 004 | draft | the libraries: the split, the ports, the journal as a contract, publishing by tag |
| 005 | draft | the exchange domain: the value of one market's trading, its commands, events and rules |
| 006 | draft | the markets domain: the value of one market from draft to final, its commands and events |

The checker reads specs from 004 (`specs-from` in `.specify/extensions/bdd/bdd-config.yml`);
001 to 003 are kept as written. The product's later specs as first listed here (forecasts, web,
evidence, ask, curator, notifications, reporting and compliance) are eitheror's and are not
numbered here.

## Working here

- Spec-kit with the bdd extension: `/speckit-specify` writes a spec, `/speckit-bdd-features` moves
  its scenarios into `features/`, `just check` runs the checker (needs `uv`).
- One sbt build; services under `modules/<name>/`, the pure domains under `modules/<name>-domain/`
  depending on `ankka-core` alone (as voice does). The template's `Item` stub at the root is the
  shape to copy and then remove.
- Build with the `ankka@ankka` skills; check a running service with `ankka-inspect`.
- Scala by default; British spelling; money in minor units as `Long`; every money command keyed.
- Conventions of the family (`../ankka-brain/conventions.md`): scalafmt hook, serialised tests,
  thin Justfile, only a tag publishes, docs pages tell no history.

## Traps

Written the day they cost time; newest first.

- 2026-10-20: `ankka init` refuses a directory that is not empty. To make an existing directory a
  project's root, scaffold it beside and merge the tree in; nothing in the scaffold collides with
  spec-kit's files.
- 2026-10-20: spec-kit 0.14 does not render `.specify/extensions/bdd/bdd-config.yml`; copy it from
  `config-template.yml` beside it, as voice did.

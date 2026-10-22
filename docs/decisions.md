# Decisions

The in-repo log. Cross-repo decisions are in the family's brains: the private brain's 0004
(ankka-cloud becomes ankka-vendor; modules an optional capability of it) and eitheror's 0055
(ankka-casino becomes ankka-modules; eitheror owns the prediction market). Numbered D1, D2, as
ankka-deployments numbers its own.

## D1 — This repository is two private libraries, not a product (2026-10-21)

**Context.** The product was specified here as two projects on one installation reusing the
casino's services. Working through a shareable wallet and the same players across casino and
market made the prediction market a module eitheror enables per brand, which eitheror owns.
What is left that is neither eitheror's nor gambling's is the rules of a book over outcomes that
pay one unit and the lifecycle of a market.

**Decision.** This repository holds `markets` and `exchange`: each a pure domain on `ankka-core`
alone and a components artefact with ledger, gate and resolution ports. eitheror's predictions
module in ankka-modules composes them and writes endpoints, facts, the reasoning mirror and the
ports. Published privately by tag, never to Maven Central. Spec 001 and the orchestration halves of
specs 002 and 003 move to ankka-modules; spec 004 is the split.

**Alternatives.** Keep the product here and consume the casino's services: lost because the same
players and wallet were wanted, and eitheror's code may not leave eitheror's repositories. Put
the market lifecycle in ankka-reasoning's market layer: lost because that layer is an observation
venue and an example, not a trading lifecycle, and reasoning stays product-neutral.

**Consequences.** The libraries own a persisted protocol: events, wire names and manifests evolve
additively and every release replays earlier journals. The glossary stays one, here. The product
pages `docs/product.md` and `docs/architecture.md` describe the earlier shape until rewritten.
`features/money/` and `features/projects/` stay until ankka-modules' amending spec carries them.

## D2 — The repository is public; the orchestration is eitheror's and private (2026-10-22)

**Context.** D1 made this repository two libraries that depend on `ankka-core` alone, and said
they were published privately. Nothing in a book over outcomes that pay one unit, or in a market's
lifecycle, is eitheror's or gambling's; what is eitheror's is the module that composes them in
ankka-modules, with its gates, wallet, facts and brands.

**Decision.** The repository is a public thinkmorestupidless repository under the Apache 2.0
licence, like ankka and ankka-contrib, and a `v*` tag publishes the four artefacts to Maven Central
under `com.thinkmorestupidless`. eitheror's predictions module stays private in ankka-modules.
Spec 004's story 5, FR-013 and its registry assumption are amended to say so.

**Until the repository can be public.** It was created private on 2026-10-22 and is made public
once nothing in it carries a private repository's content (the family rule, `ankka-brain/family.md`):
the casino's wallet route paths in `docs/architecture.md`; the superseded specs 001 to 003 and
`features/money/` and `features/projects/`, which restate ankka-modules' contracts and were kept
only until ankka-modules spec 022 carries them; and `docs/operating-model.md`, to be read once
more for the vendor's design. Removing them is the first change after this one.

**Alternatives.** Keep the libraries private and publish to the family's registry: lost because a
private registry costs every consumer a credential for code that has no reason to be hidden, and
the family's rule is public unless something installation-specific or a customer's is inside.

**Consequences.** `GLOSSARY.md` and `features/` are public and name no brand, player or
installation. Requests to eitheror in `notes/casino-requests.md` stay: they are this repository's
asks, not ankka-modules' content. The brain maps the repository in `ankka-brain`, the public half.

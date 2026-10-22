# Operating model

*As of 2026-10-21. The shape the conversation about wallets, brands and modules arrived at:
prediction markets as modules of the casino product, every brand operated from one hub, and a
liquidity pool shared by brands on one installation. This page is a proposal; nothing here runs,
and `architecture.md` still describes the earlier two-project shape until the casino's specs
carry the module model.*

## The picture

![One hub cluster and three spoke clusters. The hub is eitheror's own ankka installation: its gateway, control plane and operator, and the realm every spoke's installation trusts, where eitheror staff sign in. On it runs one project, casino-control-plane, with four services: console, eitheror's backoffice over every installation, brand and module, whose writes go to the brand that owns the entity; entitlements, the home of the entitlement entities that say which modules a brand runs, which pool it joins and what it is billed; releases, the home of the release entities that carry module versions and the channel each brand follows; and mirrors, follower copies of every brand's module entities, read only, with any write forwarded to the brand. Beside them, fleet and billing views rebuilt from the mirrored journals and the hub's own, holding nothing personal because players never leave their brand, and the project's Postgres with the hub-homed entities and the mirrored copies. An eitheror admin reaches the console through the hub's gateway. Each spoke cluster has its own ankka installation, trusting the hub realm, and a replication link dialled outward from the spoke: module entities go up to the hub, entitlement and release entities come down. The first spoke is a shared cluster with one installation hosting two brands and a pool. Each brand is one project: a core of wallet, players, kyc, responsible-gambling, payouts and modules, where modules homes the brand's module entity and deploys the enabled modules through the local control plane; then the enabled modules, here casino (game providers, rounds and bets) and predictions (forecasts and web in the brand, markets and exchange from the pool). Each of these two brands' wallets holds one clearing account, a non-player account owned by the pool and the pool's counterparty in that wallet. The pool, pool-north, is a third project on the same installation: markets, one catalogue and one price for every member; exchange, one book per outcome, where an order carries its brand and player; clearing, where every move is local, between a player and their own brand's clearing account, the pool keeps a net per brand and the operator settles the net; gates, which asks may-bet and kyc of the player's brand over grants; and membership, an entitlement at the hub, limited to brands on the same installation. Arrows from clearing reach the clearing account in each member brand's wallet. A brand operator reaches their own brand's backoffice, and players their brand's app, through the shared installation's gateway. The second spoke is a dedicated cluster for one licensee, brand acme, with the same core and only the casino module: no predictions, because its entitlement does not name it. The third spoke is a dedicated cluster for brand nord, with the core, casino, and predictions in the project with its own book and no pool. Outside, the deployments repository ankka-modules-deployments holds each cluster, its ankka version and the spoke template; Flux on each cluster pulls it; it carries platform versions only, never a module.](assets/diagrams/hub-and-spoke.svg)

## What each part is

| Part | Is | Lives |
|---|---|---|
| hub | eitheror's own ankka installation; the realm every spoke trusts | one cluster |
| `casino-control-plane` | an ankka project on the hub: `console`, `entitlements`, `releases`, `mirrors`, the fleet and billing views | the hub |
| spoke | an ankka installation whose realm trusts the hub's, on a cluster of its own or one it shares | any cluster |
| brand | one ankka project: the core, then the modules its entitlement names | one spoke |
| core | `wallet`, `players`, `kyc`, `responsible-gambling`, `payouts`, `modules`: what every brand runs | the brand's project |
| module | a set of services a brand may run: `casino`, `predictions`, later others | the brand's project, or a pool's |
| pool | an ankka project holding one `markets` and one `exchange` for several brands | the same installation as its members |
| `ankka-modules-deployments` | a git repository: each cluster, its ankka version, the spoke template; Flux pulls it | outside |

Three entities are replicated (ankka spec 051, the paid half in `ankka-replication`):

| Entity | Home | Followers | Carries |
|---|---|---|---|
| entitlement | hub | the brand it names | the modules a brand may run, the pool it joins, billing terms |
| release | hub | every spoke | a module's versions and channels |
| module | the brand | hub | which modules are enabled, at which version, and their deploy status |

A write to a follower is forwarded to the home, where the home's permissions decide it. So the
console at the hub enabling a module writes to the brand's `modules` service, as a hub principal
the brand has granted; the brand's `modules` applies the descriptors through the local control
plane and the result flows back up as events on the module entity. Players, wallets and every
personal field stay in the brand: the hub holds nothing it would have to erase.

## Which brand lives where

A brand is a project, so the choice of cluster is a choice of installation:

- **A shared cluster** hosts one installation and several brand projects. The brands are isolated
  as ankka projects are: their own namespace, database, realm, grants. They share a control plane,
  a gateway and a Postgres cluster. This is the cheap default for small licensees and the only
  place a pool can exist.
- **A dedicated cluster** hosts one installation for one licensee, who may run one brand or
  several. Chosen when the licence, the regulator or the contract demands the separation, or when
  the load does.

Both are spokes of the same hub, from the same template in the deployments repository. Moving a
brand between them is a migration of its journals, not a redeploy; spec 051's handover is the
mechanism, and it is a later concern.

## Pooled liquidity

Two brands on one installation want deeper books than either has alone. A pool is one project,
say `pool-north`, holding the `markets` and `exchange` services of specs 002 and 003 plus a
`clearing` service, and every member brand's `predictions` module points at it instead of running
its own. The books, the catalogue, the prices and the price observations are one for all members.

### The problem it has to solve

Every brand's wallet is a separate ledger, and the constitution forbids a move that is not exact
and local to one ledger. A trade between a `bolt` player and a `cato` player cannot be one atomic
move from the buyer's reservation to the seller's available money: they are in different wallets,
in different projects, and nothing may hold both.

### The clearing account

The pool owns one non-player account in each member brand's wallet, the brand's **clearing
account**. It is the pool's counterparty inside that ledger, and the only account the pool may
touch there. With it every money move the pool makes is local to one wallet:

| Event | In the buyer's brand wallet | In the seller's brand wallet |
|---|---|---|
| order placed | reserve against the player's available money | |
| trade | player's reservation to the clearing account, keyed by trade id | clearing account to the player's available money, keyed by trade id |
| split a complete set | player's available money to the clearing account | |
| merge a complete set | clearing account to the player's available money | |
| final resolution | | clearing account to each winning player, keyed by market and position |
| void | clearing account pays each position its equal part | |

Each row is a keyed, at-least-once command on one wallet, exactly as spec 001 defines them; the
wallet does not know whether its caller is the brand's own exchange or a pool's. The market pool
of spec 003 (one unit per complete set) becomes a number the pool's `clearing` service keeps, not
a single account: the money that backs it sits in the clearing accounts, split across brands by
whoever paid for the sets.

### The net, and who settles it

After a day of trading, `bolt`'s clearing account may hold more than the pool owes `bolt`'s
players, and `cato`'s less. The `clearing` service keeps, per brand, two numbers: what the
clearing account holds, and what the pool owes that brand's players (their reservations in flight,
their positions at one unit per share, and payouts due). The difference is the brand's **net**.
The sum of every brand's net is zero, which is the pool invariant of spec 003 restated across
ledgers, and a fact the checker can hold a scenario to.

A clearing account is allowed below zero up to a **credit line** the entitlement sets, because a
trade must not fail for want of float in the seller's brand. The operator settles the net on a
schedule: real money moves between the licensees' bank accounts, and a keyed, signed adjustment on
each clearing account records it. If a brand's net would breach its credit line, the pool refuses
new orders from that brand's players and says why; resting orders stand.

### Gates, personal data and facts

- **Gates.** Before accepting an order the pool asks the player's own brand: `may-bet` of its
  `responsible-gambling`, the play gate of its `kyc`. Each member brand grants the pool's
  `exchange` those routes and its wallet's reserve, release, settle and pay; the pool grants each
  member's `players` the erasure route. Grants are installation-scoped (ankka spec 040), which
  is one reason a pool cannot cross installations.
- **Personal data.** An order and a position carry a brand and a player id, marked personal as
  the constitution requires. Erasure from a member brand reaches the pool through the grant above
  and marks those fields; amounts, ids and timestamps stay.
- **Facts.** The pool publishes `exchange.trades`, `exchange.orders` and `exchange.prices` keyed
  by brand and player. A brand's reporting consumes its own key range. Letting a project read
  another project's topic is not something ankka does today; it is in `notes/ankka-requests.md`.
- **Membership.** An entitlement at the hub names the pool a brand joins. Joining creates the
  clearing account and the grants and switches the brand's `predictions` module to the pool's
  routes. Leaving is clean only once the brand's players hold no position in an open market; the
  entitlement's end date is therefore the close of the last market the brand's players may enter,
  and resting orders are cancelled at that date.
- **Non-partisan.** Every member sees the same book and the same price; the pool reorders
  nothing by brand.

### What a pool is not

- Not cross-installation. Two installations would need ankka spec 050 or an HTTP path through two
  gateways, and the grants and mutual TLS that make the local path trustworthy do not exist
  there. A licensee who wants a pool across its own dedicated clusters wants one cluster.
- Not cross-licensee by default. Pooling liquidity is money moving between operators; the
  regulator and the contract decide whether two licensees may, and the credit line is where that
  decision becomes a number. The first pool is one licensee's brands.
- Not a market maker. The pool deepens the book with more participants; it adds no inventory.

## Open questions

- Who the home sees as the caller of a forwarded replicated write: the hub principal, attested,
  or the follower service. Spec 051 does not say yet.
- Whether the market pool's backing money should be moved to one brand's clearing account at
  split time, so every market has one backing ledger, or left where it was paid. Left where paid
  is simpler and makes the net move; moved is simpler to audit.
- The settlement schedule and the bank-side process. Daily is the assumption.
- Fees: the pool's commission is the pool operator's; a brand's share comes out of the net. A
  later spec.

# Architecture

*As of 2026-10-20. What runs where, what is reused, what is new, and the seams between them.
Each service's rules are in its spec; this page is the map.*

*The shape below predates the decision to make prediction markets modules of the casino product
run from one hub; `operating-model.md` has that picture and the liquidity pool. This page is
kept until the casino's specs carry the module model.*

## Two projects on one installation

| Project | Holds | Comes from |
|---|---|---|
| `predictions` | the product: the ledger, markets, the exchange, forecasts, the interface, the reasoning graph, and the casino's player-facing services | this repository, ankka-modules's images, ankka-reasoning's image, ankka-contrib's sink image |
| `payments` | the casino's shared payments project: `merchant`, `psp-gateway`, `ingest` | ankka-modules's manifests and images, unchanged |

To `payments`, the `predictions` project is a merchant with a brand slug, exactly as a casino brand
is. It deposits by opening cashier sessions on `ingest` under its merchant key, is credited by
`merchant` on a granted route of its `wallet`, and pays out through `psp-gateway` under the grant
`payments` makes to its `payouts`. The five grants a casino brand makes and the two it receives
(ankka-modules spec 001, `contracts/grants.md`) are made here as far as the services exist: the two
to `merchant`, the one to `psp-gateway`, and the two received. The affiliate and audit grants are
not made until those products are deployed.

Identity is the casino's model: a Keycloak realm `casino-predictions` for players, whose
authenticators ask `players` whether an account may sign in, and a staff realm for operators. A
service call carries the caller's platform identity and no token.

## Services of `predictions`

### Reused from ankka-modules, unchanged

| Service | Does here | Spec there | Built there? |
|---|---|---|---|
| `players` | registration, sign-in, player sessions, profile, status, consents, erasure filing; `casino.players` and `casino.sessions` facts | 008 | no |
| `kyc` | identity review under a licence profile; the deposit gate and the play gate | 009 | no |
| `responsible-gambling` | limits, breaks, exclusions; `may-deposit`, `may-bet`, `may-log-in`, `may-play` | 010 | no |
| `payouts` | a withdrawal from request to money leaving, against this product's `wallet` holds and `psp-gateway` | 007 | no |
| `engagement`, `reporting`, `audit` | later: sends, reporting views, the audit store | 014, 015, 018 | no |

Only the casino's `contract` library, `wallet`, `game-provider`, `merchant` and the ST8 simulator are
built as of 2026-10-20. This product's launch waits on `players`, `kyc`, `responsible-gambling`,
`payouts`, `psp-gateway` and `ingest`. Each is consumed by its image and its documented routes and
facts; this repository depends on none of the casino's code. See `notes/casino-requests.md`.

### New, in this repository

| Service | Does | Spec |
|---|---|---|
| `wallet` | the ledger: one account per player per currency, available and reserved money, deposits credited on the casino's granted route after `may-deposit`, holds for `payouts`, reservations against orders, settlement of trades, payment of positions at resolution; `money.transactions` facts | 001 |
| `markets` | the catalogue and the lifecycle of a market: draft, approved, open, closed, resolved or voided; outcomes, close, resolution criteria, authority, category; mirrored into reasoning as a question, its hypotheses and a market; `markets.events` facts | 002 |
| `exchange` | one order book per outcome; orders, matching by price and time, partial fills, cancels, expiry at close; positions; complete sets split and merged; prices; settlement at resolution; `exchange.orders`, `exchange.trades`, `exchange.prices` facts; price observations into reasoning | 003 |
| `forecasts` | a player's forecast and confidence before the reveal; the crowd probability; the reveal; a forecaster's record, Brier score and calibration | 004 |
| `web` | the interface, a web-hosted ankka service: Discover, Trade, the account; mounts the product's services | 005 |
| `evidence` | sources ingested, evidence recorded, claims stated and beliefs revised by agents, as blueprints writing to reasoning | 006 |
| `ask` | the conversational interface over markets, the graph and the player's record | 007 |
| `curator` | the agent that proposes markets from evidence, for an operator to approve | 008 |
| `notifications` | ambient push: a market moved, a forecast looks wrong, a question is due | 009 |

### Reused from the ankka family

| Service | Does | Pinned |
|---|---|---|
| `reasoning` | ankka-reasoning: questions, hypotheses, holders, sources, evidence, claims, belief revisions, markets, resolutions, as-of answers, the process layer | 0.2.0, to be cut from `main` before building |
| `graph-sink` and Neo4j | ankka-contrib's sink, filling the graph the answers read | ankka-contrib 0.1.0 image |

## Money

Every amount is a whole number of minor units (`Long`) of an ISO 4217 currency; a decimal at any
boundary is refused naming the amount. A market has one currency; an order and a position carry it.
Every money command is keyed by the caller and a repeat is answered as the first was. The `wallet`
is the one ledger: `exchange` keeps positions, never money.

A **share** of an outcome pays **one unit** of the market's currency (100 minor units of GBP) if the
market resolves to that outcome, and nothing otherwise. A **price** is a whole number of minor
units from 1 to 99 per share. An order's cost is quantity times price, exactly. The interface turns
a stake into a quantity (five pounds at 58 pence is 8 shares, 4.64 reserved); nothing fractional
exists in the ledger. A **complete set** is one share of every outcome of a market, bought from the
market for one unit (**split**) and sold back for one unit (**merge**), which makes every book
liquid against the pool.

## The seams

**`merchant` to `wallet`.** `POST /v1/casino/brands/{brand}/wallets/{player}/{currency}/deposits`
with `paymentId` as the key, `amount`, `terminal`, `psp`, `scheme`, `paymentProvider`; and
`POST /v1/casino/brands/{brand}/payments/{paymentId}/outcome`. The `wallet` asks
`responsible-gambling` `may-deposit` with the player's deposit sums before crediting, and answers
the casino's shape (`EXCLUDED`, `COOLING_OFF`, `OVER_LIMIT`, `RG_UNAVAILABLE`, or balances and
`txnId`). This is the casino wallet's granted-route contract (ankka-modules spec 002
`contracts/routes.md`), kept so that `merchant` sees no difference.

**`payouts` to `wallet`.** Hold preview, hold, release, confirm, reverse, keyed by the request id.
This product has no bonus money, so the preview's bonus and clawback fields are zero and no hold
ever needs the player's confirmation.

**`exchange` to `wallet`.** Reserve (key: the order id), release, and settle (key: the trade id,
moving reserved money from the buyer to the seller's available money), and pay (key: the market
and the position, at resolution). All within the project, by identity.

**`exchange` to `responsible-gambling` and `kyc`.** `may-bet` with the order's cost before a buy is
accepted; the play gate before a player's first order in a session. A gate that cannot be reached
refuses the order (the casino's rule, FR-011 of its spec 010).

**`markets` and `exchange` to `reasoning`.** On approval: a question, one hypothesis per outcome,
and a market naming them, the venue, the resolution criteria and the close. On trades: a price
observation per outcome when the last price moves, at most once a minute. On resolution: the
evidence and a resolution. The product writes as `service:predictions/markets` and
`service:predictions/exchange`; reasoning registers the market holder `market.<id>`.

**`players` facts to everything.** `exchange`, `forecasts` and `web` keep their own views of
`casino.players` (status, KYC state) and never read another service's store.

## Reuse decisions

| Thing | Where it lives | Why |
|---|---|---|
| The order book, matching and positions | `modules/exchange-domain` here, depending on `ankka-core` alone, with the service in `modules/exchange` | Generic; eitheror could want a betting exchange on it. Kept free of the product so it can move to its own repository when a second consumer appears |
| The ledger with reservations | `wallet` here | Bound to the casino's seams and to this product's gates; generic enough to copy, not to share yet |
| Scoring and calibration | `forecasts` here, for now | ankka-reasoning names scoring as its next feature; moved there when it exists (`notes/reasoning-requests.md`) |
| Assumptions and "what would change your mind" | a layer in ankka-reasoning, not here | The layer rules there allow it; the product only reads the answers |
| Evidence ingestion | `evidence` here | Built for this product, from the family's ingestion examples; reusable parts go to ankka-contrib when they are seen to be reusable |
| Players, KYC, responsible gambling, payments, payouts | ankka-modules | Theirs; what does not fit is asked for, never forked |

## Runtime shape

Each new service is one ankka service in Scala with its own database, as the template makes it.
`exchange` is the hot path: a book is one event sourced entity per outcome of a market, matching
inside the entity so that fills are deterministic and replayable; money moves by a consumer of
`exchange.trades` calling `wallet`, at least once, keyed by trade id, with the buyer's reservation
guaranteeing solvency before the order reached the book. Market data (last price, best prices,
depth, candles) are views; the interface reads them and streams changes.

Locally: `docker compose` with Postgres per service, Kafka, Neo4j, the reasoning and sink images,
and the casino's images under their own profiles; `just up`, `just run`, `just web`. On an
installation: the two projects' manifests, applied with the ankka CLI.

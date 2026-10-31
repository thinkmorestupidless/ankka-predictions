# ankka-predictions: the product

*Superseded in part on 2026-10-21: the product is eitheror's, a module of ankka-modules, and this
repository is the markets and exchange libraries it composes (`decisions.md`, spec 004). The rest
of this page describes the product as it was first framed.*

*As of 2026-10-20. The origin document is `prediction-market-reasoning-graph-and-interface.md`;
this page restates what of it is being built, in what order, and the decisions taken since.*

## What it is

A prediction market whose first question to a person is not "what is the price" but "what do you
think will happen". Underneath is a real exchange: a limit order book for every outcome of every
market, matched by price and time, settled in real money through the payments stack that eitheror's
casino uses. Beside it is a reasoning graph, held in ankka-reasoning, that keeps why the market
believes what it believes: the evidence, the claims, the competing hypotheses, the agents' and the
crowd's beliefs, and the record of what changed when.

The origin document's framing stands: a product about the future that happens to contain a
prediction market. The market gives beliefs a number; the reasoning graph gives them an
explanation; the interface asks for a belief before it shows a price.

## Decisions taken on 2026-10-20

| Decision | Choice | Why |
|---|---|---|
| Market mechanism | A limit order book per outcome, price-time priority, partial fills. No automated market maker at launch | The core of the product is matching both sides as quickly as possible at scale; an AMM is a later liquidity feature, not the engine |
| Money | Real money at launch, in minor units, one currency per market | The casino's payments stack already carries fiat deposits and payouts; nothing is converted |
| Players, KYC, responsible gambling, payouts, payments | ankka-modules's services, deployed as they are into this product's projects | They exist or are specified; rebuilding them is waste. What does not fit is asked of ankka-modules, or built here as a different service |
| The casino's wallet | Not used. A new `wallet` here is the ledger, honouring the routes the casino's payments and payouts services call | The casino's bet is a round with one settlement; an exchange needs reservations against resting orders, fills that move money between two players, and positions paid at resolution |
| The reasoning graph | ankka-reasoning 0.2.0, run beside the product and called as a service, as voice does. Nothing civic or market-specific goes into it | Its records, market layer and as-of answers are what the origin document asks for; what it lacks becomes a feature there, not a fork here |
| Evidence | Later, built for this product, starting from the family's ingestion examples | The exchange comes first; evidence is the second product, after the market works |
| Free forecasts | Kept, as the first interaction, beside the money | The funnel in the origin document starts with a belief; the exchange is what a belief can be backed with |

## Assumptions still to confirm

Each is written into the architecture as a default and can be changed before the feature that
depends on it is planned.

- **Owner and visibility.** Written as a thinkmorestupidless repository that speaks to the casino's
  services only over HTTP against their documented contracts, and depends on none of the casino's
  libraries. Whether the repository is public is undecided; it is written as if it were (no secret,
  no installation value, no real person's data).
- **Jurisdiction.** *Decided 2026-10-31 (Trevor): the first licence is in the United States,
  as a CFTC Designated Contract Market.* The venue's obligations under that licence (emergency
  authority, which is the market's suspend and resume in spec 006; position limits; market
  surveillance; daily publication of trading information; recordkeeping) are the module's in
  ankka-modules spec 023 and its successors, not the libraries'. Until then this page's default
  stood: a UKGC-style licence, every order through the responsible-gambling `may-bet` gate and
  the KYC play gate, deposit limits binding, the licence profile the casino's `kyc` setting;
  which of those gates a DCM keeps is spec 023's to decide.
- **Launch currency.** One, assumed GBP, because the origin document prices in pounds.
- **A void market.** Every share of every outcome is paid an equal share of one unit (fifty pence
  per share in a two-outcome market). Kalshi and Polymarket differ here; to be settled in spec 003.
- **Scoring and calibration.** Computed here from the product's own forecasts and resolutions until
  ankka-reasoning's scoring feature exists; then moved.
- **Session limits.** The casino counts session minutes from game sessions, which this product
  does not open. A player's session limit does not bind until the casino or this product counts
  the product's own sessions.

## What is built, in order

The build order is the spec list in `CLAUDE.md`. In product terms:

1. **The ledger and the projects** (spec 001): a player's money in this product, deposited and
   withdrawn through the casino's payments stack, reserved against orders, moved by trades, paid
   at resolution.
2. **Markets** (spec 002): a question with outcomes, a close, resolution criteria and an authority;
   approved by an operator, opened, closed, resolved on evidence or voided; mirrored into the
   reasoning graph as a question, its hypotheses and a market.
3. **Order books and matching** (spec 003): orders, the book per outcome, matching, fills,
   positions, complete sets, prices, and settlement at resolution.
4. **Forecasts** (spec 004): a belief and a confidence given before the price is shown; the crowd's
   probability; the reveal; a forecaster's record and calibration.
5. **The interface** (spec 005): Discover, the question-first flow; Trade, the book; the account.
6. **Evidence and agent forecasters** (spec 006): sources ingested, claims stated, beliefs revised
   by agents, "why did it move" answered from the graph.
7. **Ask** (spec 007), **market creation** (spec 008), **notifications** (spec 009),
   **reporting and compliance** (spec 010).

## What it is not

- Not an automated market maker. Liquidity at launch is participants' orders and complete-set
  splitting; a market-making service is a later feature.
- Not multi-currency within a market, and nothing converts.
- Not a casino: no rounds, no bonuses, no free spins. The casino's rewards service is not deployed.
- Not a source of truth for beliefs: the reasoning graph is. The product writes its market, its
  prices and its resolutions there and reads its explanations from there.
- Not pseudonymous. A player is a casino player with a verified identity; a forecast is personal
  data and is erased with the player.

## Where to read next

- `docs/architecture.md`: the projects, the services, what is reused and what is new, the seams.
- `docs/limitations.md`: the honest list, kept as features land.
- `GLOSSARY.md` and `features/`: the domain's words and what the system does now.
- `notes/casino-requests.md`, `notes/reasoning-requests.md`, `notes/ankka-requests.md`: what this
  product asks of its siblings.

# What this product asks of ankka-modules

*As of 2026-10-20.* ankka-modules is eitheror's; nothing here changes it. Each request is what
this product needs from the casino's services to deploy them beside its own, in the order the
product needs them. An answer is a change to a casino spec, or a reason it is not.

1. **The `payments` project deployed on its own, with a merchant that is not a casino brand.**
   `merchant`, `psp-gateway` and `ingest` are consumed unchanged. The product's project is a
   merchant with the slug `predictions`. Needed: the manifest tool (`tools/brands`) or its
   equivalent applies the shared project without a casino brand existing, and `merchant`'s
   hierarchy admits a merchant whose brand project holds no `game-provider`.
2. **`players`, `kyc`, `responsible-gambling` and `payouts` deployed in a project that is not a
   casino brand.** Spec 001's layout fixes the twelve brand services (FR-002) and refuses a manifest
   naming any other set (FR-010). Needed: a project that holds a subset of the brand services plus
   services the contract does not list, or a second layout for a non-casino product. The grants the
   product makes and receives are the same five and two, less the affiliate and audit ones until
   those products are deployed.
3. **The wallet's granted-route and hold contracts as a document a replacement must honour.** The
   `wallet` here serves `POST .../wallets/{player}/{currency}/deposits`, `POST
   .../payments/{paymentId}/outcome` and the five hold routes `payouts` calls, with the bodies and
   refusals of spec 002 `contracts/routes.md`. Needed: those routes named as a contract a brand's
   wallet may be replaced behind, and a note when they change.
4. **Session minutes for a product without game sessions.** `responsible-gambling` counts session
   minutes from `casino.sessions` game sessions, which this product never opens, so a session limit
   never binds. Needed: either `players` counts player sessions when a brand has no game provider,
   or a route this product calls to report its own sessions.
5. **`may-bet` with the order's cost, before funding.** The product asks `may-bet` once per buy
   order. Loss limits are unenforced in spec 010 (its open clarification); when they are enforced
   the product will pass the loss sums the wallet keeps.
6. **The KYC play gate without a launch.** The product asks the play gate before a player's first
   order in a session rather than at a game launch. Needed: nothing, if the route stays as spec 009
   FR-020 describes it.
7. **Reporting over the product's facts.** `reporting`'s GGR and day-end fold the casino's
   families only. Not needed at launch; when the product deploys `reporting`, it will need
   families for trades, payouts at resolution and reserved money, or the product will keep its own
   reporting (spec 010 here).
8. **The category list and the currencies a brand offers.** The markets domain (spec 006 here)
   keeps a market's category and currency as text; spec 002 FR-011 (an operator setting for the
   categories, a category in use never removed) and the check that a currency is one the brand
   offers are the predictions module's (ankka-modules spec 023), which does not yet state them.
9. **A trade through the pool, charged from reservations.** In a two-outcome market two buys of
   opposite outcomes trade by splitting a set between them (spec 005 FR-011 here): each buyer's
   charge leaves its reservation for the order and enters the market's pool, and each releases
   what its price did not use. ankka-modules spec 022's split debits available money, not a
   reservation, and its settlement runs from a buyer to a seller. Needed: a split, or a
   settlement, that charges an order's reservation into the pool under the trade's key, one per
   leg. The merge side (two sells paid from the pool) is spec 022's payment as it stands.
10. **Who may change a market or move its close.** The market entity asks the resolution port
    before a draft, an approval, a suspend, a resume, an early close, a resolve, a correction and
    a void, and asks nothing before a `change` or a `move close` (spec 004 as built). An approved
    market's resolution criteria and authority may change until it opens (spec 006 FR-005), so the
    predictions module (ankka-modules spec 023) checks the `markets` permission before it calls
    either (Trevor, 2026-10-31).

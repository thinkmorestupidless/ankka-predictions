# Limitations

The honest list of what this product does not do. Kept as features land; a claim anywhere else
that goes past this page is a defect in the claim.

*As of 2026-10-31: the exchange library runs; the markets library drafts and approves only.*

## What the exchange does

- One market's trading: a limit order book per outcome, orders that rest, fill at once or fill
  whole, matching at the better price across the own book and, in a two-outcome market, the other
  outcome's book through the pool (a split for buys, a merge for sells).
- Complete sets split and merged, suspension and resumption, a close, and a finality that pays
  each position (a losing one is paid 0).
- A maximum order cost and a maximum position per market, fixed when it opens.

## What the exchange does not

- No fees.
- No matching through the pool in a market of more than two outcomes: it matches directly, and
  its complete sets are split and merged explicitly.
- No self-trade prevention beyond skipping an owner's own resting order.

## Not built

- The markets' rules (spec 006): suspend, close, resolve and void of a market.
- No service of the product exists; the libraries are composed by a consumer.
- No wallet, no forecast, no interface.
- No evidence is ingested and no agent forecasts.
- Nothing is deployed, and no installation holds the two projects.

## Depends on what is not built elsewhere

- ankka-modules's `players`, `kyc`, `responsible-gambling`, `payouts`, `psp-gateway` and `ingest`
  are specified and not built.
- ankka-reasoning 0.2.0 is not cut.
- ankka's personal data erasure (spec 042) is a draft.

## Deliberately not

- No automated market maker; liquidity is participants' orders and complete sets.
- No currency conversion; one currency per market.
- No short selling; a sell order needs the shares.
- No fractional shares; a stake becomes a whole number of shares.
- No casino rounds, bonuses or free spins.
- No person is written to the reasoning graph.

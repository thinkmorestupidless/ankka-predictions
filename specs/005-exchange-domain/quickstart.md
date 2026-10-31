# Quickstart: proving the exchange domain

How to see that this feature works, from a checkout. Needs JDK 21 and sbt; `just check` needs
`uv`; the components' suites need Docker.

## Every rule, against the value alone

```bash
sbt exchangeDomain/test
```

Expected: every scenario of `features/exchange/*.feature` (the six files, 65 scenarios and
outline rows) and the four exchange scenarios of `features/libraries/domain.feature` pass; the
market's scenario is reported ignored naming the markets suite; nothing is reported pending. No
Docker container starts. `ExchangeProperties` reports 100,000 sequences
(`-Dpredictions.sequences=1000` for a quick run). `ExchangePerfSuite` prints the medians of a
resting place and a matching place on an exchange of 10,000 resting orders, each under 1 ms.

A check that can fail: in `Matching.scala` make the pool candidate lose ties to the own book
*and* win at a worse price (swap the comparison), and watch "a buy takes the better price of a
resting sell of its own outcome and a buy of the other" go red; put it back.

A second: in `Exchange.scala` stop adding to `restingToBuy` on `OrderPlaced`, and watch "a buy
order that would take the position above the maximum position is refused naming the limit" and
the property suite's invariant 4 go red.

## The components stand on the real domain

```bash
sbt exchangeComponents/test
```

Expected: `features/libraries/components.feature`'s trade scenario passes (the ledger is asked to
settle `t-1`, keyed `t-1`, and a repeat is answered as the first was); `PlacementSuite`'s six tests
pass with the exchange refusing by price where the stub refused as not built; the components'
`ExchangePerfSuite` runs (no longer skipped) and holds 10 ms at the median through the entity;
`JournalCompatibilitySuite` replays `0.0.0`'s three fixtures (`every-event`, `opened`, `traded`).

## The fixtures

```bash
rm -r modules/exchange-components/src/test/resources/journal/0.0.0
sbt "exchangeComponents/Test/runMain com.thinkmorestupidless.predictions.exchange.components.JournalFixtureWriter 0.0.0"
git diff --stat modules/exchange-components/src/test/resources/journal/   # three files
```

Done once in this feature (research R13); after the first tag a fixture is never rewritten.

## The features, the glossary and the specs agree

```bash
just check                                    # 0 findings
```

## What the library does not do

`docs/limitations.md`: no fees, no short selling, no fractional shares, no market maker; the
markets' rules wait on spec 006.

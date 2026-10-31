# Quickstart: proving the markets domain

## The domain alone

```bash
sbt marketsDomain/test
```

Expected: every scenario of `features/markets/` passes, and the market scenario of
`features/libraries/domain.feature`; the exchange's scenarios of that file are reported ignored,
naming `ExchangeDomainFeatures`. `MarketProperties` prints the sequences it ran and the count of
each event; every event kind occurs, `Voided` after `Resolved` among them.

```bash
sbt -Dpredictions.sequences=1000 marketsDomain/test   # a quick run
```

A check that can fail: in `Market.scala` let a correction through after the window (compare with
`isAfter` instead of `isBefore`), and watch "a resolution may not be corrected after the review
window" go red; put it back.

A second: in `Drafting.scala` stop comparing the resolution criteria of an open market, and watch
"an open market's resolution criteria may not be changed" go red; put it back.

## The components stand on the real domain

```bash
sbt marketsComponents/test   # Docker: a throwaway Postgres
```

Expected: green with nothing in `modules/markets-components/src/main` changed. The
`JournalCompatibilitySuite` replays `opened.jsonl`, `every-event.jsonl`, `resolved.jsonl` and
`voided.jsonl` from `src/test/resources/journal/0.0.0/`.

## Everything

```bash
just fmt && sbt test && just check   # 0 findings
```

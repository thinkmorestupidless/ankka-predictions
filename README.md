# ankka-predictions

The two libraries of a prediction market on [ankka](https://github.com/thinkmorestupidless/ankka):
**markets**, one market from its draft to its finality, and **exchange**, one market's trading, a
limit order book per outcome and the pool that settles through it. A consumer (eitheror's
predictions module) composes them with its own money, players and rules behind three ports. Read
`docs/product.md` for what is being built and `docs/decisions.md` for why it is two libraries; the
specs are under `specs/` (`CLAUDE.md` lists them), their scenarios in `features/`, their words in
`GLOSSARY.md`. `just check` holds the three together.

The exchange's rules are in (spec 005): placing, matching through the book and the pool,
cancelling, splitting and merging, suspending, closing and finalising, with the limits fixed when
it opens. The markets' rules arrive with spec 006: until then a market is drafted and approved and
refuses every other command as not built, and the scenarios that need a rule are reported
ignored, naming that spec.

## The libraries

| Artefact (`com.thinkmorestupidless`) | Package | What |
|---|---|---|
| `ankka-predictions-exchange-domain_3` | `…predictions.exchange` | the exchange as a value, its commands, events and refusals, on `ankka-core` alone |
| `ankka-predictions-markets-domain_3` | `…predictions.markets` | the market as a value, likewise |
| `ankka-predictions-exchange-components_3` | `…predictions.exchange.components` | the exchange entity, the placement workflow, settlement, positions and the close timer, on `ankka-sdk` |
| `ankka-predictions-markets-components_3` | `…predictions.markets.components` | the market entity, the catalogue, the market's timer and its schedule |

The journal is a contract: the manifests, the JSON of every event and the component and handler
names are fixed once a release is tagged, and every release replays the journals of the ones
before it (`modules/*-components/src/test/resources/journal/`). `specs/004-libraries/contracts/`
has all of them.

## Using them

Pin the release and the ankka it was built against (`V.ankka` in `project/Dependencies.scala`,
0.14.2):

```scala
libraryDependencies ++= Seq(
  "com.thinkmorestupidless" %% "ankka-predictions-exchange-components" % version,
  "com.thinkmorestupidless" %% "ankka-predictions-markets-components"  % version
)
```

Build the components from your ports and register them in your own service; the libraries
register nothing:

```scala
val timerRuntime = TimerRuntime()
val exchange = ExchangeComponents(ExchangePorts(ledger, gate, ledgerPatience = Duration.ofSeconds(5)))
val markets  = MarketsComponents(MarketPorts(resolution, () => timerRuntime.timerScheduler))
// exchange.descriptors ++ markets.descriptors, with ProjectionRuntime() and timerRuntime
```

- `LedgerPort` holds and moves money: reserve, release, settle, charge, pay and payout, each
  naming the market and keyed so a repeat is answered as the first was.
- `GatePort` says whether an owner may place an order.
- `ResolutionPort` says whether an operator may draft, approve, suspend, close, resolve or void.

An order is placed through the `exchange-placement` workflow, one per order under
`<market>/<order>`: the gate, the reservation of a buy's cost, the exchange, and the release of a
buy the exchange refused. A refusal reaches a caller as a `CommandError` whose message is the
refusal as JSON; `Refused.unapply` reads it back.

## Test

```bash
sbt test        # every suite; the components' start a throwaway Postgres, so Docker is needed
just check      # the features, the glossary and the specs (needs uv)
just fmt        # format as CI checks
```

The components' suites are the example consumer: they build both libraries with scripted ports
and run them in `AnkkaTestKit`.

## Releasing

Only a `v*` tag publishes, to Maven Central (`.github/workflows/release.yml`); the version is the
tag's. A tag is refused unless its journal fixtures are committed: write them with each
components module's `JournalFixtureWriter` first.

# Quickstart: proving the libraries

How to see that this feature works, from a checkout. Needs JDK 21, sbt, Docker (the components'
suites start a Postgres) and `uv` (the features checker).

## The build is four libraries on ankka-core

```bash
sbt projects                                  # exchangeDomain marketsDomain exchangeComponents marketsComponents root
sbt exchangeDomain/makePom && grep -B1 -A3 '<artifactId>ankka-' "$(ls -t modules/exchange-domain/target/scala-3.9.0/*.pom | head -1)"
# ankka-core with no scope (compile) and ankka-testkit in test scope, beside the module's own
# artifactId: nothing of ankka but its core at compile time (V1); likewise marketsDomain
ls src 2>/dev/null || echo "the stub is gone"
```

## The domains hold their rules with no runtime

```bash
sbt exchangeDomain/test                       # features/exchange/*, features/libraries/domain.feature (the exchange scenarios), the property suite
sbt marketsDomain/test                        # features/markets/*, the market scenario of domain.feature, the property suite
```

Expected: every scenario named in specs 005 and 006 passes; no Docker container starts (watch
`docker ps` while they run); the property suites report their sequence count. Until specs 005 and
006 land, their scenarios are reported ignored, each naming the spec that lifts it.

A check that can fail: change one rule (a sell matching the highest resting buy becomes the
lowest) and watch `matching.feature` go red, then put it back.

## The components stand on ports

```bash
sbt exchangeComponents/test marketsComponents/test
```

Expected: `features/libraries/components.feature` passes against scripted ports in `AnkkaTestKit`
(a Postgres starts); the orchestration scenarios of `features/exchange/orders.feature` pass with
the scripted gate and ledger; the "registers none" scenario starts a service with no descriptors
and finds no exchange and no market. `PlacementSuite` holds the ledger's patience and two crash
tests by restart; `MarketsComponentsSuite` sees a market open by its timer. Until spec 005 the
trade scenario is ignored and `ExchangePerfSuite` skips, both saying so.

## The journal is a contract

```bash
sbt 'exchangeComponents/testOnly *JournalCompatibilitySuite' 'marketsComponents/testOnly *JournalCompatibilitySuite'
ls modules/exchange-components/src/test/resources/journal/   # 0.0.0, then one per release
```

Expected: every fixture of every version replays to the recorded value; the `future.v9` manifest
and an unknown `type` fail naming them, and nothing after them is applied.

## The features, the glossary and the specs agree

```bash
just check                                    # 0 findings
```

## Publishing by tag (not run in this feature)

```bash
sbt publishLocal                              # ~/.ivy2/local/com.thinkmorestupidless/ankka-predictions-exchange-domain_3/<dynver>/
```

A `v*` tag runs `.github/workflows/release.yml`. It refuses a tag whose journal fixtures are not
committed (`journal/<version>/` in both components modules), and it needs the four Sonatype and PGP
secrets set on the repository; until they are, the upload fails naming them. Only Trevor tags.

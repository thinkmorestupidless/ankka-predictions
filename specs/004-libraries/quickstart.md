# Quickstart: proving the libraries

How to see that this feature works, from a checkout. Needs JDK 21, sbt, Docker (the components'
suites start a Postgres) and `uv` (the features checker).

## The build is four libraries on ankka-core

```bash
sbt projects                                  # exchangeDomain marketsDomain exchangeComponents marketsComponents root
sbt exchangeDomain/makePom && grep -c '<artifactId>ankka-' modules/exchange-domain/target/scala-3.9.0/*.pom
# 1: ankka-core and nothing else in compile scope (V1); likewise marketsDomain
ls src 2>/dev/null || echo "the stub is gone"
```

## The domains hold their rules with no runtime

```bash
sbt exchangeDomain/test                       # features/exchange/*, features/libraries/domain.feature (the exchange scenarios), the property suite
sbt marketsDomain/test                        # features/markets/*, the market scenario of domain.feature, the property suite
```

Expected: every scenario named in specs 005 and 006 passes; no Docker container starts (watch
`docker ps` while they run); the property suites report their sequence count.

A check that can fail: change one rule (a sell matching the highest resting buy becomes the
lowest) and watch `matching.feature` go red, then put it back.

## The components stand on ports

```bash
sbt exchangeComponents/test marketsComponents/test
```

Expected: `features/libraries/components.feature` passes against scripted ports in `AnkkaTestKit`
(a Postgres starts); the orchestration scenarios of `features/exchange/orders.feature` pass with
the scripted gate and ledger; the "registers none" scenario starts a service with no descriptors
and finds no exchange.

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

A `v*` tag runs `.github/workflows/release.yml`, which needs the four Sonatype and PGP secrets
set on the repository; until they are set the job fails at its first step naming them. Only Trevor
tags.

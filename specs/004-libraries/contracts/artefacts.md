# The artefacts and the protocol

What a consumer pins, imports and journals. Everything in this file is a published contract once
the first tag exists: a change to a name below is a new major version (spec 004 FR-009).

## Artefacts

| sbt project | Artefact (`com.thinkmorestupidless`) | Depends on | Package |
|---|---|---|---|
| `exchangeDomain` | `ankka-predictions-exchange-domain_3` | `ankka-core` | `com.thinkmorestupidless.predictions.exchange` |
| `marketsDomain` | `ankka-predictions-markets-domain_3` | `ankka-core` | `com.thinkmorestupidless.predictions.markets` |
| `exchangeComponents` | `ankka-predictions-exchange-components_3` | `exchange-domain`, `ankka-sdk` | `com.thinkmorestupidless.predictions.exchange.components` |
| `marketsComponents` | `ankka-predictions-markets-components_3` | `markets-domain`, `ankka-sdk` | `com.thinkmorestupidless.predictions.markets.components` |

The version is the tag's, through sbt-dynver; `V.ankka` in `project/Dependencies.scala` pins the
ankka the release is built against, and the consumer pins the same ankka.

## The domain API

```scala
// com.thinkmorestupidless.predictions.exchange
final case class Exchange(...)                      // the value, spec 005 FR-001; Exchange.empty
enum ExchangeCommand { case Open(...); Place(...); Cancel(...); Split(...); Merge(...); Suspend(...); Resume(...); Close(...); Finalise(...) }
enum ExchangeEvent   { case Opened(...); OrderPlaced(...); TradeMade(...); OrderFilled(...); OrderCancelled(...);
                       OrderExpired(...); SetsSplit(...); SetsMerged(...); Suspended(...); Resumed(...); Closed(...); PositionPaid(...);
                       RemainderKept(...); Final(...) }
final case class Refusal(reason: String, names: Map[String, String])
final case class Decision(next: Exchange, events: Vector[ExchangeEvent])
object Exchange:
  val empty: Exchange
  def apply(exchange: Exchange, command: ExchangeCommand): Either[Refusal, Decision]
  def replay(exchange: Exchange, event: ExchangeEvent): Exchange
  given Serializer[Exchange]      = Codecs.serializer[Exchange]("exchange")
  given Serializer[ExchangeEvent] = Codecs.serializer[ExchangeEvent]("exchange-event")
```

```scala
// com.thinkmorestupidless.predictions.markets
final case class Market(...)                        // the value, spec 006 FR-001; Market.empty
enum MarketCommand { case Draft(...); Change(...); Approve(...); Open(...); Suspend(...); Resume(...); Close(...); MoveClose(...);
                     Resolve(...); Correct(...); Void(...); Finalise(...) }
enum MarketEvent   { case Drafted(...); Changed(...); Approved(...); Opened(...); Suspended(...); Resumed(...); Closed(...); CloseMoved(...);
                     Resolved(...); Corrected(...); Voided(...); Final(...) }
object Market:
  val empty: Market
  def apply(market: Market, command: MarketCommand): Either[Refusal, Decision]
  def replay(market: Market, event: MarketEvent): Market
  given Serializer[Market]      = Codecs.serializer[Market]("market")
  given Serializer[MarketEvent] = Codecs.serializer[MarketEvent]("market-event")
```

Every command carries `at: Instant`. A command yields `Left(refusal)` with no change, or
`Right(Decision(next, events))` such that `events.foldLeft(exchange)(replay) == next`. The fields
of each case are specs 005 and 006's and are fixed by their plans, not here.

## Manifests

| Manifest | Type | Module |
|---|---|---|
| `exchange` | `Exchange` (snapshots) | exchange-domain |
| `exchange-event` | `ExchangeEvent` (journal) | exchange-domain |
| `market` | `Market` (snapshots) | markets-domain |
| `market-event` | `MarketEvent` (journal) | markets-domain |

Inside the JSON the case is named by the `type` field (`{"type":"OrderPlaced",...}`). A case is
never renamed or removed; a field is only added, with a default (spec 004 FR-009).

## Component ids and wire names

| Component | Id | Kind | Wire names |
|---|---|---|---|
| the exchange | `exchange` | event sourced entity, one per market | commands `open`, `place`, `cancel`, `split`, `merge`, `suspend`, `resume`, `close`, `finalise`; queries `get`, `prices`, `depth`, `position` |
| placement | `exchange-placement` | workflow, one per order, id `<market>/<order>` | command `place` (the start, the claim); query `outcome`; steps `gate`, `reserve`, `apply`, `release`, `unavailable` |
| positions | `exchange-positions` | view of the exchange's changes | query `by-owner` |
| settlement | `exchange-settlement` | consumer of the exchange's events, calls the ledger | none |
| exchange timers | `exchange-timers` | timed action | `close` |
| the market | `market` | event sourced entity, one per market | commands `draft`, `change`, `approve`, `open`, `suspend`, `resume`, `close`, `move-close`, `resolve`, `correct`, `void`, `finalise`; query `get` |
| catalogue | `market-catalogue` | view of the markets' changes | queries `by-state`, `by-category` |
| market timers | `market-timers` | timed action | `due` |

A wire name is declared apart from the Scala method name and never changes (ankka's rule); a new
behaviour is a new wire name.

## Construction

```scala
val exchange = ExchangeComponents(ExchangePorts(ledger, gate, ledgerPatience = Duration.ofSeconds(5)))
val markets  = MarketsComponents(MarketPorts(resolution))
Ankka.service.registerAll(exchange.descriptors ++ markets.descriptors)   // in the consumer's Main
```

# The exchange domain API, as this spec fixes it

Spec 004's `contracts/artefacts.md` fixes the names: the value, the nine commands, the fourteen
events, the manifests. This page fixes what this spec adds to the API and what each refusal and
event means. It is a published contract with the first tag.

## Functions

```scala
object Exchange:
  val empty: Exchange
  def cost(quantity: Long, price: Long): Long                                 // quantity × price
  def apply(exchange: Exchange, command: ExchangeCommand): Either[Refusal, Decision]
  def replay(exchange: Exchange, event: ExchangeEvent): Exchange             // total over every case

final case class Exchange(...):                                              // spec 004, plus
  def position(owner: OwnerId, outcome: OutcomeId): Position
  def pool: Long                                                             // setsOutstanding × unit + remainder
  def lastPrice(outcome: OutcomeId): Option[Long]
  def bestBid(outcome: OutcomeId): Option[Long]
  def bestAsk(outcome: OutcomeId): Option[Long]
  def depth(outcome: OutcomeId): (Vector[Level], Vector[Level])              // buys best first, sells best first

final case class Level(price: Long, quantity: Long)                          // moved here from the components

final case class Position(quantity: Long = 0, restingUnderSells: Long = 0, cost: Long = 0,
                          restingToBuy: Long = 0)                            // the last field is new, with a default

object Quantity:
  /** A positive whole number as the domain takes it, or the refusal naming the quantity as given. */
  def of(text: String): Either[Refusal, Long]
  def of(n: BigDecimal): Either[Refusal, Long]
```

`ExchangeCommand.Place.quantity` stays a `Long`; `Quantity.of` is for the edge that receives a
number as text or a decimal (the consumer's endpoint, the step definitions) so that "not a
positive whole number" is one rule, refused `quantity`.

`Open` takes its arguments as given and checks none of them. Its outcomes are an approved
market's, which spec 006's markets domain refuses unless there are two to twenty with distinct
names; its unit and limits are the consumer's values. A caller that opens an exchange some other
way owns those checks: two equal outcomes would match a buy against its own book's buys through
the pool. The caller also keeps `maxPosition × unit` within a `Long`: a split's charge, a merge's
payout and a sell's proceeds are bounded by that product and no more.

## Refusals

| `reason` | `names` | When |
|---|---|---|
| `out of order` | `last`, `at` | the command's time is before the value's last event |
| `state` | `state` | the command does not apply in this state (the state's word: `empty`, `open`, `suspended`, `closed`, `final`), or an order is not resting (`filled`, `cancelled`, `expired`) |
| `price` | `price` | outside 1 to unit − 1 |
| `quantity` | `quantity` | not a positive whole number (the text as given) |
| `outcome` | `outcome` | not the market's |
| `limit` | `limit` | `maximum order cost` or `maximum position` |
| `quantity held` | `held` | a sell or merge of more than is held free |
| `owner` | `owner` | a cancel by someone else |
| `id` | `id` | a known order id with other fields; a cancel of an order never accepted |
| `key` | `key` | a known set key with other fields |
| `not fillable whole` | | a fill-whole order the books cannot fill whole |
| `ledger unavailable` | | the components' (spec 004 R8); never the domain's |

`not built` is gone.

## Events: what a consumer may rely on

- `OrderPlaced.cost` is what was reserved under `order` for a buy; 0 for a sell.
- `TradeMade.legs` has exactly two legs. `through = direct`: one buy leg and one sell leg of
  one outcome. `split`: two buy legs of different outcomes. `merge`: two sell legs of different
  outcomes. A buy leg's `charge` is what the trade takes from its reservation; `released`, under
  `releaseOrdinal` (from 1 per order), what it gives back; a sell leg's `paid` is what the seller
  receives. `prices` names every outcome traded.
- `OrderFilled` follows the `TradeMade` that completed the order, in the same decision.
- `OrderCancelled` and `OrderExpired` carry the remaining reservation under the order's next
  ordinal; a sell's is 0 under ordinal 0.
- `SetsSplit.charge = sets × unit` under `key`; `SetsMerged.paid = sets × unit` under `key`.
- `PositionPaid` once per position with shares at finality, `paid` 0 for a losing outcome,
  keyed by the market and `owner/outcome`.
- `RemainderKept.amount` moves no money.
- The events of one command, in order, replayed onto the value the command was applied to, give
  `Decision.next`.

## The order of a place's events

```
OrderPlaced
(TradeMade, [OrderFilled resting], [OrderFilled taker])*
[OrderCancelled taker]          # a fill-at-once remainder
```

## What is not here

No `Trade` in the value, no fees, no clock, no port. Everything else in
`specs/004-libraries/contracts/artefacts.md` and `ports.md` stands.

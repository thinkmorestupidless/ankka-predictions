# The markets domain API, as this spec fixes it

Spec 004's `contracts/artefacts.md` fixed the types, the manifests and the names. This spec fixes
what the functions decide. Nothing in the types changes (research R1).

## Functions

```scala
object Market:
  val empty: Market
  def apply(market: Market, command: MarketCommand): Either[Refusal, Decision]
  def replay(market: Market, event: MarketEvent): Market        // total over every case, unchanged

final case class Market(...):
  def resolution: Option[Resolution]                            // the one in force
  def nextDue: Option[Instant]                                  // FR-013
```

## Refusals

| Reason | Names | When |
|---|---|---|
| `out of order` | `last`, `at` | the command's time is before the market's last change |
| `state` | `state` (`empty`, `draft`, `approved`, `open`, `suspended`, `closed`, `resolved`, `void`, `final`) | the command does not apply in this state, or a change touches a field the state fixes |
| `field` | `field`: `question`, `description`, `category`, `outcomes`, `currency`, `close`, `expected resolution date`, `resolution criteria`, `authority`, `reason` | a field breaks its rule (research R3) or a reason is blank |
| `close` | `close` | an approval after the close; an early close without an operator and a reason; a close moved no later |
| `open time` | `open time` | an open before the open time |
| `outcome` | `outcome` | a resolution or correction to an outcome the market does not have |
| `evidence` | none | no evidence, or a piece with a blank part |
| `review window` | `review window` (its end, or the length when negative) | a correction or a void after the window; a finalise before its end; a negative length |
| `no change` | none | a correction to the outcome in force; a change to the definition the market has |

`not built` is gone.

## What the consumer relies on

- A command it applies at `nextDue` (open, close with no operator, finalise) is accepted at that
  time, given no other command moved the market first.
- The domain checks no permission, no category list and no currency table: the resolution port and
  the consumer's lists do (spec 004 `contracts/ports.md`; ankka-modules 023). A `change` and a
  `move close` ask the port nothing; the consumer checks who may first (research R12).
- A void after a resolution keeps the resolutions in the value: what was resolved stays on record.

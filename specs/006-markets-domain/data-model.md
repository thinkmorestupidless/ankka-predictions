# Data Model: The Markets Domain

The types are spec 004's (`modules/markets-domain/`); this spec adds no field (research R1). What
it fixes is which command may follow which, what each refuses, and what stays true.

## The value

`Market(market, definition, state, record, resolutions, void, lastTime)` (FR-001).

- `definition: Option[Definition]`: question, description, category, outcomes, currency, open
  time, close, expected resolution date, resolution criteria, authority. Field rules: research R3.
- `resolutions`: the first resolution and each correction revising it; the last is in force.
- `void: Option[VoidRecord]`: reason, evidence, operator, time, window end.
- `record: Vector[Entry]`: one entry per event: what, operator (none for open, a timed close and
  final), time, reason where given.
- `nextDue` (FR-013): the open time while approved, the close while open or suspended, the
  window's end while resolved or void, none otherwise.

## States

```text
empty ─draft→ draft ─approve→ approved ─open→ open ⇄ suspended
                     (approve opens at once when the open time is none or passed)
open | suspended ─close→ closed ─resolve→ resolved ─finalise→ final
approved | open | suspended | closed | resolved(within window) ─void→ void ─finalise→ final
resolved ─correct (within window)→ resolved
```

## Commands, their refusals in order, and their events

Every command is first refused as out of order when its time is before `lastTime`.

| Command | Refusals, in order | Events |
|---|---|---|
| `Draft` | state not empty (`state`); the fields (`field`, R3) | `Drafted` |
| `Change` | state final (`state`); approved and the question, outcomes or currency differ (`state`); approved and the open time none or at or before the time (`field` open time); open or later and any field but the description differs (`state`); the fields (`field`); the definition unchanged (`no change`) | `Changed` |
| `Approve` | not draft (`state`); close at or before the time (`close`) | `Approved`, and `Opened` when the open time is none or at or before the time |
| `Open` | not approved (`state`); before the open time (`open time`) | `Opened` |
| `Suspend` | not open (`state`); reason blank (`field` reason) | `Suspended` |
| `Resume` | not suspended (`state`) | `Resumed` |
| `Close` | not open or suspended (`state`); before the close without an operator and a non-blank reason (`close`) | `Closed` |
| `MoveClose` | not open or suspended (`state`); the new close at or before the current (`close`); reason blank (`field` reason) | `CloseMoved` with the expected resolution date moved to the new close's date where earlier |
| `Resolve` | not closed (`state`); the outcome not the market's (`outcome`); no evidence or a blank part (`evidence`); a negative window (`review window`) | `Resolved` with the window's end |
| `Correct` | not resolved (`state`); the window ended (`review window`); the outcome not the market's (`outcome`); evidence (`evidence`); the outcome in force (`no change`); a negative window (`review window`) | `Corrected` with a new window's end |
| `Void` | not approved, open, suspended, closed or resolved (`state`); resolved and the window ended (`review window`); reason blank (`field` reason); evidence (`evidence`); a negative window (`review window`) | `Voided` with the window's end |
| `Finalise` | final: answered with no event; not resolved or void (`state`); before the window's end (`review window`) | `Final` |

## Invariants (the property suite's, research R11)

1. A decision's events replayed onto the value it was decided on equal its next value.
2. The journal replayed onto `Market.empty` equals the value.
3. `lastTime` never goes back, and is the last event's time.
4. The record has one entry per event, in order.
5. `nextDue` is what FR-013 says for the state, worked out from the events alone; an approved
   market always has one.
6. A final market refuses every command but finalise, which yields no event.

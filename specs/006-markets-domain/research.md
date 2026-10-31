# Research: The Markets Domain

Decisions the plan rests on. Each says what was chosen, why, and what else was weighed. The spec's
clarifications of 2026-10-31 are not repeated; they are in `spec.md`.

## R1. The protocol does not move

**Decision**: no event case, event field, command field, manifest, component id or wire name
changes. `Refusals.notBuilt` is removed with the stub; `Refusals.field(name)` names a draft's
field; `MarketState`'s comment adds void from resolved.

**Rationale**: spec 004 fixed the events with this spec in view, and every rule here is decidable
from the events it already has. A void of a resolved market is a `Voided` like any other: replay
sets the state to void and keeps the resolutions it revised, so the record still shows what was
resolved before the void. A change of an approved market is a `Changed` carrying the whole
definition, as a draft's is.

**Alternatives**: a `field` list on `Changed` saying which fields moved (the consumer can diff two
definitions; adding it would be a protocol change before the first tag for no rule's sake).

## R2. The order of checks in a command

**Decision**: out of order first (spec 004, kept), then the state, then the command's own fields
and times in the order the data model lists them; the first that fails is the refusal. A finalise
of a final market is answered with no event before any other check (FR-012).

Where the spec lists refusals without an order (FR-012 names the window before the state), the
order is this one.

**Rationale**: the state check first means a command on the wrong kind of market is refused for
that, and never for a field it could not have used; the scenarios expect "naming the state" where
both would fail (`a draft market may not be voided`).

## R3. The draft's field rules

**Decision**: lengths count Unicode code points; a text field that is blank (empty or white space
only) is empty. In order: question 1 to 500; description 0 to 5,000; category non-blank; outcomes
two to twenty, each name 1 to 100 and all distinct; currency non-blank; close after the open time
when there is one, else after the command's time; expected resolution date no earlier than the
close's date in UTC; resolution criteria 1 to 2,000; authority 1 to 200. Each failure is
`Refusals.field(<name>)` with the name as the scenarios say it: `question`, `description`,
`category`, `outcomes`, `currency`, `close`, `expected resolution date`, `resolution criteria`,
`authority`. The rules live in `Drafting.scala`, used by draft and by change.

**Rationale**: FR-004's bounds, which came from the reasoning graph's market layer. Code points,
because a question in any script should be bounded by what a reader sees, not by UTF-16 units.
The close is compared with the command's time when there is no open time because the market
would otherwise open already closed. The expected resolution date is a date, so it is compared
with the close's UTC date: a close at 23:00 on the 24th resolving on the 24th is allowed.

**Alternatives**: bounds on the category and currency (the consumer checks both against its lists,
FR-004 keeps them as text); trimming the stored text (the domain keeps what it is given).

## R4. A change, by state

**Decision**: a draft: any field, under R3, with the change's time. Approved and not yet open:
any field but the question, the outcomes and the currency (a difference in one of those is
refused naming the state), under R3. Open, suspended, closed, resolved or void: only the
description; a difference in any other field, the close included, is refused naming the state.
Final: refused naming the state (FR-012). A change whose definition equals the market's is
refused as no change. An approved market whose new open time has already passed is not opened by
the change: `nextDue` then says the open time, and the consumer's timer applies the open at once.

**Rationale**: the clarification of 2026-10-31. Refusing an identical change keeps the record to
what changed. Opening inside a change would make one command two transitions the scenarios do not
describe; the timer path already exists.

## R5. Approve and open

**Decision**: approve: a draft only (state); a close at or before the command's time is refused
naming the close; otherwise `Approved`, and `Opened` in the same decision when there is no open
time or it is at or before the command's time. Open: approved only (state); before the open time
refused naming the open time; at or after it `Opened`.

**Rationale**: FR-006 and FR-007, as spec 004 began them; "at or after" is inclusive throughout,
so a timer firing exactly on time succeeds.

## R6. Evidence and the authority

**Decision**: a resolve, correct or void with no evidence, or with a piece whose source, locator
or excerpt is blank, is refused naming the evidence. A resolve records the authority it carries;
the domain does not compare it with the definition's: which authority may resolve is the
consumer's, as who may resolve is.

**Rationale**: constitution V: evidence that names nothing is not provenance. The authority is
checked where the operator is, by the consumer, which the scenarios give the market's.

**Alternatives**: refusing a resolve whose authority differs from the definition's (a new refusal
the spec does not name; raised for 023 if wanted).

## R7. The close

**Decision**: open or suspended only (state). At or after the close: accepted with or without an
operator and a reason, both recorded when given. Before it: accepted with an operator and a
non-blank reason, else refused naming the close.

**Rationale**: FR-007. A close before its time without a reason is a timer firing early or a
caller leaving the reason out; either way the close is what it is refused for.

## R8. Suspend, resume and move close

**Decision**: suspend: open only, a non-blank reason (else `field("reason")`). Resume: suspended
only. Move close: open or suspended; a new close at or before the current one refused naming the
close; a non-blank reason (else `field("reason")`); the expected resolution date becomes the new
close's UTC date when it would otherwise be earlier.

**Rationale**: FR-007a and FR-008; neither suspend nor resume is keyed (the spec's edge case).

## R9. Resolve, correct, void and finalise

**Decision**: the review window is open while the command's time is before its end, and has ended
at its end. A window length below zero is refused naming the review window; zero is allowed.
- Resolve: closed only; the outcome must be the market's; evidence (R6); the window ends at the
  command's time plus its length.
- Correct: resolved only (state); the window ended → review window; the outcome not the market's →
  outcome; evidence (R6); the outcome in force → no change. The new window ends at the command's
  time plus its length.
- Void: approved, open, suspended, closed or resolved (state); resolved and the window ended →
  review window; a non-blank reason (else `field("reason")`); evidence (R6). Its window ends at
  the command's time plus its length.
- Finalise: final → no event; resolved or void only (state); before the window's end → review
  window; else `Final`.

**Rationale**: FR-009 to FR-012 and the clarification. The boundary is the finalise's: at the
window's end the market may be made final, so it may no longer be corrected or voided.

## R10. The step definitions' world

**Decision**: `MarketsWorld` keeps markets by id, a fixed now (`2026-10-21T10:00:00Z`) for steps
without a time, the last command's answer, and builds every seeded market by applying commands
to `Market.empty`, never by constructing a value, so each seed is itself replayable. A seed with
no stated draft time is drafted at `2026-09-01T00:00:00Z`, before every time the scenarios use;
"resolved N hours ago" resolves at now less N hours with the Background's window; "the time T
comes" applies the command `nextDue` calls for at T (open, close or finalise).

Two steps carry more values than `GherkinSuite`'s six: the full draft (nine) and the correction
with a time (seven). Each is bound with literal text for the values only its one scenario uses
(`with the outcomes "yes" and "no" in "GBP"`; `on the evidence "premierleague.com" at
"https://premierleague.com/tables"`), as spec 005 put the side into the text; no scenario changes.

**Rationale**: the exchange's world (spec 005 R10) proved the shape; seeding through commands
means a seed that breaks a rule fails loudly instead of testing a market that cannot exist.

## R11. The property suite

**Decision**: `MarketProperties`, munit-scalacheck, 100,000 sequences of up to 30 commands from a
small vocabulary (two operators, a few definitions valid and not, times that mostly advance), and
after each: the decision's events replayed onto the value equal the next value; the journal
replayed onto `Market.empty` equals the value; `lastTime` never goes back; the record gains one
entry per event; `nextDue` is what FR-013 says for the state; a final market answers every command
but finalise with a refusal. The suite prints how many of each event it saw, as the exchange's does.

**Rationale**: SC-002, and the exchange's experience: printing the counts shows the generator
reached resolution and void rather than refusing everything.

## R12. The components change only where they stood on the stub

**Decision**: no change in `markets-components`' main sources. The tests' sample definitions are
made valid under R3 where they were not. Who may change a market or move its close is left to the
consumer (Trevor, 2026-10-31): the entity asks the resolution port nothing for either, as spec 004
built it, and ankka-modules 023's endpoint checks the permission first. Asked of ankka-modules as
item 10 in `notes/casino-requests.md`.

**Rationale**: SC-003, and the answer of 2026-10-31.

**Alternatives**: `change` asking `mayDraft` and `move close` asking `mayClose`; two new port
methods. Both declined for now.

## R13. The `0.0.0` fixtures are regenerated once

**Decision**: `JournalFixtureWriter` gains `resolved.jsonl`: a draft, a change, an approval with an
open time ahead, the open, a suspend and resume, a move close, the close, a resolve, a correction
and the finality; and `voided.jsonl`: a market resolved and then voided within its window, made
final. `opened.jsonl` and `every-event.jsonl` are rewritten unchanged. The directory is deleted
and the writer run for `0.0.0`.

**Rationale**: the first release's fixtures should carry a market's whole life; the compatibility
suite then replays every path a consumer will journal.

## R14. Documentation

**Decision**: the README says both domains' rules are in; `CLAUDE.md`'s table marks 006 building;
`docs/limitations.md` says what the markets domain does and leaves (who may act, the lists of
categories and currencies).

## Verify at implementation

- **V1.** `GherkinSuite` passes `""` from `asking ""` to a `String` definition as the empty string.
- **V2.** The fixtures `opened.jsonl` and `every-event.jsonl` are byte for byte what they were.
- **V3.** The components' suites pass with the sample definitions valid under R3 and nothing in
  `src/main` changed.

## Found during implementation

- **2026-10-31, the baseline (T001).** `sbt marketsDomain/test`: 5 passed (`MarketStubSuite`), 48
  ignored (the 43 scenarios of `features/markets/` and the five of `domain.feature`, each naming
  the spec or suite that lifts it). `sbt marketsComponents/test`: 9 passed, 4 ignored.
- **2026-10-31, `notBuilt` goes last.** T002 keeps `Refusals.notBuilt` until the last command
  body (T015) replaces `apply`'s fall-through, so every commit between compiles.
- **2026-10-31, a description-only change.** `Drafting.change` checks a change that touches only
  the description against the description's bound alone: an open market whose close has passed is
  still allowed its description.
- **2026-10-31, one commit for the bindings.** T005, T006, T010, T012 and T016 all write
  `MarketsFeatures.scala`; they were written together, before any body: every step of
  `features/markets/` has a definition, and against the stub 36 scenarios fail and 7 pass (the
  draft kept, the approvals that open, the open on time, the record, the next due).
- **2026-10-31, the full draft step.** With `"yes" and "no" in "GBP"` as literal text it still had
  seven values, so the category `"sport"` is literal too. In the correction with a time, the
  locator's slashes are escaped (`\/`), since `/` is alternation in a cucumber expression.
- **2026-10-31, one commit for the bodies.** T007 to T009, T011 and T013 to T015 all write
  `Market.apply`; written together, they turn the 36 failing scenarios green, and
  `Refusals.notBuilt` goes with the fall-through it served.
- **2026-10-31, the property suite's reach.** Purely random runs reached corrections 9 times in
  100,000 (a resolution needs a draft, an approval and a close first). Half the runs now start
  from a market closed early; the counts are then Resolved 16,433, Corrected 632, Voided 28,923
  (1,266 of them after a resolution) and Final 25,799. The default run takes about ten seconds.
- **2026-10-31, T020 had nothing to change (V3).** Every definition the components' tests draft
  (`Samples.definition`, a close a week ahead) was already valid under R3; `marketsComponents/test`
  passed unchanged on the real domain (9 passed, 4 ignored: the exchange's scenarios).
- **2026-10-31, R13 and V2.** `resolved.jsonl` and `voided.jsonl` (11 events each) are new;
  `opened.jsonl` and `every-event.jsonl` regenerated byte for byte as they were.

## Requests

- ankka-modules `notes/casino-requests.md` item 10: who may change a market or move its close is
  the predictions module's to check before it calls the market (R12).

## Design finished

2026-10-31: analyze reported nothing above LOW.

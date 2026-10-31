# Feature Specification: The Markets Domain

**Feature Branch**: `006-markets-domain`

**Created**: 2026-10-21

**Status**: Draft

**Input**: User description: "The `markets-domain` library of spec 004, in full: the value of
one market from draft to final, the commands applied to it, the refusals and the events. A market
is drafted with a question, a category, two to twenty outcomes, a currency, an optional open time,
a close, an expected resolution date, resolution criteria and an authority; changed while a draft;
approved; opened on its open time or at once; closed on its close or early with a reason; resolved
to one outcome on evidence by its authority; corrected within a review window; voided with a
reason and evidence; final when the window passes. The market checks its own shape and its own
times; who may act is the consumer's resolution port, and the market records the operator it is
told acted. Nothing reads a clock, a list of categories, a currency table or a network."

## Clarifications

### Session 2026-10-31

- Q: May a resolved market be voided within its review window? → A: Yes, with a reason and
  evidence, restarting the window as a correction does; after the window it is refused naming
  the review window.
- Q: After approval, which fields may change? → A: Approved and not yet open, every field but the
  question, the outcomes and the currency; once open (or suspended, closed, resolved or void),
  only the description, and the close through move close.
- Q: Do the glossary's proposed terms stand? → A: All 21 accepted as defined.
- Q: (found in review) May a change to an approved market remove its open time or move it to now
  or earlier, which would leave it approved with nothing due? → A: No; refused naming the open
  time.

## Context

Spec 002 described the `markets` service: the lifecycle, the catalogue players read, the facts on
a topic and the mirror into a reasoning graph. Decision D1 split it. The lifecycle is a domain:
rules about what a market is and which change may follow which, decidable from the market and
the command alone. The catalogue, the facts, the mirror and who may act are the consumer's, and
ankka-modules spec 023 carries them. This spec restates the lifecycle as the value and its
commands, as spec 005 does for the exchange.

Two rules spec 002 gave the service are the consumer's here. Whether a category is one the
product lists and whether a currency is one it offers depend on settings the domain cannot see;
the domain keeps both as text and the consumer checks them before drafting. Whether the operator
named may draft, approve, resolve or void is the resolution port's answer; the domain records the
operator it is given. The bounds spec 002 set on the wording stay, because they came from what the
reasoning graph's market layer accepts and a market should fit it whether or not it is mirrored.

An open market may be suspended by an operator with a reason and resumed by one: the
operator's emergency authority, which the first licence (a US Designated Contract Market,
`docs/product.md`) requires of the venue. The exchange suspends and resumes with the market
(spec 005). Suspended, the market still closes when its close comes.

The market tells the consumer when it is next due to change by itself: its open time, its close
or the end of its review window. The consumer's timer applies the command at that time; the
domain refuses the command before it.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - A market is drafted, changed and approved (Priority: P1)

A developer drafts a market on an empty value: a question of one to five hundred characters, a
description, a category, two to twenty outcomes with distinct names, a currency, an open time or
none, a close after the open time, an expected resolution date no earlier than the close,
resolution criteria and an authority. The draft is refused naming the field that breaks a rule.
Any field of a draft may be changed. Approval records the operator and the time; after it the
question, the outcomes and the currency cannot change, and once the market opens only its
description can (its close moves through move close). A draft whose close has passed cannot be
approved.

**Why this priority**: Nothing opens without an approved market.

**Independent Test**: Draft with each field wrong and then right; change a draft and an approved
market; approve a draft whose close has passed; every refusal names its field.

**Acceptance Scenarios**:

- changed `features/markets/lifecycle.feature`: a draft market with a question, a category, two outcomes, a currency, a close and resolution criteria is kept as a draft
- changed `features/markets/lifecycle.feature`: a draft with fewer than two outcomes is refused
- changed `features/markets/lifecycle.feature`: a draft with two outcomes of the same name is refused
- added `features/markets/lifecycle.feature`: a draft with more than twenty outcomes is refused
- changed `features/markets/lifecycle.feature`: a draft whose close is not after its open time is refused
- changed `features/markets/lifecycle.feature`: a draft whose expected resolution date is before its close is refused
- added `features/markets/lifecycle.feature`: a draft with an empty question is refused
- changed `features/markets/lifecycle.feature`: a draft may be changed by an operator until it is approved
- changed `features/markets/lifecycle.feature`: an approved market's question and outcomes may not be changed
- added `features/markets/lifecycle.feature`: an approved market's resolution criteria may be changed before it opens
- added `features/markets/lifecycle.feature`: an approved market's open time may not be removed
- added `features/markets/lifecycle.feature`: an open market's resolution criteria may not be changed
- added `features/markets/lifecycle.feature`: a draft whose close has passed may not be approved

---

### User Story 2 - A market opens and closes on its times (Priority: P1)

An approved market with no open time, or whose open time has passed, opens on approval; one with
an open time ahead opens when that time comes and is refused opening before it. An open market
closes when its close comes, or earlier when an operator closes it with a reason. While open, its
close may be moved later with a reason, never earlier. An open market may be suspended by an
operator with a reason, and a suspended one resumed; a market that is not open cannot be
suspended; a suspended market closes when its close comes, as an open one does.

**Why this priority**: The exchange opens, suspends, resumes and closes with the market.

**Independent Test**: Approve with and without an open time; open early, on time; close on time,
early, and move the close both ways.

**Acceptance Scenarios**:

- changed `features/markets/lifecycle.feature`: an approved market with no open time opens at once
- changed `features/markets/lifecycle.feature`: an approved market with an open time opens when that time comes
- added `features/markets/lifecycle.feature`: an approved market is not opened before its open time
- added `features/markets/lifecycle.feature`: an approved market whose open time has passed opens at once
- added `features/markets/lifecycle.feature`: an open market's close may be moved later with a reason
- added `features/markets/lifecycle.feature`: an open market's close may not be moved earlier
- added `features/markets/lifecycle.feature`: an operator suspends an open market naming a reason
- added `features/markets/lifecycle.feature`: an operator resumes a suspended market
- added `features/markets/lifecycle.feature`: a market that is not open may not be suspended
- added `features/markets/lifecycle.feature`: a suspended market closes when its close comes
- changed `features/markets/resolution.feature`: an open market closes when its close comes
- changed `features/markets/resolution.feature`: an operator closes an open market early naming a reason

---

### User Story 3 - A market is resolved on evidence, corrected, voided and made final (Priority: P1)

A closed market is resolved to one of its outcomes naming at least one piece of evidence (a
source, a locator and an excerpt) and the authority; a resolution with no evidence, to an outcome
the market lacks, or on a market that is not closed is refused. A resolution may be corrected to a
different outcome within the review window, on evidence; the correction revises the first and
restarts the window; a correction after the window, or to the outcome already resolved, is
refused. An approved, open or closed market may be voided with a reason and evidence, and a
resolved one within its review window; a draft or a final market may not. A resolved or void market is final when its review window passes and is
refused finality before.

**Why this priority**: Resolution is where money is decided; its evidence is the product's
integrity.

**Independent Test**: Resolve, correct inside and outside the window, void from every state, make
final early and on time; every refusal names its reason.

**Acceptance Scenarios**:

- changed `features/markets/resolution.feature`: a closed market is resolved to one of its outcomes on evidence by its authority
- changed `features/markets/resolution.feature`: a resolution naming no evidence is refused
- changed `features/markets/resolution.feature`: a resolution naming an outcome the market does not have is refused
- changed `features/markets/resolution.feature`: an open market may not be resolved
- changed `features/markets/resolution.feature`: a resolution is corrected within the review window on new evidence
- changed `features/markets/resolution.feature`: a resolution may not be corrected after the review window
- added `features/markets/resolution.feature`: a correction naming the outcome already resolved is refused as no change
- added `features/markets/resolution.feature`: a correction restarts the review window
- changed `features/markets/resolution.feature`: a resolution is final when the review window passes
- added `features/markets/resolution.feature`: a resolved market is not made final before the review window passes
- changed `features/markets/resolution.feature`: an approved market is voided with a reason and evidence
- added `features/markets/resolution.feature`: a void is final when the review window passes
- added `features/markets/resolution.feature`: a resolved market is voided within its review window
- added `features/markets/resolution.feature`: a resolved market may not be voided after the review window
- added `features/markets/resolution.feature`: a final market may not be voided
- added `features/markets/resolution.feature`: a draft market may not be voided

---

### User Story 4 - The market records its changes and tells what is next due (Priority: P2)

A market records, for each change, the operator and the time, and the reason where one was
given. It tells the time it is next due to change by itself: its open time while approved, its
close while open, the end of its review window while resolved or void, and nothing otherwise.

**Why this priority**: The consumer's timers and its operator reads come from these; they prove
nothing the lifecycle does not.

**Independent Test**: Run a market through its life and read the record and the next due time at
each step.

**Acceptance Scenarios**:

- added `features/markets/lifecycle.feature`: a market records who did each change and when
- added `features/markets/lifecycle.feature`: a market tells the time it is next due to change

---

### User Story 5 - The value, its events and replay (Priority: P1)

Every command yields a refusal naming its reason, with no event and no change, or the next value
and its events; replaying the events onto an empty value yields an equal value. A command carries
its time; one earlier than the market's last change is refused as out of order.

**Why this priority**: Spec 004's promise, made concrete for this domain.

**Independent Test**: Property tests over random command sequences: replay equality and refusal
purity.

**Acceptance Scenarios**:

- changed `features/libraries/domain.feature`: a market rebuilt from its events alone is the market that produced them
- added `features/markets/resolution.feature`: a change carrying a time earlier than the market's last change is refused as out of order

---

### Edge Cases

- A draft with no open time and a close in the past at approval: refused naming the close; the
  draft is changed first.
- A close moved later on a market whose expected resolution date is before the new close: the
  expected resolution date is moved to the close in the same change.
- Two changes carrying the same time: applied in the order given; the record keeps both.
- A suspend of a suspended market, or a resume of an open one: refused naming the state; neither
  is keyed, since neither moves money.
- A void of a suspended market: allowed, as of an open one; a close moved while suspended: as
  while open.
- A void of a resolved market: allowed within the review window, for a question that turned out
  to have no answer (a wrong outcome is corrected instead); it restarts the window; after the
  window it is refused naming the review window. Spec 002 allowed void from approved, open or
  closed only.
- A blank reason on a suspend, a move close or a void; evidence missing or with a blank source,
  locator or excerpt; a review window of negative length; a change to the definition the market
  already has: refused naming the reason, the evidence, the review window and as no change
  respectively. An early close without an operator and a reason is refused naming the close.
- The categories and currencies a product allows: checked by the consumer before the draft
  command; the domain keeps both as text. Spec 002 FR-011 (a category in use cannot be removed)
  is the consumer's and is asked of ankka-modules in `notes/casino-requests.md`.
- Who may approve, and whether the drafter may: the resolution port's. ankka-modules spec 023
  answered it (2026-10-31): one operator approves, and the drafter may.

## Requirements *(mandatory)*

### Functional Requirements

**The value**

- **FR-001**: The value MUST be one market: its question, description, category, outcomes,
  currency, open time (or none), close, expected resolution date, resolution criteria and
  authority; its state (draft, approved, open, suspended, closed, resolved, void, final); the
  record of each
  change with its operator, time and reason; its resolution (outcome, evidence, authority,
  operator, time, the end of the review window) and each correction revising it; its void (reason,
  evidence); and the time of its last change.
- **FR-002**: The commands MUST be draft, change, approve, open, suspend, resume, close, move
  close, resolve, correct, void and finalise, each carrying its time and, where an operator acts, the operator.
  A time earlier than the last change MUST be refused as out of order. Every command MUST yield a
  refusal naming its reason, with no event and no change, or the next value and its events;
  replaying a value's events onto an empty value MUST yield an equal value.
- **FR-003**: The domain MUST read no clock, settings, list, database or network, and MUST
  depend on `ankka-core` alone (spec 004 FR-003).

**Drafting**

- **FR-004**: A draft MUST carry a question of 1 to 500 characters, a description of up to
  5,000, a category, two to twenty outcomes with distinct names of 1 to 100 characters each, a
  currency, an optional open time, a close after the open time (or after the draft's time when
  there is none), an expected resolution date no earlier than the close, resolution criteria of 1
  to 2,000 characters and an authority of 1 to 200; a draft breaking any of these MUST be refused
  naming the field. The category and the currency are text to the domain.
- **FR-005**: Any field of a draft MAY be changed under the same rules. An approved market not
  yet open MAY change every field but the question, the outcomes and the currency, keeping an
  open time after the change's time (else refused naming the open time). Once open,
  suspended, closed, resolved or void, only the description MAY change, and the close only
  through move close (FR-008); a final market refuses every change (FR-012). Any other change
  MUST be refused naming the state.

**Opening and closing**

- **FR-006**: An approve MUST be refused naming the close when the close has passed; otherwise it
  MUST record the operator and the time and, when there is no open time or it has passed, open
  the market in the same change.
- **FR-007**: An open MUST be refused naming the open time before it; at or after it the market
  MUST be open. A close MUST be refused naming the state unless the market is open or suspended;
  at or after the close it needs no reason, before it an operator and a reason MUST be recorded.
- **FR-007a**: A suspend MUST be refused naming the state unless the market is open; otherwise it
  MUST record the operator, the time and a reason, and the market MUST be suspended. A resume MUST
  be refused naming the state unless the market is suspended; otherwise it MUST record the
  operator and the time, and the market MUST be open again.
- **FR-008**: While open or suspended, the close MAY be moved later with an operator and a reason, moving the
  expected resolution date to it when that would otherwise be earlier; moved earlier, the change
  MUST be refused naming the close.

**Resolution, correction, void, finality**

- **FR-009**: A resolve MUST be refused naming the state unless the market is closed, naming the
  outcome when it is not the market's, and naming the evidence when none is given; otherwise it
  MUST record the outcome, at least one piece of evidence (a source, a locator and an excerpt),
  the authority, the operator, the time and the end of the review window, which the command
  carries as a length.
- **FR-010**: A correct MUST be refused naming the review window after it, and as no change when
  it names the outcome already resolved; otherwise it MUST record a resolution revising the first,
  with evidence, and restart the review window from its time.
- **FR-011**: A void MUST be refused naming the state unless the market is approved, open,
  suspended, closed or resolved, and naming the review window when it is resolved and the window
  has ended; otherwise it MUST record a reason, at least one piece of evidence, the operator, the
  time and the end of a review window from its time.
- **FR-012**: A finalise MUST be refused naming the review window before its end and naming the
  state unless the market is resolved or void; at or after the end the market MUST be final, and
  final it MUST refuse every command but a repeat of finalise, which produces no event.

**The record and the next due time**

- **FR-013**: The value MUST answer, for each change, the operator, the time and the reason, and
  MUST answer the time it is next due: the open time while approved and ahead, the close while
  open or suspended, the end of the review window while resolved or void, none otherwise.

**Events**

- **FR-014**: The events MUST be at least: drafted, changed, approved, opened, suspended,
  resumed, closed, close moved, resolved, corrected, voided, final; and MUST evolve as spec 004 FR-009 says.

### Key Entities

- **Market**: the value (FR-001).
- **Resolution**: outcome, evidence, authority, operator, time, window end; revised by a
  correction.
- **Void**: reason, evidence, operator, time, window end.
- **Evidence**: a source, a locator and an excerpt; never fewer than one per resolution or void.
- **Next due**: the one time the consumer sets a timer for.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Every scenario under `features/markets/` and the market scenario of
  `features/libraries/domain.feature` pass against the value alone, with no runtime started.
- **SC-002**: A property suite over at least 100,000 random command sequences finds no sequence
  whose replay is unequal to the value produced, and no refusal that changed the value.
- **SC-003**: eitheror's `markets` service adds no lifecycle rule: its code is the resolution
  port, the catalogue, the facts, the mirror and the timers (spec 004 SC-004).

## Assumptions

- The review window's length is carried by the resolve and void commands from the consumer's
  setting, 24 hours by default (spec 002); the domain has no default.
- The wording bounds stay as spec 002 set them from the reasoning graph's market layer; a product
  that never mirrors keeps them all the same.
- The exchange of spec 005 opens when the market opens, suspends and resumes with it, closes
  when it closes and is made final when it is final; driving one from the other is the consumer's (ankka-modules spec 023).

## Dependencies

- Spec 004 (the libraries, the ports, the journal as a contract); spec 005 (the exchange the
  market's changes drive).
- ankka-modules spec 023 (the consumer: who may, the categories and currencies, the timers).

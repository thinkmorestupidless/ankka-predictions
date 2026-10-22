# Feature Specification: Markets

**Feature Branch**: `002-markets`

**Created**: 2026-10-20

**Status**: Superseded

*Superseded on 2026-10-21 (decision D1): the market's lifecycle is restated as the markets domain in spec 006; the catalogue, the facts and the reasoning mirror are eitheror's, carried by ankka-modules spec 023. This spec is kept as written and is no longer checked against the living features.*

**Input**: User description: "The `markets` service: the catalogue and the lifecycle of a market. An operator writes a market as a draft (question, description, category, two or more outcomes, currency, open time, close, expected resolution date, resolution criteria, authority), approves it, and it opens and closes on its times; an operator may close it early, resolve a closed market to one outcome on evidence by its authority, correct a resolution within a review window, or void it. Every market is mirrored into the reasoning graph as a question, one hypothesis per outcome and a market; a resolution is mirrored with its evidence. Facts on `markets.events`. Players read open, closed and resolved markets by category and time; operators read everything."

## Context

A market is the thing a player forecasts and trades. The exchange (spec 003) opens a book per
outcome when a market opens and settles positions when it resolves; the forecasts (spec 004) and
the interface (spec 005) read the catalogue. The reasoning graph is where the market's question,
its outcomes as hypotheses, its prices and its resolution are recorded, so that the product's
explanations trace to records there (constitution V). ankka-reasoning's market layer already
holds a market as a venue, outcomes mapped to hypotheses, resolution criteria, a close, price
observations and resolutions with evidence; this service writes those and keeps nothing of the
graph itself.

At launch an operator creates every market by hand. Agentic creation (spec 008) proposes drafts to
the same lifecycle.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - An operator writes, approves and opens a market (Priority: P1)

An operator drafts a market: a question, a description, a category from the product's list, two or
more outcomes with distinct names, a currency the product offers, an open time (or none, to open
on approval), a close after the open, an expected resolution date no earlier than the close,
resolution criteria and an authority. The draft is checked and refused naming what is wrong. An
operator approves the draft, and the market opens at its open time, or at once when it has none.
A player sees a market only once it is open.

**Why this priority**: Nothing can be forecast or traded without an open market.

**Independent Test**: Draft, approve and let a market open on a clock the test controls; confirm
each state, each refusal, and what a player can see at each step.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/markets/lifecycle.feature`: a draft market with a question, a category, two outcomes, a currency, a close and resolution criteria is kept as a draft
- added `features/markets/lifecycle.feature`: a draft with fewer than two outcomes is refused
- added `features/markets/lifecycle.feature`: a draft with two outcomes of the same name is refused
- added `features/markets/lifecycle.feature`: a draft whose close is not after its open time is refused
- added `features/markets/lifecycle.feature`: a draft whose expected resolution date is before its close is refused
- added `features/markets/lifecycle.feature`: a draft naming a category the product does not list is refused
- added `features/markets/lifecycle.feature`: a draft naming a currency the product does not offer is refused
- added `features/markets/lifecycle.feature`: an approved market with no open time opens at once
- added `features/markets/lifecycle.feature`: an approved market with an open time opens when that time comes
- added `features/markets/lifecycle.feature`: a draft market is not shown to a player
- added `features/markets/lifecycle.feature`: an approved market that has not opened is not shown to a player
- added `features/markets/lifecycle.feature`: an open market is shown to a player with its outcomes
- added `features/markets/lifecycle.feature`: a draft may be changed by an operator until it is approved
- added `features/markets/lifecycle.feature`: an approved market's question and outcomes may not be changed

---

### User Story 2 - A market closes and is resolved on evidence (Priority: P1)

A market closes at its close, or earlier when an operator closes it naming a reason. An operator
resolves a closed market to one of its outcomes, naming at least one piece of evidence (a source,
where it was found, and the words that settle it) and the authority consulted. A resolution may be
corrected by an operator within the review window, with evidence; after the window it is final
and the exchange pays positions. An operator may void a market that is approved, open or closed,
with a reason and evidence, and a void is final after the same window.

**Why this priority**: Resolution is where money is decided; the rule and its evidence are the
product's integrity.

**Independent Test**: Close a market by its clock and by an operator; resolve, correct within the
window, attempt to correct after; void from each state; confirm the states, the refusals and that
every resolution names evidence.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/markets/resolution.feature`: an open market closes when its close comes
- added `features/markets/resolution.feature`: an operator closes an open market early naming a reason
- added `features/markets/resolution.feature`: a closed market is resolved to one of its outcomes on evidence by its authority
- added `features/markets/resolution.feature`: a resolution naming no evidence is refused
- added `features/markets/resolution.feature`: a resolution naming an outcome the market does not have is refused
- added `features/markets/resolution.feature`: an open market may not be resolved
- added `features/markets/resolution.feature`: a resolution is corrected within the review window on new evidence
- added `features/markets/resolution.feature`: a resolution may not be corrected after the review window
- added `features/markets/resolution.feature`: a resolution is final when the review window passes
- added `features/markets/resolution.feature`: an approved market is voided with a reason and evidence
- added `features/markets/resolution.feature`: a resolved market whose window has passed may not be voided
- added `features/markets/resolution.feature`: a market's resolution is shown with its outcome, its evidence and its authority

---

### User Story 3 - A market is mirrored into the reasoning graph (Priority: P2)

When a market is approved, the service records in the reasoning graph a question with the
market's wording, one hypothesis per outcome, and a market naming those hypotheses, the venue,
the resolution criteria and the close. When a market is resolved or voided, the service records
the evidence and a resolution; a correction is recorded as a resolution revising the first. The
reasoning graph being unavailable never stops a market from approving, closing or resolving; the
mirror is retried until it is written, and the market says whether its mirror is current.

**Why this priority**: Explanations and the agents' forecasts (spec 006) need the question and the
market in the graph; it comes after the lifecycle because a market works without it.

**Independent Test**: Approve and resolve a market with the reasoning graph present and absent;
confirm the records written, the retry, and the market's mirror status.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/markets/reasoning.feature`: an approved market is recorded in the reasoning graph as a question, one hypothesis per outcome and a market
- added `features/markets/reasoning.feature`: a resolved market's evidence and resolution are recorded in the reasoning graph
- added `features/markets/reasoning.feature`: a corrected resolution is recorded as revising the first
- added `features/markets/reasoning.feature`: a market approves while the reasoning graph cannot be reached and its mirror is written when it can
- added `features/markets/reasoning.feature`: a market says whether its mirror is current

---

### User Story 4 - Players and operators read the catalogue (Priority: P2)

A player lists open markets by category, and markets closing soonest; reads one market with its
outcomes, its close and its resolution criteria; and lists closed and resolved markets. An
operator lists markets in every state, drafts included, and reads a market with who drafted,
approved, closed, resolved or voided it and when.

**Why this priority**: The interface's Discover and Trade (spec 005) start from these lists.

**Independent Test**: Seed markets in every state and category; list and read as a player and as
an operator; confirm what each sees.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/markets/catalogue.feature`: a player lists open markets of a category
- added `features/markets/catalogue.feature`: a player lists open markets closing soonest first
- added `features/markets/catalogue.feature`: a player lists resolved markets with their outcomes
- added `features/markets/catalogue.feature`: an operator lists draft markets
- added `features/markets/catalogue.feature`: an operator reads who approved a market and when

---

### User Story 5 - Every change to a market is a fact (Priority: P3)

Every change to a market is published on `markets.events`, keyed by the market: drafted, changed,
approved, opened, closed, resolved, resolution corrected, final, voided. The exchange opens and
closes books and settles from these; the forecasts and the interface build their views from them.

**Why this priority**: Readers never query the catalogue's store; it comes last because the
lifecycle is proved first.

**Independent Test**: Run a market through its life; confirm one fact per change, in order, with
the fields each reader needs.

**Acceptance Scenarios** *(each names a scenario in a living feature; none is written here)*:

- added `features/markets/facts.feature`: every change to a market is published as one fact naming the market and its new state
- added `features/markets/facts.feature`: a resolution fact names the outcome and the end of the review window
- added `features/markets/facts.feature`: a final fact is published when the review window passes

---

### Edge Cases

- An operator approves a market whose open time has already passed: it opens at once.
- An operator approves a market whose close has already passed: refused; the draft must be
  changed first.
- A market is voided while open: the exchange cancels its orders and, when the void is final,
  pays every share an equal part of one unit (spec 003); this service only records the void.
- The clock that opens and closes markets runs late: the market opens or closes when the timer
  fires, and the fact carries the time it should have, so readers can tell.
- Two operators act on one market at once: the second is refused naming the state the first
  produced.
- A correction names the same outcome as the resolution: refused as no change.
- The reasoning graph refuses a record by a rule of its own (a question too long, an outcome name
  too long): the market is approved and its mirror status says why it is not current, for an
  operator to fix the wording or the graph.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: An operator MUST be able to draft a market with a question (1 to 500 characters), a
  description (up to 5,000), a category from the product's list, two to twenty outcomes with
  distinct names (1 to 100 characters each), a currency the product offers, an optional open
  time, a close after the open time (or after now, with no open time), an expected resolution
  date no earlier than the close, resolution criteria (1 to 2,000 characters) and an authority (1
  to 200). A draft that breaks any of these MUST be refused naming the field.
- **FR-002**: An operator MUST be able to change any field of a draft; after approval the
  question, the outcomes and the currency MUST NOT change, and the close and the expected
  resolution date MAY be moved later by an operator while the market is open, naming a reason.
- **FR-003**: An operator MUST be able to approve a draft, recording the operator and the time.
  [NEEDS CLARIFICATION: whether the operator who drafted a market may approve it, or a second
  operator must; a four-eyes rule is usual where money is at stake, and the casino's backoffice
  roles do not yet say.]
- **FR-004**: An approved market MUST open at its open time, or at once when it has none or the
  time has passed, and MUST close at its close; both by the service's own timers, which MUST
  survive a restart. An operator MUST be able to close an open market early naming a reason.
- **FR-005**: The states MUST be draft, approved, open, closed, resolved, void and final, and only
  these transitions MUST be allowed: draft to approved; approved to open; open to closed; closed
  to resolved; resolved to resolved (a correction, within the review window); approved, open or
  closed to void; resolved or void to final when the review window passes. Any other MUST be
  refused naming the state.
- **FR-006**: A resolution MUST name one of the market's outcomes, at least one piece of evidence
  (a source name, a locator and an excerpt) and the authority; a void MUST name a reason and at
  least one piece of evidence; a correction MUST name a different outcome than the resolution it
  corrects, within the review window. Every resolution, correction and void MUST record the
  operator and the time.
- **FR-007**: The review window MUST be a product setting, 24 hours by default, measured from the
  resolution; the market MUST become final when it passes, by a timer, and the final fact MUST be
  what the exchange pays on.
- **FR-008**: The service MUST write to the reasoning graph, on approval, a question with the
  market's wording, one hypothesis per outcome, and a market naming the venue `predictions`, the
  hypotheses, the resolution criteria and the close; on resolution or void, the evidence and a
  resolution; on a correction, a resolution revising the first. Every write MUST be idempotent by
  the market's id, retried until written, and MUST never block or fail a transition; the market
  MUST say whether its mirror is current and, when it is not, why.
- **FR-009**: Players MUST be able to list open markets by category and by close, read an open,
  closed, resolved, void or final market with its outcomes, close, resolution criteria, and, once
  resolved, its outcome, evidence and authority; drafts and approved markets MUST NOT be shown to
  players. Operators MUST be able to list and read markets in every state with who did what and
  when.
- **FR-010**: Every change MUST be published on `markets.events`, keyed by the market, with a
  stable fact id, the state after it, the time it took effect and the time it was due, and the
  fields a reader needs: outcomes and currency on approval, the outcome and the window's end on
  resolution and correction, the reason on a void or an early close.
- **FR-011**: The list of categories MUST be an operator setting; a category in use by a market
  MUST NOT be removed.

### Key Entities *(each a term in the project glossary)*

- **market**: the question, outcomes, currency, times, criteria and authority, with its state and
  who changed it.
- **outcome**: a named way the market can resolve; one hypothesis in the reasoning graph.
- **resolution**: the outcome, the evidence, the authority, the operator and the time; corrected
  within the review window; final after it.
- **void**: a resolution to no outcome, with a reason and evidence.
- **review window**: the time after a resolution during which it may be corrected.
- **category**: a subject from the operator's list.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A market opens and closes within 5 seconds of its times in 99% of cases on a running
  installation.
- **SC-002**: A player's list of open markets by category answers within 200 ms at the 99th
  percentile with 10,000 markets in the catalogue.
- **SC-003**: 100% of resolutions and voids in a test name at least one piece of evidence and an
  authority.
- **SC-004**: With the reasoning graph absent for an hour, 100% of markets approved in that hour
  are mirrored within 5 minutes of its return.
- **SC-005**: A reader folding `markets.events` arrives at the state of every market in the
  catalogue.

## Assumptions

- The product's categories at launch are sport, politics, technology, economics, science,
  entertainment and other; operators change the list.
- The review window is 24 hours; disputes raised by players are a later feature, and until then
  a correction is an operator's act.
- A void market's payout rule belongs to spec 003.
- An operator is a holder of a staff token with a role naming this product, as the casino's
  backoffice routes are admitted.
- The reasoning graph is ankka-reasoning 0.2.0; its bounds (a question of at most 500 characters,
  an outcome name of at most 16 characters as the market layer's outcome id, a market id of at
  most 32) shape FR-001's limits, and an outcome's id in the graph is derived from its name.
- Timers are the platform's; the service keeps the due time so that a late timer is visible.

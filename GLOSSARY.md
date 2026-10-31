# Glossary

The words this project's features use, each in exactly one sense. A term is proposed until
`/speckit-clarify` settles it or the features that use it pass against the service; as of
2026-10-20 every term below is proposed, since nothing runs yet.

## People

### player
A person who places orders and holds positions. The libraries know a player only as the id an
order or a position carries; who they are, their account and their money are the consumer's.
Avoid: user, trader, customer, participant

### owner
*Proposed.* The player an order, a position, a split or a merge belongs to; the only player who
may cancel the order.

### operator
The person a market records as having drafted, changed, approved, closed, resolved, corrected or
voided it. Whether they may is the consumer's resolution port; the market records who it is told.
Avoid: admin, administrator, staff member

### account
A player's standing with the casino's players service: active, suspended, self-excluded, blocked
or closed. Not the wallet.

### forecaster
A player seen through their forecasts and their record, rather than their money.

## Money

### wallet
A player's ledger account in one currency: available money and reserved money. The one place money
is kept in this product.
Avoid: balance sheet

### available money
The part of a wallet a player may reserve, withdraw or spend now.
Avoid: free balance

### reserved money
The part of a wallet set aside against the player's resting orders. Released when an order is
cancelled, expires or is filled at a better price; moved when a trade settles.
Avoid: locked funds, escrow

### reservation
The reserved money that one order holds, keyed by the order.

### deposit
Money arriving in a wallet from the payments stack, keyed by its payment.

### withdrawal
Money leaving a wallet through the payments stack, as the casino's `payouts` service runs it.

### hold
Reserved money a withdrawal request takes while it is reviewed and paid, keyed by the request.

### minor unit
The smallest unit of a currency; a penny of a pound. Every amount in this product is a whole number
of minor units.

### unit
What one share pays if its outcome wins, as a whole number of minor units carried when an
exchange opens: one hundred for a currency with two decimals.

### key
The identifier a caller supplies with a money command so that a repeat of the command is answered
as the first was.
Avoid: idempotency key, nonce

### payment
A deposit or a refund as the payments stack knows it, identified by its payment id; the key of a
deposit.

### deposit gate
The responsible-gambling check a deposit passes before it is credited: allowed, excluded, cooling
off, over limit, or unavailable.
Avoid: RG check

### withdrawal request
A player's request to withdraw an amount, as the casino's `payouts` service runs it; the key of a
hold.

### hold preview
What a withdrawal request would be able to hold: the most withdrawable amount and whether the
player must confirm.

### transaction
One recorded change to a wallet: a deposit, a reservation, a release, a trade, a payout, a hold
step or an adjustment, with its amount, its id and its time.

### adjustment
An operator's keyed, signed correction to a wallet, with a reason and a kind.

### actor
The operator or service recorded as having made a change.

### fact
A message published about one change to a wallet, carrying the balances after it; what a reader
folds to rebuild the wallet.

### erasure
The removal of a player's personal fields on their request, leaving amounts, ids and times.

### currency
The ISO 4217 currency a wallet, a market, an order and a position carry; nothing converts
between two.

### pool
The money a market's complete sets are backed by: one unit for every set outstanding, plus any
remainder a void left. The exchange counts it; the consumer's ledger holds it (ankka-modules' pool
account). What a split pays into, and a merge and a payout are paid from.
Avoid: escrow, house

### play gate
The casino's KYC check that a player may play, asked before an order.

### bet gate
The casino's responsible-gambling check that a player may stake an amount, asked with a buy
order's cost before the order is accepted.
Avoid: RG gate, may-bet

## Markets

### market
A question about the future with two or more outcomes, a close, resolution criteria and an
authority, traded in one currency; the thing a player forecasts and trades.
Avoid: contract

### question
The market's wording: what will or will not happen, by when.

### outcome
One of the ways a market can resolve. Every market has at least two; a two-outcome market's are
"yes" and "no".
Avoid: option, side

### category
The subject a market belongs to: sport, politics, technology, economics, and so on.

### close
The moment trading ends on a market; may be moved later while the market is open, never earlier.
Avoid: expiry, closing time

### open time
*Proposed.* The moment an approved market opens; a market with none opens on approval.

### expected resolution date
*Proposed.* The date an operator expects to resolve the market, no earlier than its close.

### next due
*Proposed.* The time a market is next due to change by itself: its open time, its close, or the
end of its review window; what a consumer sets a timer for.

### resolution criteria
The text that says what evidence settles the market and how.

### authority
The source the operator consults to resolve a market, named when the market is created.

### resolution
The operator's record that a market came to one outcome, on evidence, by its authority.

### void
The resolution of a market that came to no outcome: every share of every outcome is paid an equal
part of one unit.
Avoid: annulled

### draft market
A market an operator has written and not yet approved.

### approved market
A market an operator has approved and that has not yet opened.

### open market
A market that is trading.

### suspended market
An open market on which an operator has halted trading with a reason, until it is resumed or its
close comes. Its resting orders stand and may be cancelled; nothing is placed, split or merged.
Avoid: halted market, paused market

### suspend
An operator's halt of trading on an open market, with a reason: the operator's emergency
authority. The exchange suspends with the market.

### resume
An operator's reopening of a suspended market; trading continues with the orders that rested.

### closed market
A market whose close has passed and that is not yet resolved.

### review window
The time after a resolution or a void during which an operator may correct it; when it passes
the market is final. A correction restarts it. Its length is carried by the command.

### final market
A resolved or void market whose review window has passed; what the exchange pays on.

### mirror
A market's record in the reasoning graph: its question, hypotheses, market and resolutions. It is
current when every record the market has produced is written there.

## Trading

### share
A claim on one unit of the market's currency if its outcome wins, and on nothing otherwise.
Avoid: token, contract, ticket

### price
A whole number of minor units that one share trades at, from one to one less than the unit.
Avoid: odds, probability (in a trading step)

### exchange
*Proposed.* The value of one market's trading: a book per outcome, every order by id, the
positions, the sets outstanding and the remainder, the last price of each outcome, the sequence
number and the last time; open, closed or final.

### duration
*Proposed.* How long an order may rest: until the market closes (the default), fill what can be
filled at once and cancel the rest, or fill whole at once or not at all.

### sequence number
*Proposed.* The number an exchange gives each trade in turn, from one, by which the order of
trades is fixed.

### maximum order cost
*Proposed.* The most a buy order may cost on an exchange; carried when the exchange opens.

### maximum position
*Proposed.* The most shares of one outcome a player may hold and have resting to buy on an
exchange; carried when the exchange opens.

### cost
*Proposed.* Of a buy order, quantity times price; of a position, what its shares cost: a buy adds
quantity times the trade's price, a split adds an equal part of the unit per share rounded down,
and a sale removes the sold shares' part rounded down.

### sets outstanding
*Proposed.* The complete sets an exchange has split and not merged or paid; the total of shares
held of every outcome, which is the same for each.

### remainder
*Proposed.* What a void leaves in the pool when one unit does not divide equally among the
outcomes; kept and recorded, never paid.

### order
A player's instruction to buy or sell a quantity of shares of one outcome at a price or better.

### buy order
An order to acquire shares, reserving quantity times price of the player's available money.

### sell order
An order to part with shares the player holds, reserving those shares.

### quantity
A whole number of shares an order names or a position holds.

### book
The resting buy and sell orders of one outcome, ordered by price and then by time.
Avoid: order book, market depth

### resting order
An order, or the part of one, that is in the book waiting to be matched.

### trade
One match of a buy and a sell at one price for one quantity. An order may be filled by several
trades.
Avoid: execution

### partial fill
A trade for less than an order's remaining quantity; the rest keeps resting.

### last price
The price of an outcome's most recent trade.

### best bid
The highest price among an outcome's resting buy orders.

### best ask
The lowest price among an outcome's resting sell orders.

### depth
The quantity resting at each price of an outcome's book.

### price stream
The live sequence of trades and price changes of a market that a player holds open.

### expired order
A resting order cancelled by the market's close.

### position
The shares of one outcome of one market that a player holds, with what they cost.
Avoid: holding, portfolio

### complete set
One share of every outcome of a market; what a split makes from one unit and a merge turns back
into one.

### split
Buying a complete set from the market for one unit.

### merge
Selling a complete set back to the market for one unit.

### payout
The money a position is paid when its market resolves.
Avoid: winnings, settlement

## Forecasting

### forecast
A player's choice of outcome and confidence on a market, made before the market's prices are shown
to them.
Avoid: guess, vote

### confidence
How strongly a player holds a forecast, from fifty to one hundred percent.

### crowd probability
The share of forecasters who chose an outcome, shown only above a minimum number of forecasts.
Avoid: consensus, poll

### market probability
An outcome's last price read as a probability.
Avoid: implied probability

### reveal
Showing a player the market probability and the crowd probability after their forecast.

### record
A forecaster's resolved forecasts and how they turned out.

### calibration
How closely a forecaster's confidences matched how often they were right.

### Brier score
The mean squared difference between a forecaster's confidences and the outcomes, lower is better.

## Reasoning

### reasoning graph
The record of questions, hypotheses, evidence, claims and beliefs that ankka-reasoning holds and
this product writes to and reads from.

### hypothesis
One outcome of a market as the reasoning graph holds it: an answer to its question.

### evidence
Something observed from a source, with its locator and excerpt, as the reasoning graph holds it.

### claim
A holder's statement that derives from evidence and supports or contradicts a hypothesis.

### belief
A holder's probability for a hypothesis, revised over time with the claims it rests on.

### holder
Who holds a belief in the reasoning graph: an agent, a person, a model, or the market itself.

### price observation
A market's last price for an outcome, written to the reasoning graph as the market's belief.

## Projects

### project
One ankka project on the installation: `predictions`, which holds the product, or `payments`,
which holds the casino's payments services.

### service
One deployed service of a project, named as the project knows it.

### grant
A project's permission for a named service of another project to call one of its routes; a route
not granted refuses the caller.
Avoid: permission, ACL

### route
One way a service can be called, with its path and its audience.
Avoid: endpoint, URL

### brand
The slug a route carries that names the product to the payments stack; this product's is
`predictions`.

## Libraries

### library
*Proposed.* An artefact this repository publishes for a consumer to build on: a domain or its
components. Four: `exchange-domain`, `exchange-components`, `markets-domain`, `markets-components`.

### domain
*Proposed.* The pure rules of a book or a market: values, commands, refusals and events, reading
nothing outside themselves.

### command
*Proposed.* What is applied to a value to change it: it yields a refusal naming its reason or the
next value and its events.

### event
*Proposed.* What a command led to, applied to a value to yield the next; the journal holds events.

### journal
*Proposed.* The sequence of events a consumer keeps for a book or a market, from which the value
is rebuilt.

### replay
*Proposed.* Applying a journal's events to an empty value in sequence.

### manifest
*Proposed.* The name an event is stored under in a journal, by which a release knows how to apply it.

### release
*Proposed.* A published version of the libraries, from a tag.

### components
*Proposed.* The entities, views and timers written against a domain, which a consumer registers
in its service.

### consumer
*Proposed.* A service that takes the libraries, registers the components and supplies the ports;
first, eitheror's predictions module.

### port
*Proposed.* The seam through which the components reach the outside world; the consumer
implements it.

### ledger port
*Proposed.* The port for money: reserve, release, settle and pay, each keyed; it answers done,
insufficient, or a refusal with its reason.

### gate port
*Proposed.* The port that says whether a player may place an order; it answers allow or a refusal
with its reason.

### resolution port
*Proposed.* The port that says whether a caller may resolve or void a market.

### out of order
*Proposed.* Of a command: carrying a time earlier than the value's last event; refused.

## Everyday words

rebuilt, rebuilds, rebuild, applied, applies, apply, empty, produced, produces, unchanged, takes,
starts, served, registers, longer,
a, an, the, and, or, of, in, on, at, to, for, by, with, from, as, is, are, was, were, has, have,
had, be, been, not, no, any, each, every, one, two, three, ten, first, same, other, another, its,
their, who, whose, that, which, than, before, after, during, until, when, while, now, later,
already, still, again, once, twice, more, less, most, least, all, none, some, only, exactly, at
least, at most, above, below, between, within, over, under, zero, equal, whole, part, rest, new,
same, named, naming, names, refused, refuses, refusal, accepted, accepts, recorded, records, kept,
answered, answers, shown, shows, listed, lists, placed, places, cancelled, cancels, filled, fills,
remaining, opened, opens, closed, closes, suspended, suspends, resumed, resumes, resolved, resolves, voided, voids, approved, approves,
credited, credits, moved, moves, released, releases, paid, pays, held, holds, chosen, chooses,
makes, made, reads, reading, writes, written, asks, asked, says, said, passes, passed, fails,
failed, reaches, reached, cannot, may, must, does, do, did, done, pence, pounds, percent, minute,
minutes, hour, hours, day, days, time, times, order (as in sequence), earlier, latest, oldest,
newest, highest, lowest, total, sum, amount, number, text, reason, because, so, if, then, both,
neither, either, nothing, something, anything, everything, person, people, thing, things, way,
service, product, system,
charged, charges, charging, divide, due, opening, approval, ends, restarts, tells, money, answer, next, carrying, difference, better, directly, through, keyed, drafted, moved, nothing, total, fresh, twenty, revises, revising,
arrives, settles, settled, settle, received, allows, allowed, published, confirmed, confirms, ago, excluded, carries, adjusts, adjusted, type, period, limit, insufficient, folded, folding, cooling, off, withdrawable, week, unavailable, stay, stays, size, reversed, required, previewed, page, month, mismatch, keeps, id, deployed, confirmation, matches, kind, granted, filed, files, outcome (as the word of a payment or a hold step), cancelled, refunded, erased,
active, change, changes, changed, comes, came, correction, corrects, corrected, current, date, end, expected, expires, expired, fill, fillable, fresh, hypotheses, offers, outstanding, play, review, revising, revises, sold, soonest, state, status, stream, window, yet, venue, final, gate, sets, splits, merges, split, merged, bet, cancel, voids, approves, drafts, resolves, lists, reads, holds, shows, closes, pays, make, saying, asking, asks, naming

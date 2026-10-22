# Prediction Market + Reasoning Graph
## Product and Interface Concept

## 1. Product Thesis

Most prediction markets are designed like financial trading products.

They present users with:

- market prices
- percentages
- charts
- order books
- volume
- spreads
- portfolios
- positions
- settlement rules
- trading terminology

That makes sense for sophisticated users who naturally think in terms such as:

> “I think this market is mispriced at 37%.”

But many people do not think that way.

They think:

> “No chance.”

> “I reckon that happens.”

> “Everyone seems convinced, but I don’t buy it.”

> “I think they’re underestimating this.”

The opportunity is therefore not simply to build a cleaner prediction-market interface.

It is to build a product around a much more basic human behaviour:

> **What do you think is going to happen?**

The market machinery can exist underneath that interaction without being the primary user experience.

A useful framing is:

> **A product about the future which happens to contain a prediction market.**

The Reasoning Graph strengthens this idea considerably because it allows the platform to represent not just **what the market believes**, but **why beliefs exist, how they change, what evidence supports them, and what would cause them to change again**.

---

# 2. Prediction Markets as Collective Belief

A conventional prediction market gives a very useful signal:

> **What probability is the crowd currently assigning to an outcome?**

For example:

**Will Company X release Product Y before 31 December?**

Market:

- YES: 42%
- NO: 58%

That tells us something important.

But it does not tell us:

- why the market believes this
- what evidence has driven the price
- whether participants are reacting to the same evidence
- which assumptions matter most
- what evidence supports YES
- what evidence supports NO
- what changed when the probability moved
- what would cause the probability to move again
- whether the market is ignoring relevant evidence
- whether the crowd and a reasoning system disagree
- whether disagreement is based on different evidence or different interpretation

This is where the Reasoning Graph becomes useful.

---

# 3. The Role of the Reasoning Graph

The Reasoning Graph can sit alongside the financial market and represent the reasoning surrounding the market.

A simple conceptual flow is:

```text
Events
  ↓
Evidence
  ↓
Facts / Claims
  ↓
Hypotheses / Beliefs
  ↓
Predictions
  ↓
Market Positions
  ↓
Outcome
  ↓
New Evidence
```

This creates a persistent, inspectable model of how beliefs evolve.

The prediction market becomes one expression of collective belief.

The Reasoning Graph becomes the explanation and history of that belief.

---

# 4. Market Probability vs Reasoned Belief

A particularly useful concept is to keep two distinct values.

## Market probability

The probability implied by trading activity.

For example:

```text
Market probability: 38%
```

This answers:

> What does the crowd currently price this outcome at?

## Reasoned belief

A probability or confidence produced by an agent, analyst, model, or reasoning process based on available evidence.

For example:

```text
Reasoning system belief: 57%
```

This answers:

> Given the evidence currently available to us, what should we believe?

These values do not need to agree.

In fact, disagreement is one of the most interesting things the platform can expose.

```text
Market:     38%
Reasoning:  57%
Difference: +19 percentage points
```

The important question then becomes:

> **Why is there a disagreement?**

That is substantially more interesting than merely showing a chart.

---

# 5. Reasoning Objects

A market could be linked to first-class Reasoning Graph objects.

For example:

```text
Market
 ├── Question
 ├── Resolution Criteria
 ├── Outcome Options
 ├── Evidence
 ├── Facts
 ├── Claims
 ├── Hypotheses
 ├── Beliefs
 ├── Predictions
 ├── Agents
 ├── Participants
 ├── Events
 └── Outcome
```

Possible entities include the following.

## Market

The financial market itself.

Example:

```text
Will SpaceX land humans on Mars before 1 January 2035?
```

Properties might include:

- opening date
- closing date
- resolution date
- outcomes
- current market probability
- liquidity
- volume
- resolution authority
- resolution criteria

## Evidence

An externally observable piece of information.

Examples:

- a company announcement
- an official government release
- polling data
- an earnings report
- a product launch
- a scientific paper
- a sports injury announcement
- satellite data
- legislation
- a court judgement

Evidence should retain provenance.

For example:

```text
Evidence:
SpaceX announces successful long-duration Mars-transfer test

Source:
Official SpaceX announcement

Observed:
12 May 2031
```

## Claims

Statements derived from or supported by evidence.

Example:

```text
SpaceX has demonstrated the life-support capability
required for a multi-month crewed mission.
```

## Hypotheses

Competing explanations or possible future states.

For example:

```text
H1:
A crewed Mars mission will launch before 2035.

H2:
Technical delays will push the first crewed mission beyond 2035.

H3:
Technical readiness will exist, but regulatory or political factors
will prevent a mission before 2035.
```

## Beliefs

A reasoning participant's current confidence in a hypothesis.

For example:

```text
Agent A:
H1 = 0.61

Agent B:
H1 = 0.43

Market:
H1 = 0.48
```

## Events

Things that cause the reasoning state to change.

Example:

```text
2029-06-14
NASA publishes updated Mars architecture.

2030-01-10
SpaceX completes orbital refuelling demonstration.

2031-04-03
Launch vehicle suffers major test failure.
```

## Decisions

Actions based on beliefs.

Within a prediction market this could include:

```text
Open YES position

Close YES position

Increase NO position

Create market

Do not trade

Request additional evidence
```

## Outcome

The eventual resolution.

Example:

```text
Outcome:
NO

Resolved:
1 January 2035
```

Once the outcome is known, the system can evaluate not only who made money, but also which reasoning processes were useful.

---

# 6. A Market as a Living Reasoning Object

Rather than treating a market as a static financial contract, it can be treated as a living reasoning object.

For example:

```text
Market:
Will the UK enter recession in 2028?
```

The graph might contain:

```text
                    Market
                      │
        ┌─────────────┴─────────────┐
        │                           │
     YES belief                  NO belief
        │                           │
   ┌────┴────┐                 ┌────┴────┐
   │         │                 │         │
Evidence A Evidence B      Evidence C Evidence D
```

Evidence may support or contradict particular claims.

Claims may support or contradict hypotheses.

Different agents may assign different importance to evidence.

This allows the product to represent genuine disagreement rather than collapsing everything into a single number.

---

# 7. Explaining Market Movement

One of the most useful applications of the Reasoning Graph is explaining why a market moved.

A conventional interface might show:

```text
42% → 61%
```

That tells the user what happened.

The reasoning system could explain:

> The probability increased from 42% to 61% after three new pieces of evidence appeared.

For example:

1. the regulator approved the key licence
2. the company moved its launch schedule forward
3. a major supplier confirmed production capacity

The user could then ask:

> Why did this market move?

And receive an evidence-backed explanation.

More importantly, because the graph is event-sourced, the system can reconstruct what was believed at any historical point.

Questions might include:

> What did the market believe last Tuesday?

> What did our reasoning system believe?

> What evidence was available at that point?

> What changed on Wednesday?

> Which hypothesis gained confidence?

> Which piece of evidence caused the largest revision?

> Did that revision ultimately prove justified?

This creates something closer to a historical record of collective reasoning.

---

# 8. Evidence Provenance

The platform should retain the provenance of every important piece of evidence.

A useful evidence record might include:

```text
Evidence ID
Source
Author
Publication date
Observed date
Original content
Extracted claim
Reliability
Relevant markets
Supporting hypotheses
Contradicting hypotheses
Agent interpretation
Human review state
```

This is important because a Reasoning Graph should not merely contain conclusions.

It should allow someone to trace:

```text
Prediction
   ↓
Belief
   ↓
Claim
   ↓
Evidence
   ↓
Source
```

That makes explanations auditable.

---

# 9. Competing Reasoning

The system should not try to force a single canonical explanation.

Instead it can preserve competing views.

For example:

```text
Hypothesis A:
The company will launch before December.

Supporting evidence:
- manufacturing complete
- regulatory filing submitted
- marketing campaign scheduled

Contrary evidence:
- previous launch delays
- unresolved supplier issue
```

Meanwhile:

```text
Hypothesis B:
The launch will slip into next year.

Supporting evidence:
- supplier issue unresolved
- regulatory approval historically takes six months
- company has repeatedly missed internal dates
```

Users could therefore ask:

> Make the strongest case for YES.

or:

> Make the strongest case for NO.

or:

> What evidence would most weaken the YES case?

or:

> What would have to happen for NO to become the dominant belief?

This is much more valuable than an LLM simply inventing a plausible argument at query time.

The answer can be grounded in persistent reasoning state.

---

# 10. Prediction Calibration

The Reasoning Graph also makes it possible to evaluate the quality of forecasters.

For every participant or agent, the system can store:

```text
Prediction
Probability
Timestamp
Evidence available
Outcome
```

Over time this creates a calibration history.

For example:

```text
Technology markets:
72% accuracy

Football:
54% accuracy

Macroeconomics:
68% accuracy
```

More sophisticated measures could include:

- Brier score
- calibration curves
- confidence calibration
- domain-specific accuracy
- performance against market consensus
- performance when disagreeing with consensus
- performance after new evidence
- performance by time horizon

This could become central to user identity.

---

# 11. User Identity: Forecaster Rather Than Trader

Most prediction markets implicitly make the user into a trader.

The account screen therefore tends to emphasise:

```text
Portfolio value
Profit / loss
Open positions
Trading volume
Returns
```

A different product could make the user primarily a **forecaster**.

Example profile:

```text
Trevor

Prediction accuracy: 71%

Technology     ★★★★
Politics       ★★★
Football       ★★
Economics      ★★★★

You predict technology outcomes more accurately
than 89% of active forecasters.

12 predictions currently unresolved.
```

Financial performance can still exist.

But it does not need to be the user's primary identity.

The product becomes partly about answering:

> How good are you at knowing what happens next?

---

# 12. Participation Without Money

A very important design option is allowing prediction before financial participation.

The interaction could be:

```text
Question
   ↓
Prediction
   ↓
Confidence
   ↓
Reveal crowd
   ↓
Track accuracy
   ↓
Optional financial position
```

This produces a much wider funnel than:

```text
Deposit
   ↓
Understand contract pricing
   ↓
Select market
   ↓
Place trade
```

Users could therefore participate for free and gradually build reputation.

Eventually the system might say:

> You have correctly predicted 8 of the last 10 AI markets.

> On this question you disagree strongly with the market.

> The market currently prices YES at 31%.

> Do you want to back your prediction?

This creates a much more natural transition into financial participation.

---

# 13. The Core Consumer Interface

The strongest consumer interface may be extremely simple.

Instead of presenting dozens of markets and charts, present one unresolved question.

Example:

```text
WILL HUMANS LAND ON MARS BEFORE 2035?

[large visual]

What do you think?

        YES            NO
```

The user chooses NO.

Then:

```text
How strongly do you feel?

Just a hunch ───────────── Absolutely certain
```

The user selects a confidence level.

Only then does the application reveal the market.

```text
You said:

NO — 75% confidence

The market currently thinks:

YES — 42%
NO  — 58%
```

Then:

```text
You disagree with the market.

£5 on NO would return £8.62 if you're right.

[Back yourself with £5]
```

The interaction begins with belief rather than finance.

---

# 14. Hide the Market Before the User Predicts

This could be an important differentiator.

If the application immediately shows:

```text
YES 63%
NO  37%
```

the user is already anchored by the crowd.

Instead, ask for their view first.

Example:

```text
Will X happen before December?

YES
NO
```

The user selects NO.

Then reveal:

```text
67% of the market disagrees with you.
```

This produces psychological tension.

The next question becomes:

> Do you think everyone else is wrong?

That is much more intuitive than:

```text
Buy NO @ 33p
```

For sophisticated users there could still be a preference:

```text
Show market probabilities before prediction
```

But the default consumer experience could deliberately avoid anchoring.

---

# 15. Feed-Based Interaction

The product could feel more like a content feed than an exchange.

Not necessarily visually like TikTok, but interactionally.

One market fills most of the screen.

Example:

```text
Will Newcastle finish in the top four?
```

Swipe.

```text
Will OpenAI release its next flagship model before Google?
```

Swipe.

```text
Will UK inflation fall below 2% this year?
```

Swipe.

```text
Will humans return to the Moon before 2030?
```

Possible interaction:

```text
Swipe right  = YES
Swipe left   = NO
Swipe up     = skip
Hold         = explain / tell me more
```

After prediction:

```text
You: YES

Market: 31% YES

You're in the minority.
```

Then:

```text
£1
£5
£10

Back your prediction
```

The user is consuming possible futures rather than browsing financial instruments.

---

# 16. Native Visual Languages for Different Markets

A major opportunity is to avoid forcing every prediction through exactly the same interface.

Different subjects can have different visual representations.

## Sports

Rather than:

```text
Newcastle  22%
Arsenal    48%
Draw       30%
```

show the fixture.

Users can tap the team they expect to win.

## Elections

Use:

- candidate imagery
- constituency maps
- electoral maps
- seat projections
- timelines

## Awards

Present nominees as a gallery.

The user chooses the expected winner.

## Economic Indicators

Rather than asking someone to buy a contract:

```text
UK CPI > 3.0%
```

ask:

> Where do you think inflation will be in December?

Then provide a scale:

```text
0% ─────────●───────── 8%
           3.4%
```

After the user answers:

```text
Your prediction: 3.4%

Market consensus: 2.8%

You are above the market.
```

## League Tables

For a question such as:

> Who will win the Premier League?

Allow users to drag teams into the order they expect.

The system can infer implied probabilities or identify areas where the user differs from market consensus.

The design principle is:

> **Interact with the thing being predicted, not the financial abstraction representing it.**

---

# 17. Conversational / No-Interface Prediction Market

The most radical interface may be conversational.

The user does not browse a market list at all.

They ask:

> What's interesting today?

The system replies:

> Three markets have moved significantly since yesterday.

Or:

> Find me things about AI where I disagree with the market.

Or:

> What changed overnight?

Or:

> Where has the crowd changed its mind?

Or:

> Which of my predictions are now looking wrong?

Or:

> What do I think that almost nobody else thinks?

The conversational interface can use the Reasoning Graph to produce grounded answers.

---

# 18. Example Conversational Interaction

The assistant might say:

> OpenAI delayed its hardware announcement yesterday.
>
> The market's probability of an October launch fell from 51% to 38%.
>
> You previously predicted an October launch with 70% confidence.

User:

> I still think October.

System:

> Your prediction remains substantially above the market.
>
> £10 on October at the current price would return £26.30 if it resolves YES.
>
> Place £10?

User:

> Yes.

Done.

The exchange exists underneath the conversation.

The user never needs to understand an order book.

---

# 19. Reasoning Graph + Conversational Interface

This is where the Reasoning Graph becomes particularly powerful.

The user can ask:

> Why did the market move?

The system can inspect:

```text
Market movement
   ↓
Recent events
   ↓
New evidence
   ↓
Affected beliefs
```

and answer:

> Three pieces of evidence appear to explain most of the movement:
>
> 1. the launch event was postponed
> 2. the regulator requested additional documentation
> 3. the manufacturing partner reduced its delivery forecast

The user can then ask:

> Is there still a case for YES?

The system traverses the other side of the graph.

It can answer using evidence which supports the competing hypothesis.

---

# 20. "What Would Change Your Mind?"

One particularly strong application is counterfactual reasoning.

The user could ask:

> What would need to happen for this market to move above 70%?

The graph could identify unresolved assumptions.

For example:

```text
Current belief: 52%

Major unresolved dependencies:

- regulatory approval
- successful prototype test
- manufacturing capacity

If regulatory approval occurs:
estimated reasoning confidence rises to ~61%

If approval + prototype test succeed:
estimated reasoning confidence rises to ~74%
```

This turns the product from a passive probability display into an active model of future state.

---

# 21. Push Notifications as Interface

The application may not require frequent active browsing.

Notifications can become part of the interface.

Example:

```text
Newcastle v Arsenal

Isak has been ruled out.

Newcastle's market probability fell:

41% → 29%

You predicted Newcastle yesterday.

Still agree?
```

Buttons:

```text
STILL NEWCASTLE

CHANGE PREDICTION
```

Another example:

```text
Mars before 2035

Probability moved:

38% → 51%

A major SpaceX announcement caused the move.

[See why]
```

This makes the market ambient.

It appears when something relevant to the user's beliefs changes.

---

# 22. Personalised Disagreement

A particularly powerful product feature is surfacing disagreement.

The platform knows:

```text
What the market believes
What the user believes
What reasoning agents believe
```

Therefore it can find interesting gaps.

Example:

> You predict this outcome at 72%.
>
> The market is at 39%.
>
> This is one of your largest disagreements with the crowd.

Or:

> You are usually highly accurate in AI-related markets.
>
> On this question you are substantially more optimistic than the market.

This gives users a reason to return.

The system is effectively asking:

> Where might you know something the crowd does not?

---

# 23. The Future as the Home Screen

Instead of calling the primary navigation item:

```text
Markets
```

call it:

```text
THE FUTURE
```

The interface could be organised temporally.

Example:

```text
TODAY

Champions League
Central bank announcement
Election debate

THIS MONTH

Apple launch
Inflation release
Company earnings

2027

Election
Mars mission test
New console launch

2030+

Moon base
Climate targets
AGI
Mars
```

Markets could appear as events distributed across a visual timeline.

The user explores possible futures rather than a catalogue of contracts.

A conceptual tagline might be:

> **Humanity's continuously updated map of what it thinks will happen next.**

---

# 24. Three Product Modes

One system could support three different interfaces over the same underlying markets.

## Discover

Designed for casual users.

Characteristics:

- highly visual
- question-first
- prediction before price
- swipe-based
- minimal numbers
- little trading terminology
- stories and events
- reputation and forecasting

Example:

```text
What do you think will happen?
```

## Ask

Conversational interface.

Example questions:

```text
What's interesting today?

What changed overnight?

Why did this probability fall?

Where does the market disagree with me?

What am I good at predicting?

Make the case for YES.

What would change this prediction?
```

## Trade

Designed for sophisticated users.

Includes:

- market charts
- order books
- spread
- depth
- liquidity
- limit orders
- portfolio analytics
- APIs
- historical prices
- execution controls

The underlying market is identical.

Only the way it is perceived changes.

---

# 25. Agentic Market Creation

The Reasoning Graph could also support automatic or assisted creation of new markets.

An agent identifies an emerging unresolved question.

For example:

```text
Company X announces product programme.

Potential question:

Will Company X commercially launch Product Y
before 31 December 2028?
```

The agent then constructs:

- market wording
- outcomes
- closing date
- resolution criteria
- authoritative resolution sources
- ambiguity checks
- relevant evidence sources

A human could approve the market before publication.

The Reasoning Graph would then begin accumulating evidence immediately.

---

# 26. Market Resolution

Resolution can also benefit from structured reasoning.

Rather than simply:

```text
YES WON
```

the resolution could retain:

```text
Outcome:
YES

Resolution evidence:
Official product launch announcement

Resolution date:
14 November 2028

Authority:
Company website + regulator filing
```

If resolution is disputed, the system has an audit trail.

The platform can show:

> Why was this market resolved YES?

with traceable evidence.

---

# 27. Agents as Forecasters

Different agents could maintain independent reasoning around the same market.

Example:

```text
Market consensus:        47%

News agent:              58%
Technical agent:         63%
Macroeconomic agent:     39%
Industry specialist:     71%
User:                    65%
```

The important point is not simply averaging them.

The Reasoning Graph retains *why* they differ.

For example:

```text
Technical agent:
high confidence because prototype milestones are ahead of plan

Macroeconomic agent:
low confidence because capital availability is deteriorating
```

The product can expose that disagreement.

---

# 28. Human and Machine Beliefs

The platform therefore becomes a mixture of:

```text
Human predictions
Market prices
AI predictions
Expert predictions
Evidence
Reasoning
Outcomes
```

Over time it becomes possible to compare them.

Questions include:

> Are humans better than agents in this domain?

> Which agents are well calibrated?

> Which evidence sources consistently improve prediction quality?

> Does the market react too slowly to particular evidence?

> Which participants are good at identifying mispriced markets?

> Which users are good at predicting certain classes of event?

This dataset could itself become highly valuable.

---

# 29. A Market as an Experiment

Every resolved market is effectively an experiment.

At the start:

```text
Question
Predictions
Beliefs
Evidence
```

At the end:

```text
Observed outcome
```

Therefore the platform can evaluate reasoning quality.

The Reasoning Graph creates a feedback loop:

```text
Evidence
   ↓
Beliefs
   ↓
Predictions
   ↓
Market
   ↓
Outcome
   ↓
Evaluation
   ↓
Updated reasoning
```

This is extremely close to the broader institutional reasoning model:

```text
Evidence
   ↓
Competing beliefs
   ↓
Decision
   ↓
Outcome
   ↓
New evidence
```

Prediction markets provide a particularly clean environment because the eventual outcome frequently provides an objective test.

---

# 30. The Most Interesting Product Combination

The strongest product may therefore combine three ideas.

## 1. Prediction market

Provides:

```text
Quantified collective belief
```

## 2. Reasoning Graph

Provides:

```text
Structured explanation of belief
```

## 3. Instinctive interface

Provides:

```text
Human interaction with possible futures
```

Together the system becomes something more ambitious than a betting platform.

It becomes:

> **A continuously evolving model of possible futures, what people believe about them, why those beliefs exist, and how those beliefs change as reality unfolds.**

---

# 31. A Possible Product Architecture

A simplified architecture could look like this:

```text
                   ┌─────────────────────┐
                   │     Data Sources    │
                   │ news / APIs / feeds │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │   Evidence Agents   │
                   └──────────┬──────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │   Reasoning Graph   │
                   │                     │
                   │ Events              │
                   │ Evidence            │
                   │ Claims              │
                   │ Hypotheses          │
                   │ Beliefs             │
                   │ Predictions         │
                   └──────────┬──────────┘
                              │
              ┌───────────────┴───────────────┐
              │                               │
              ▼                               ▼
     ┌─────────────────┐             ┌─────────────────┐
     │ Market Engine   │             │ Agent System    │
     │                 │             │                 │
     │ prices          │             │ explanations    │
     │ orders          │             │ forecasting     │
     │ liquidity       │             │ monitoring      │
     │ settlement      │             │ market creation │
     └────────┬────────┘             └────────┬────────┘
              │                               │
              └───────────────┬───────────────┘
                              │
                              ▼
                   ┌─────────────────────┐
                   │     User Layer      │
                   │                     │
                   │ Discover            │
                   │ Ask                 │
                   │ Trade               │
                   │ Notifications       │
                   └─────────────────────┘
```

---

# 32. Suggested MVP

The first prototype should probably avoid building the whole exchange interface.

Instead build the consumer interaction.

A simple mobile prototype could contain twenty example markets.

Each market occupies one screen.

Flow:

```text
1. Show question

2. User chooses outcome

3. Ask confidence

4. Reveal market probability

5. Highlight agreement / disagreement

6. Show concise explanation

7. Offer optional stake

8. Add prediction to user's record
```

Example:

```text
Will Newcastle finish in the top four?

YES
```

Then:

```text
How confident?

70%
```

Then:

```text
Market consensus:

YES 43%

You are more optimistic than the market.
```

Then:

```text
Why does the market disagree?
```

And perhaps:

```text
Back your prediction:

£1
£5
£10
```

This prototype would test the most important assumption:

> Can prediction markets become interesting to people who have no interest in trading interfaces?

---

# 33. Second MVP: Reasoning Layer

The next prototype could add Reasoning Graph behaviour.

Choose five or ten markets.

For each market maintain:

- evidence
- claims
- YES hypothesis
- NO hypothesis
- current reasoning confidence
- market probability
- recent events

Then expose questions such as:

```text
Why did this move?

What supports YES?

What supports NO?

What changed this week?

What would change your mind?

Why does the reasoning system disagree with the market?
```

This would demonstrate the actual differentiation.

---

# 34. Third MVP: Conversational Market

A further experiment could remove most of the traditional UI entirely.

The opening screen contains:

```text
Ask about the future...
```

Possible queries:

```text
What's interesting today?

Anything changed in AI?

What predictions am I getting wrong?

Where do I disagree with everyone?

What could happen this weekend?

Show me markets about Newcastle.

What changed overnight?
```

The assistant uses:

```text
market data
+
reasoning graph
+
user prediction history
```

to construct answers.

Financial positions appear only when relevant.

---

# 35. Key Design Principles

The system should follow a small number of strong principles.

## Ask for belief before showing price

Avoid anchoring wherever possible.

## Make the subject the interface

Football should look like football.

Politics should look like politics.

Technology should look like technology.

Do not force everything into a generic trading table.

## Treat users as forecasters first

Money is optional.

Prediction and reputation can exist independently.

## Preserve disagreement

Do not collapse competing reasoning into one explanation.

## Show why probabilities change

Movement without explanation is only half the information.

## Keep evidence traceable

Every meaningful claim should be connected back to provenance.

## Make reasoning historical

Users should be able to reconstruct:

```text
What did we believe then?

Why?

What changed?

Were we right?
```

## Let complexity appear gradually

The casual user may see:

```text
YES / NO
```

The expert may eventually reach:

```text
order book
liquidity
Bayesian evidence
forecast calibration
agent reasoning
```

Both can use the same underlying market.

---

# 36. Final Product Framing

A conventional prediction market says:

> **Trade on what happens next.**

A more interesting consumer product could say:

> **What do you think happens next?**

The Reasoning Graph lets the platform go further:

> **Why do you think that?**

Then:

> **What does everyone else think?**

Then:

> **Why do they disagree?**

Then:

> **What changed?**

And eventually:

> **Do you want to back your belief?**

That progression may provide a much more natural way of introducing people to prediction markets.

The end product is therefore not simply an exchange.

It is a system for exploring:

- possible futures
- human beliefs
- machine beliefs
- disagreement
- evidence
- uncertainty
- changing expectations
- eventual outcomes

The market gives those beliefs a number.

The Reasoning Graph gives them an explanation.

The interface makes them understandable.

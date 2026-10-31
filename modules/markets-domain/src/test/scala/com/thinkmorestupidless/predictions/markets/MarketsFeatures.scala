package com.thinkmorestupidless.predictions.markets

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

import java.time.{Duration, Instant}

/** `features/markets/`: every scenario run against the value alone (spec 006 SC-001). */
class MarketsFeatures extends GherkinSuite("features/markets"):

  private var world = MarketsWorld()

  override def beforeEach(context: BeforeEach): Unit = world = MarketsWorld()

  private def t(s: String): Instant = Instant.parse(s)
  private def op(name: String)      = OperatorId(name)
  private def ev(source: String, locator: String, excerpt: String) =
    Vector(Evidence(source, locator, excerpt))
  private def definition(id: String) = world.value(id).definition.get

  // ── the Background ──────────────────────────────────────────────────────────

  Given("an operator {string}")((name: String) => world.operator = op(name))
  Given("the review window is {string} hours") { (hours: String) =>
    world.window = Duration.ofHours(hours.toLong)
  }

  // ── seeds ───────────────────────────────────────────────────────────────────

  Given("a draft market {string}")((id: String) => world.drafted(id, world.definition()))
  Given("a draft market {string} with no open time") { (id: String) =>
    world.drafted(id, world.definition())
  }
  Given("a draft market {string} opening at {string}") { (id: String, at: String) =>
    world.drafted(id, world.definition(openTime = Some(t(at))))
  }
  Given("a draft market {string} closing at {string}") { (id: String, at: String) =>
    world.drafted(id, world.definition(close = t(at)))
  }
  Given("a draft market {string} asking {string}") { (id: String, question: String) =>
    world.drafted(id, world.definition(question = question))
  }
  Given("a draft market {string} drafted by {string} at {string}") {
    (id: String, by: String, at: String) => world.drafted(id, world.definition(), t(at), op(by))
  }
  Given("{string} approved the market {string}") { (by: String, id: String) =>
    world.seed(id, MarketCommand.Approve(op(by), world.now))
  }
  Given("an approved market {string}") { (id: String) =>
    world.approved(id, world.definition(openTime = Some(world.now.plus(Duration.ofDays(10)))))
  }
  Given("an approved market {string} asking {string}") { (id: String, question: String) =>
    world.approved(
      id,
      world.definition(openTime = Some(world.now.plus(Duration.ofDays(10))), question = question)
    )
  }
  Given("an approved market {string} opening at {string}") { (id: String, at: String) =>
    world.approved(id, world.definition(openTime = Some(t(at))))
  }
  Given("an approved market {string} opening at {string} and closing at {string}") {
    (id: String, opens: String, closes: String) =>
      world.approved(id, world.definition(close = t(closes), openTime = Some(t(opens))))
  }
  Given("an open market {string} closing at {string}") { (id: String, at: String) =>
    world.approved(id, world.definition(close = t(at)))
  }
  Given("an open market {string} with the outcomes {string} and {string}") {
    (id: String, a: String, b: String) =>
      world.approved(id, world.definition(outcomes = Vector(a, b)))
  }
  Given("a suspended market {string} closing at {string}") { (id: String, at: String) =>
    world.approved(id, world.definition(close = t(at)))
    world.seed(id, MarketCommand.Suspend(world.ops, "seeded", world.seeded.plusSeconds(2)))
  }
  Given("a closed market {string} with the outcomes {string} and {string}") {
    (id: String, a: String, b: String) =>
      world.closed(id, world.now, c => world.definition(close = c, outcomes = Vector(a, b)))
  }
  Given(
    "a closed market {string} with the outcomes {string} and {string} and the authority {string}"
  ) { (id: String, a: String, b: String, authority: String) =>
    world.closed(
      id,
      world.now,
      c => world.definition(close = c, outcomes = Vector(a, b), authority = authority)
    )
  }
  Given("a resolved market {string} resolved to {string} {string} hours ago") {
    (id: String, outcome: String, hours: String) =>
      world.resolved(id, outcome, world.now.minus(Duration.ofHours(hours.toLong)))
  }
  Given("a resolved market {string} resolved to {string} at {string}") {
    (id: String, outcome: String, at: String) => world.resolved(id, outcome, t(at))
  }
  Given("a void market {string} voided at {string}") { (id: String, at: String) =>
    val voided = t(at)
    world.approved(
      id,
      world.definition(close = voided.plus(Duration.ofDays(30))),
      voided.minus(Duration.ofDays(60))
    )
    world.seed(
      id,
      MarketCommand.Void(world.ops, "seeded", Vector(world.evidence), world.window, voided)
    )
  }
  Given("a final market {string}") { (id: String) =>
    world.resolved(id, "yes", world.now.minus(Duration.ofHours(48)))
    world.seed(id, MarketCommand.Finalise(world.now.minus(Duration.ofHours(24))))
  }

  // ── drafting and changing ───────────────────────────────────────────────────

  private def draft(by: String, id: String, d: Definition) =
    world.apply(id, MarketCommand.Draft(MarketId(id), op(by), d, world.now)): Unit

  When(
    "{string} drafts a market {string} asking {string} in the category \"sport\" with the outcomes \"yes\" and \"no\" in \"GBP\" closing at {string} with the resolution criteria {string} and the authority {string}"
  ) {
    (
        by: String,
        id: String,
        question: String,
        close: String,
        criteria: String,
        authority: String
    ) =>
      draft(
        by,
        id,
        world.definition(
          close = t(close),
          question = question,
          criteria = criteria,
          authority = authority
        )
      )
  }
  When("{string} drafts a market {string} with the outcomes {string}") {
    (by: String, id: String, a: String) => draft(by, id, world.definition(outcomes = Vector(a)))
  }
  When("{string} drafts a market {string} with the outcomes {string} and {string}") {
    (by: String, id: String, a: String, b: String) =>
      draft(by, id, world.definition(outcomes = Vector(a, b)))
  }
  When("{string} drafts a market {string} with {string} outcomes") {
    (by: String, id: String, n: String) =>
      draft(by, id, world.definition(outcomes = (1 to n.toInt).map(i => s"o-$i").toVector))
  }
  When("{string} drafts a market {string} opening at {string} and closing at {string}") {
    (by: String, id: String, opens: String, closes: String) =>
      draft(by, id, world.definition(close = t(closes), openTime = Some(t(opens))))
  }
  When(
    "{string} drafts a market {string} closing at {string} with the expected resolution date {string}"
  ) { (by: String, id: String, close: String, date: String) =>
    draft(by, id, world.definition(close = t(close)).copy(expectedResolution = world.date(date)))
  }
  When("{string} drafts a market {string} asking {string}") {
    (by: String, id: String, question: String) =>
      draft(by, id, world.definition(question = question))
  }
  When("{string} changes the question of the market {string} to {string}") {
    (by: String, id: String, question: String) =>
      world.apply(
        id,
        MarketCommand.Change(op(by), definition(id).copy(question = question), world.now)
      ): Unit
  }
  When("{string} changes the resolution criteria of the market {string} to {string}") {
    (by: String, id: String, criteria: String) =>
      world.apply(
        id,
        MarketCommand.Change(op(by), definition(id).copy(criteria = criteria), world.now)
      ): Unit
  }

  // ── opening, closing, suspending ────────────────────────────────────────────

  When("{string} approves the market {string}") { (by: String, id: String) =>
    world.apply(id, MarketCommand.Approve(op(by), world.now)): Unit
  }
  When("{string} approves the market {string} at {string}") { (by: String, id: String, at: String) =>
    world.apply(id, MarketCommand.Approve(op(by), t(at))): Unit
  }
  When("the market {string} is opened at {string}") { (id: String, at: String) =>
    world.apply(id, MarketCommand.Open(t(at))): Unit
  }
  When("the time {string} comes")((at: String) => world.comes(world.current, t(at)): Unit)
  When("the market {string} is read")((id: String) => world.current = id)
  When("{string} moves the close of the market {string} to {string} with the reason {string}") {
    (by: String, id: String, close: String, reason: String) =>
      world.apply(id, MarketCommand.MoveClose(op(by), t(close), reason, world.now)): Unit
  }
  When("{string} suspends the market {string} with the reason {string}") {
    (by: String, id: String, reason: String) =>
      world.apply(id, MarketCommand.Suspend(op(by), reason, world.now)): Unit
  }
  When("{string} resumes the market {string}") { (by: String, id: String) =>
    world.apply(id, MarketCommand.Resume(op(by), world.now)): Unit
  }
  When("{string} closes the market {string} with the reason {string}") {
    (by: String, id: String, reason: String) =>
      world.apply(id, MarketCommand.Close(Some(op(by)), Some(reason), world.now)): Unit
  }
  When("{string} closes the market {string} at {string}") { (by: String, id: String, at: String) =>
    world.apply(id, MarketCommand.Close(Some(op(by)), None, t(at))): Unit
  }

  // ── resolving, correcting, voiding ──────────────────────────────────────────

  private var offered: Vector[Evidence] = Vector.empty

  private def resolve(by: String, id: String, outcome: String, evidence: Vector[Evidence]) =
    offered = evidence
    world.apply(
      id,
      MarketCommand.Resolve(
        op(by),
        OutcomeId(outcome),
        evidence,
        definition(id).authority,
        world.window,
        world.now
      )
    ): Unit

  When(
    "{string} resolves the market {string} to {string} on the evidence {string} at {string} saying {string}"
  ) { (by: String, id: String, outcome: String, source: String, locator: String, excerpt: String) =>
    resolve(by, id, outcome, ev(source, locator, excerpt))
  }
  When("{string} resolves the market {string} to {string} on no evidence") {
    (by: String, id: String, outcome: String) => resolve(by, id, outcome, Vector.empty)
  }
  When(
    "{string} corrects the resolution of {string} to {string} on the evidence {string} at {string} saying {string}"
  ) { (by: String, id: String, outcome: String, source: String, locator: String, excerpt: String) =>
    world.apply(
      id,
      MarketCommand.Correct(
        op(by),
        OutcomeId(outcome),
        ev(source, locator, excerpt),
        world.window,
        world.now
      )
    ): Unit
  }
  // Seven values, one over the runner's six: the evidence of the one scenario is in the text (R10).
  When(
    "{string} corrects the resolution of {string} to {string} on the evidence \"premierleague.com\" at \"https:\\/\\/premierleague.com\\/tables\" saying {string} at {string}"
  ) { (by: String, id: String, outcome: String, excerpt: String, at: String) =>
    world.apply(
      id,
      MarketCommand.Correct(
        op(by),
        OutcomeId(outcome),
        ev("premierleague.com", "https://premierleague.com/tables", excerpt),
        world.window,
        t(at)
      )
    ): Unit
  }
  When(
    "{string} voids the market {string} with the reason {string} on the evidence {string} at {string} saying {string}"
  ) { (by: String, id: String, reason: String, source: String, locator: String, excerpt: String) =>
    world.apply(
      id,
      MarketCommand.Void(op(by), reason, ev(source, locator, excerpt), world.window, world.now)
    ): Unit
  }
  When("the market {string} is made final at {string}") { (id: String, at: String) =>
    world.apply(id, MarketCommand.Finalise(t(at))): Unit
  }

  // ── outcomes ────────────────────────────────────────────────────────────────

  private def isIn(id: String, state: String): Unit =
    assertEquals(MarketState.word(world.value(id).state), state)

  Then("the market {string} is a {word} market")(isIn)
  Then("the market {string} is an {word} market")(isIn)

  private def naming(name: String): Unit =
    val r = world.refusal
    assert(r.reason == name || r.names.get("field").contains(name), s"$r does not name the $name")

  Then("the {word} is refused naming the {word}")((_: String, name: String) => naming(name))
  Then("the {word} is refused naming the expected resolution date") { (_: String) =>
    naming("expected resolution date")
  }
  Then("the {word} is refused naming the open time")((_: String) => naming("open time"))
  Then("the {word} is refused naming the review window")((_: String) => naming("review window"))
  Then("the {word} is refused naming the state {string}") { (_: String, state: String) =>
    assertEquals(world.refusal, Refusal("state", Map("state" -> state)))
  }
  Then("the {word} is refused as no change") { (_: String) =>
    assertEquals(world.refusal, Refusals.noChange)
  }
  Then("the {word} is refused as out of order") { (_: String) =>
    assertEquals(world.refusal.reason, "out of order")
  }

  Then("the market {string} asks {string}") { (id: String, question: String) =>
    assertEquals(definition(id).question, question)
  }
  Then("the market {string} has the resolution criteria {string}") { (id: String, c: String) =>
    assertEquals(definition(id).criteria, c)
  }
  Then("the market {string} closes at {string}") { (id: String, at: String) =>
    assertEquals(definition(id).close, t(at))
  }
  Then("the market {string} records the reason {string}") { (id: String, reason: String) =>
    assert(world.value(id).record.exists(_.reason.contains(reason)), world.value(id).record)
  }
  Then(
    "the resolution of {string} names the outcome {string}, the evidence and the authority {string}"
  ) { (id: String, outcome: String, authority: String) =>
    val r = world.value(id).resolution.get
    assertEquals((r.outcome, r.evidence, r.authority), (OutcomeId(outcome), offered, authority))
  }
  Then("the resolution of {string} names the outcome {string}") { (id: String, outcome: String) =>
    assertEquals(world.value(id).resolution.map(_.outcome), Some(OutcomeId(outcome)))
  }
  Then("the resolution of {string} revises the first") { (id: String) =>
    val rs = world.value(id).resolutions
    assertEquals(rs.size, 2)
    assertNotEquals(rs(0).outcome, rs(1).outcome)
  }
  Then("the review window of {string} ends at {string}") { (id: String, at: String) =>
    assertEquals(world.value(id).resolution.map(_.windowEnd), Some(t(at)))
  }
  Then("the market {string} shows {word} by {string} at {string}") {
    (id: String, what: String, by: String, at: String) =>
      val shown = world.value(id).record
      assert(
        shown.exists(e => e.what == what && e.operator.contains(op(by)) && e.at == t(at)),
        shown
      )
  }
  Then("the market {string} is next due at {string}") { (id: String, at: String) =>
    assertEquals(world.value(id).nextDue, Some(t(at)))
  }

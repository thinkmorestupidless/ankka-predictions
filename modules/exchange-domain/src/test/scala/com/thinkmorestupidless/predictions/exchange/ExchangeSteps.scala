package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.testkit.GherkinSuite

/**
 * The step definitions the exchange's features share, against the value alone: no runtime, no clock
 * (research R10). The runner matches a step by its text whatever its keyword (V8), so a text is
 * bound once; "holds" gives a holding before the scenario's first When and checks one after.
 */
abstract class ExchangeSteps(features: String) extends GherkinSuite(features):

  protected var world: ExchangeWorld = ExchangeWorld()

  override def beforeEach(context: BeforeEach): Unit = world = ExchangeWorld()

  protected def n(text: String): Long = text.trim.toLong

  protected def refusal: Refusal = world.last match
    case Some(Left(r)) => r
    case other         => fail(s"the last command was not refused: $other")

  protected def accepted(answer: Either[Refusal, Decision], what: String): Unit =
    answer.left.foreach(r => fail(s"$what was refused: $r"))

  private def order(market: String, id: String): Order =
    world.value(market).orders.getOrElse(OrderId(id), fail(s"no order $id in $market"))

  private def position(market: String, owner: String, outcome: String): Position =
    world.value(market).position(OwnerId(owner), OutcomeId(outcome))

  /** A place whose quantity is read as text by the domain's own rule (R10). */
  private def placing(
      owner: String,
      side: Side,
      id: String,
      quantity: String,
      outcome: String,
      market: String,
      price: String,
      duration: OrderDuration
  ): Either[Refusal, Decision] =
    Quantity.of(quantity) match
      case Left(r) =>
        world.last = Some(Left(r))
        Left(r)
      case Right(q) => world.place(market, owner, side, id, q, outcome, n(price), duration)

  // ── exchanges ───────────────────────────────────────────────────────────────

  Given("an open exchange {string} with the outcomes {string} and {string} and a unit of {string}") {
    (m: String, a: String, b: String, unit: String) => world.open(m, Vector(a, b), n(unit))
  }

  Given(
    "an open exchange {string} with the outcomes {string}, {string} and {string} and a unit of {string}"
  ) { (m: String, a: String, b: String, c: String, unit: String) =>
    world.open(m, Vector(a, b, c), n(unit))
  }

  Given(
    "a closed exchange {string} with the outcomes {string} and {string} and a unit of {string}"
  ) { (m: String, a: String, b: String, unit: String) =>
    world.open(m, Vector(a, b), n(unit))
    accepted(world.apply(m, ExchangeCommand.Close(world.tick())), s"closing $m")
  }

  Given(
    "a suspended exchange {string} with the outcomes {string} and {string} and a unit of {string}"
  ) { (m: String, a: String, b: String, unit: String) =>
    world.open(m, Vector(a, b), n(unit))
    accepted(world.apply(m, ExchangeCommand.Suspend(world.tick())), s"suspending $m")
  }

  Given("the maximum order cost of {string} is {string}") { (m: String, limit: String) =>
    world.reopen(m, maxOrderCost = Some(n(limit)))
  }

  Given("the maximum position of {string} is {string}") { (m: String, limit: String) =>
    world.reopen(m, maxPosition = Some(n(limit)))
  }

  Given("the exchange {string} is suspended") { (m: String) =>
    accepted(world.apply(m, ExchangeCommand.Suspend(world.tick())), s"suspending $m")
  }

  When("the exchange {string} resumes") { (m: String) =>
    world.acted = true
    accepted(world.apply(m, ExchangeCommand.Resume(world.tick())), s"resuming $m")
  }

  When("the exchange {string} closes") { (m: String) =>
    world.acted = true
    world.apply(m, ExchangeCommand.Close(world.tick())): Unit
  }

  Given("the exchange {string} closed") { (m: String) =>
    accepted(world.apply(m, ExchangeCommand.Close(world.tick())), s"closing $m")
  }

  When("the exchange {string} is made final resolved to {string}") { (m: String, o: String) =>
    world.acted = true
    world.apply(m, ExchangeCommand.Finalise(Finality.Resolved(OutcomeId(o)), world.tick())): Unit
  }

  When("the exchange {string} is made final resolved to {string} again") { (m: String, o: String) =>
    world.acted = true
    world.apply(m, ExchangeCommand.Finalise(Finality.Resolved(OutcomeId(o)), world.tick())): Unit
  }

  When("the exchange {string} is made final void") { (m: String) =>
    world.acted = true
    world.apply(m, ExchangeCommand.Finalise(Finality.Void, world.tick())): Unit
  }

  // ── holdings, orders, sets ──────────────────────────────────────────────────

  Given("{string} holds {string} shares of {string} in {string}") {
    (owner: String, shares: String, outcome: String, m: String) =>
      if world.acted then assertEquals(position(m, owner, outcome).quantity, n(shares))
      else world.seedHolding(m, owner, n(shares))
  }

  Given("{string} holds {string} shares of {string} in {string} at a cost of {string}") {
    (owner: String, shares: String, outcome: String, m: String, cost: String) =>
      if !world.acted then world.seedHolding(m, owner, n(shares))
      val p = position(m, owner, outcome)
      assertEquals((p.quantity, p.cost), (n(shares), n(cost)))
  }

  // The runner takes six values at most, so the side is in the text: one definition per side.
  for (word, sd) <- Vector("buy" -> Side.Buy, "sell" -> Side.Sell) do
    val placeText =
      s"{string} places a $word order {string} for {string} shares of {string} in {string} at price {string}"

    When(placeText) {
      (owner: String, id: String, q: String, outcome: String, m: String, price: String) =>
        world.acted = true
        placing(owner, sd, id, q, outcome, m, price, OrderDuration.RestUntilClose): Unit
    }

    When(s"$placeText to fill at once and cancel the rest") {
      (owner: String, id: String, q: String, outcome: String, m: String, price: String) =>
        world.acted = true
        placing(owner, sd, id, q, outcome, m, price, OrderDuration.FillAtOnce): Unit
    }

    When(s"$placeText to fill whole or not at all") {
      (owner: String, id: String, q: String, outcome: String, m: String, price: String) =>
        world.acted = true
        placing(owner, sd, id, q, outcome, m, price, OrderDuration.FillWhole): Unit
    }

    Given(
      s"{string} placed a $word order {string} for {string} shares of {string} in {string} at price {string}"
    ) { (owner: String, id: String, q: String, outcome: String, m: String, price: String) =>
      accepted(
        placing(owner, sd, id, q, outcome, m, price, OrderDuration.RestUntilClose),
        s"order $id"
      )
    }

  When("{string} cancels the order {string}") { (owner: String, id: String) =>
    world.acted = true
    world.apply(
      cancelling(id),
      ExchangeCommand.Cancel(OrderId(id), OwnerId(owner), world.tick())
    ): Unit
  }

  Given("{string} cancelled the order {string}") { (owner: String, id: String) =>
    accepted(
      world
        .apply(cancelling(id), ExchangeCommand.Cancel(OrderId(id), OwnerId(owner), world.tick())),
      s"cancelling $id"
    )
  }

  /** The market an order was placed in: every cancel in the features is of an order in `m-1`. */
  private def cancelling(id: String): String = "m-1"

  When("{string} splits {string} sets in {string} under the key {string}") {
    (owner: String, sets: String, m: String, key: String) =>
      world.acted = true
      setMove(m, ExchangeCommand.Split(SetKey(key), OwnerId(owner), n(sets), world.tick()), key)
  }

  When("{string} merges {string} sets in {string} under the key {string}") {
    (owner: String, sets: String, m: String, key: String) =>
      world.acted = true
      setMove(m, ExchangeCommand.Merge(SetKey(key), OwnerId(owner), n(sets), world.tick()), key)
  }

  Given("{string} split {string} sets in {string}") { (owner: String, sets: String, m: String) =>
    val key = world.nextSeedKey("seed-split")
    accepted(
      world.apply(m, ExchangeCommand.Split(SetKey(key), OwnerId(owner), n(sets), world.tick())),
      s"$owner's split"
    )
  }

  Given("{string} split {string} sets in {string} under the key {string}") {
    (owner: String, sets: String, m: String, key: String) =>
      accepted(
        setMove(m, ExchangeCommand.Split(SetKey(key), OwnerId(owner), n(sets), world.tick()), key),
        s"$owner's split"
      )
  }

  private def setMove(m: String, c: ExchangeCommand, key: String): Either[Refusal, Decision] =
    val answer = world.apply(m, c)
    world.first(key, answer)
    answer

  Given("{string} sold {string} shares of {string} in {string} to {string} at price {string}") {
    (seller: String, shares: String, outcome: String, m: String, buyer: String, price: String) =>
      world.sold(m, seller, n(shares), outcome, buyer, n(price))
  }

  // ── reads ───────────────────────────────────────────────────────────────────

  When("the pool of {string} is read")((_: String) => world.acted = true)
  When("the positions of {string} are read")((_: String) => world.acted = true)
  When("the position of {string} in {string} of {string} is read") {
    (_: String, _: String, _: String) =>
      world.acted = true
  }
  When("the prices of {string} in {string} are read")((_: String, _: String) => world.acted = true)
  When("the depth of {string} in {string} is read")((_: String, _: String) => world.acted = true)

  // ── outcomes ────────────────────────────────────────────────────────────────

  Then(
    "the order {string} rests in the book of {string} in {string} at price {string} for {string} shares"
  ) { (id: String, outcome: String, m: String, price: String, shares: String) =>
    val book = world.value(m).books.getOrElse(OutcomeId(outcome), fail(s"no book of $outcome"))
    val r    = (book.buys ++ book.sells).find(_.order == OrderId(id))
    assertEquals(r.map(_.price), Some(n(price)), s"$id in the book of $outcome")
    assertEquals(order(m, id).remaining, n(shares))
  }

  Then("nothing rests in the book of {string} in {string}") { (outcome: String, m: String) =>
    assertEquals(world.value(m).books.get(OutcomeId(outcome)), Some(Book()))
  }

  Then("the cost of the order {string} is {string}") { (id: String, cost: String) =>
    assertEquals(order("m-1", id).cost, n(cost))
  }

  Then("the order {string} is {word}") { (id: String, state: String) =>
    assertEquals(OrderState.word(order("m-1", id).state), state)
  }

  Then("the order {string} is cancelled for {string} shares") { (id: String, shares: String) =>
    val o = order("m-1", id)
    assertEquals((o.state, o.remaining), (OrderState.Cancelled, n(shares)))
  }

  Then("the order is refused naming the {word}") { (reason: String) =>
    assertEquals(refusal.reason, reason)
  }
  Then("the order is refused naming the state {string}") { (state: String) =>
    assertEquals(refusal, Refusal("state", Map("state" -> state)))
  }
  Then("the order is refused naming the maximum order cost") { () =>
    assertEquals(refusal, Refusals.limit(Refusals.maximumOrderCost))
  }
  Then("the order is refused naming the maximum position") { () =>
    assertEquals(refusal, Refusals.limit(Refusals.maximumPosition))
  }
  Then("the order is refused naming the quantity held")(() =>
    assertEquals(refusal.reason, "quantity held")
  )
  Then("the order is refused naming the id {string}") { (id: String) =>
    assertEquals(refusal, Refusals.id(OrderId(id)))
  }
  Then("the order is refused as not fillable whole")(() =>
    assertEquals(refusal, Refusals.notFillableWhole)
  )
  Then("the cancel is refused naming the owner")(() => assertEquals(refusal.reason, "owner"))
  Then("the cancel is refused naming the state {string}") { (state: String) =>
    assertEquals(refusal, Refusal("state", Map("state" -> state)))
  }
  Then("the split is refused naming the maximum position") { () =>
    assertEquals(refusal, Refusals.limit(Refusals.maximumPosition))
  }
  Then("the merge is refused naming the quantity held")(() =>
    assertEquals(refusal.reason, "quantity held")
  )
  Then("the command is refused naming the {word}") { (reason: String) =>
    assertEquals(refusal.reason, reason)
  }
  Then("the command is refused naming the state {string}") { (state: String) =>
    assertEquals(refusal, Refusal("state", Map("state" -> state)))
  }

  Then("the answer is the one the first order {string} received") { (id: String) =>
    assert(world.firstOf(id).isRight, s"the first order $id was refused: ${world.firstOf(id)}")
    assertEquals(world.last.map(_.map(_.events)), Some(Right(Vector.empty)))
  }
  Then("the answer is the one the first split {string} received") { (key: String) =>
    assert(world.firstOf(key).isRight, s"the first split $key was refused")
    assertEquals(world.last.map(_.map(_.events)), Some(Right(Vector.empty)))
  }

  Then("no event is produced")(() => assertEquals(world.lastEvents, Vector.empty))

  Then("{string} is released {string}") { (o: String, amount: String) =>
    assert(world.releases(o).contains(n(amount)), s"$o's releases: ${world.releases(o)}")
  }
  Then("{string} is charged {string}")((o: String, amount: String) =>
    assertEquals(world.charged(o), n(amount))
  )
  Then("{string} is paid {string}")((o: String, amount: String) =>
    assertEquals(world.paid(o), n(amount))
  )
  Then("{string} is paid nothing")((o: String) => assertEquals(world.paid(o), 0L))

  Then("{string} is paid {string} for the position in {string} of {string}") {
    (owner: String, amount: String, outcome: String, _: String) =>
      val paid = world.lastEvents.collect {
        case e: ExchangeEvent.PositionPaid
            if e.owner == OwnerId(owner) && e.outcome == OutcomeId(outcome) =>
          e.paid
      }
      assertEquals(paid, Vector(n(amount)))
  }

  private def between(a: String, b: String): Vector[ExchangeEvent.TradeMade] =
    world.trades.filter(t => t.legs.map(_.order.value).toSet == Set(a, b))

  Then(
    "one trade is made for {string} shares at price {string} between the orders {string} and {string}"
  ) { (shares: String, price: String, a: String, b: String) =>
    val made = between(a, b)
    assertEquals(
      made.map(t => (t.through, t.quantity, t.prices.values.toVector)),
      Vector((Through.Direct, n(shares), Vector(n(price))))
    )
  }

  Then(
    "one trade is made for {string} shares between the orders {string} and {string} by splitting a set"
  ) { (shares: String, a: String, b: String) =>
    assertEquals(
      between(a, b).map(t => (t.through, t.quantity)),
      Vector((Through.Split, n(shares)))
    )
  }

  Then(
    "one trade is made for {string} shares between the orders {string} and {string} by merging a set"
  ) { (shares: String, a: String, b: String) =>
    assertEquals(
      between(a, b).map(t => (t.through, t.quantity)),
      Vector((Through.Merge, n(shares)))
    )
  }

  Then("no trade is made")(() => assertEquals(world.trades, Vector.empty))

  private def legOf(owner: String): Leg =
    world.trades
      .flatMap(_.legs)
      .find(_.owner == OwnerId(owner))
      .getOrElse(fail(s"no leg of $owner"))

  Then("the trade charges {string} {string} and pays {string} {string}") {
    (a: String, charge: String, b: String, paid: String) =>
      assertEquals((legOf(a).charge, legOf(b).paid), (n(charge), n(paid)))
  }
  Then("the trade charges {string} {string} and charges {string} {string}") {
    (a: String, x: String, b: String, y: String) =>
      assertEquals((legOf(a).charge, legOf(b).charge), (n(x), n(y)))
  }
  Then("the trade pays {string} {string} and pays {string} {string}") {
    (a: String, x: String, b: String, y: String) =>
      assertEquals((legOf(a).paid, legOf(b).paid), (n(x), n(y)))
  }

  Then("the trade between {string} and {string} carries the sequence number {string}") {
    (a: String, b: String, sequence: String) =>
      assertEquals(between(a, b).map(_.sequence), Vector(n(sequence)))
  }

  Then("the pool of {string} holds {string}")((m: String, amount: String) =>
    assertEquals(world.value(m).pool, n(amount))
  )
  Then("the pool of {string} holds {string} as its remainder") { (m: String, amount: String) =>
    assertEquals(world.value(m).remainder, n(amount))
  }
  Then("{string} sets are outstanding in {string}") { (sets: String, m: String) =>
    assertEquals(world.value(m).setsOutstanding, n(sets))
  }
  Then("{string} shares of {string} are held in {string} in total") {
    (shares: String, outcome: String, m: String) =>
      val held =
        world.value(m).positions.values.flatMap(_.get(OutcomeId(outcome))).map(_.quantity).sum
      assertEquals(held, n(shares))
  }
  Then(
    "the position of {string} in {string} of {string} shows {string} shares at a cost of {string}"
  ) { (owner: String, outcome: String, m: String, shares: String, cost: String) =>
    val p = position(m, owner, outcome)
    assertEquals((p.quantity, p.cost), (n(shares), n(cost)))
  }

  Then("the last price of {string} in {string} is {string}") {
    (outcome: String, m: String, price: String) =>
      assertEquals(world.value(m).lastPrice(OutcomeId(outcome)), Some(n(price)))
  }
  Then("{string} in {string} has no last price") { (outcome: String, m: String) =>
    assertEquals(world.value(m).lastPrice(OutcomeId(outcome)), None)
  }
  Then("the best bid of {string} in {string} is {string} and the best ask is {string}") {
    (outcome: String, m: String, bid: String, ask: String) =>
      val x = world.value(m)
      assertEquals(
        (x.bestBid(OutcomeId(outcome)), x.bestAsk(OutcomeId(outcome))),
        (Some(n(bid)), Some(n(ask)))
      )
  }
  Then(
    "the depth shows {string} shares to buy at {string} and {string} shares to sell at {string}"
  ) { (buys: String, bid: String, sells: String, ask: String) =>
    assertEquals(
      world.value("m-1").depth(OutcomeId("yes")),
      (Vector(Level(n(bid), n(buys))), Vector(Level(n(ask), n(sells))))
    )
  }

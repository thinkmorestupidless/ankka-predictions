package com.thinkmorestupidless.predictions.exchange

import java.time.Instant
import scala.collection.mutable

/**
 * The step definitions' world (research R10): each exchange by its market, the commands applied to
 * it in order, the answer to the last command, and the first answer each order id and set key
 * received. Holdings a scenario gives are splits applied with the maximum position lifted for that
 * one command, so the pool stays exact; a limit a scenario gives reopens the exchange with it and
 * applies the same commands again, since limits are fixed when an exchange opens.
 */
final class ExchangeWorld:

  /** The answer to a command: a refusal, or the decision. */
  type Answer = Either[Refusal, Decision]

  private final case class Applied(command: ExchangeCommand, lifted: Boolean)

  private final case class Market(
      open: ExchangeCommand.Open,
      applied: Vector[Applied],
      value: Exchange,
      events: Vector[ExchangeEvent]
  )

  private val markets = mutable.Map.empty[MarketId, Market]
  private val firsts  = mutable.Map.empty[String, Answer]
  private var clock   = Instant.parse("2026-10-31T10:00:00Z")
  private var seeds   = 0

  /** The answer to the last command applied, and the value it was applied to. */
  var last: Option[Answer]     = None
  var before: Option[Exchange] = None
  var acted: Boolean           = false

  /** The default limits: spec 003's 1,000 units a buy and 10,000 shares an outcome. */
  def open(
      market: String,
      outcomes: Vector[String],
      unit: Long,
      at: Option[Instant] = None
  ): Unit =
    val m = MarketId(market)
    val c: ExchangeCommand.Open = ExchangeCommand.Open(
      m,
      outcomes.map(OutcomeId(_)),
      unit,
      maxOrderCost = 1_000 * unit,
      maxPosition = 10_000,
      at = at.getOrElse(tick())
    )
    val decided =
      Exchange(Exchange.empty, c).fold(r => sys.error(s"$market did not open: $r"), identity)
    markets(m) = Market(c, Vector.empty, decided.next, decided.events)

  def value(market: String): Exchange = state(MarketId(market)).value

  def events(market: String): Vector[ExchangeEvent] = state(MarketId(market)).events

  def opened(market: String): ExchangeCommand.Open = state(MarketId(market)).open

  /** Every command applied to the exchange, its open first, as a fresh exchange would take them. */
  def commands(market: String): Vector[ExchangeCommand] =
    val m = state(MarketId(market))
    m.open +: m.applied.map(_.command)

  private def state(m: MarketId): Market =
    markets.getOrElse(m, sys.error(s"no exchange ${m.value} in this scenario"))

  def tick(): Instant =
    clock = clock.plusSeconds(1)
    clock

  /** Apply a command, recording its answer; `lifted` applies it with no maximum position. */
  def apply(market: String, command: ExchangeCommand, lifted: Boolean = false): Answer =
    val m      = state(MarketId(market))
    val answer = decide(m.value, command, lifted)
    before = Some(m.value)
    last = Some(answer)
    answer.foreach { d =>
      markets(MarketId(market)) = m.copy(
        applied = m.applied :+ Applied(command, lifted),
        value = d.next,
        events = m.events ++ d.events
      )
    }
    answer

  private def decide(x: Exchange, command: ExchangeCommand, lifted: Boolean): Answer =
    if !lifted then Exchange(x, command)
    else
      Exchange(x.copy(maxPosition = Long.MaxValue), command).map(d =>
        d.copy(next = d.next.copy(maxPosition = x.maxPosition))
      )

  /** Record the answer an id or key received the first time. */
  def first(key: String, answer: Answer): Unit = if !firsts.contains(key) then firsts(key) = answer

  def firstOf(key: String): Answer = firsts.getOrElse(key, sys.error(s"$key was never applied"))

  /** Open the exchange again with other limits and apply the same commands, in order. */
  def reopen(
      market: String,
      maxOrderCost: Option[Long] = None,
      maxPosition: Option[Long] = None
  ): Unit =
    val m = state(MarketId(market))
    val c: ExchangeCommand.Open = m.open.copy(
      maxOrderCost = maxOrderCost.getOrElse(m.open.maxOrderCost),
      maxPosition = maxPosition.getOrElse(m.open.maxPosition)
    )
    val opened = Exchange(Exchange.empty, c).fold(r => sys.error(r.toString), identity)
    markets(m.open.market) = Market(c, Vector.empty, opened.next, opened.events)
    m.applied.foreach(a =>
      apply(market, a.command, a.lifted).left.foreach(r =>
        sys.error(s"${a.command} on reopening: $r")
      )
    )
    last = None
    before = None

  /** Give an owner shares of every outcome: a split the scenario's limits do not refuse. */
  def seedHolding(market: String, owner: String, shares: Long): Unit =
    seeds += 1
    apply(
      market,
      ExchangeCommand.Split(SetKey(s"seed-$owner-$seeds"), OwnerId(owner), shares, tick()),
      lifted = true
    ).left
      .foreach(r => sys.error(s"$owner could not be given $shares shares: $r"))

  def place(
      market: String,
      owner: String,
      side: Side,
      order: String,
      quantity: Long,
      outcome: String,
      price: Long,
      duration: OrderDuration = OrderDuration.RestUntilClose
  ): Answer =
    val answer = apply(
      market,
      ExchangeCommand.Place(
        OrderId(order),
        OwnerId(owner),
        OutcomeId(outcome),
        side,
        price,
        quantity,
        duration,
        tick()
      )
    )
    first(order, answer)
    answer

  /** The seller sells to the buyer: the buyer's order rests, the seller's meets it. */
  def sold(
      market: String,
      seller: String,
      shares: Long,
      outcome: String,
      buyer: String,
      price: Long
  ): Unit =
    seeds += 1
    val n = seeds
    place(market, buyer, Side.Buy, s"seed-buy-$n", shares, outcome, price).left.foreach(r =>
      sys.error(r.toString)
    )
    place(market, seller, Side.Sell, s"seed-sell-$n", shares, outcome, price).left.foreach(r =>
      sys.error(r.toString)
    )

  def nextSeedKey(prefix: String): String =
    seeds += 1
    s"$prefix-$seeds"

  /** The events of the last answer: none when it was refused. */
  def lastEvents: Vector[ExchangeEvent] = last.flatMap(_.toOption).fold(Vector.empty)(_.events)

  def trades: Vector[ExchangeEvent.TradeMade] = lastEvents.collect {
    case t: ExchangeEvent.TradeMade => t
  }

  /**
   * Every release to the owner in the last answer, one per key: a trade at a better price, a
   * cancel, an expiry. "X is released N" holds when one of them is N, since a fill-at-once order
   * releases its price improvement and then its remainder (`matching.feature`, research).
   */
  def releases(owner: String): Vector[Long] =
    val o = OwnerId(owner)
    lastEvents.flatMap {
      case e: ExchangeEvent.TradeMade =>
        e.legs.filter(l => l.owner == o && l.released > 0).map(_.released)
      case e: ExchangeEvent.OrderCancelled =>
        Option.when(e.owner == o && e.released > 0)(e.released)
      case e: ExchangeEvent.OrderExpired => Option.when(e.owner == o && e.released > 0)(e.released)
      case _                             => None
    }

  def charged(owner: String): Long =
    val o = OwnerId(owner)
    lastEvents.map {
      case e: ExchangeEvent.TradeMade => e.legs.filter(_.owner == o).map(_.charge).sum
      case e: ExchangeEvent.SetsSplit => if e.owner == o then e.charge else 0L
      case _                          => 0L
    }.sum

  def paid(owner: String): Long =
    val o = OwnerId(owner)
    lastEvents.map {
      case e: ExchangeEvent.TradeMade    => e.legs.filter(_.owner == o).map(_.paid).sum
      case e: ExchangeEvent.SetsMerged   => if e.owner == o then e.paid else 0L
      case e: ExchangeEvent.PositionPaid => if e.owner == o then e.paid else 0L
      case _                             => 0L
    }.sum

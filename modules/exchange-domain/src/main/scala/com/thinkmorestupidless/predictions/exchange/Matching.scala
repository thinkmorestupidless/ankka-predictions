package com.thinkmorestupidless.predictions.exchange

import java.time.Instant
import scala.collection.immutable.SortedMap

/**
 * The matcher (spec 005 FR-008 to FR-012, research R3): an order just placed, already resting in
 * its book, is matched against the best candidate until it is filled or none is within its price. A
 * candidate is the best order of the other side of its own book, or, in a two-outcome market, the
 * best order of the same side of the other outcome's book, met through the pool. The better price
 * for the taker wins, its own book when equal; an order of the taker's own owner is passed over.
 */
object Matching:

  /** A resting order the taker could trade with, at the price the taker would pay or be paid. */
  private final case class Candidate(resting: Order, takerPrice: Long, through: Through)

  /** The events after the taker's `OrderPlaced`, or the refusal of a fill-whole order. */
  def place(
      exchange: Exchange,
      taker: OrderId,
      at: Instant
  ): Either[Refusal, Vector[ExchangeEvent]] =
    val (after, events) = run(exchange, taker, at, Vector.empty)
    val order           = after.orders(taker)
    order.duration match
      case OrderDuration.FillWhole if order.remaining > 0 => Left(Refusals.notFillableWhole)
      case OrderDuration.FillAtOnce if order.remaining > 0 =>
        val (released, ordinal) = Exchange.release(order)
        Right(events :+ ExchangeEvent.OrderCancelled(taker, order.owner, released, ordinal, at))
      case _ => Right(events)

  @scala.annotation.tailrec
  private def run(
      x: Exchange,
      taker: OrderId,
      at: Instant,
      events: Vector[ExchangeEvent]
  ): (Exchange, Vector[ExchangeEvent]) =
    val order = x.orders(taker)
    if order.remaining == 0 then (x, events)
    else
      best(x, order) match
        case None => (x, events)
        case Some(c) =>
          val made = trade(x, order, c, at)
          run(made.foldLeft(x)(Exchange.replay), taker, at, events ++ made)

  private def best(x: Exchange, taker: Order): Option[Candidate] =
    def first(resting: Vector[Resting]): Option[Order] =
      resting.iterator
        .flatMap(r => x.orders.get(r.order))
        .find(_.owner != taker.owner)
    val book = x.books.getOrElse(taker.outcome, Book())
    val own = taker.side match
      case Side.Buy =>
        first(book.sells)
          .filter(_.price <= taker.price)
          .map(s => Candidate(s, s.price, Through.Direct))
      case Side.Sell =>
        first(book.buys)
          .filter(_.price >= taker.price)
          .map(b => Candidate(b, b.price, Through.Direct))
    val other = x.outcomes match
      case Vector(a, b) =>
        val theirs = x.books.getOrElse(if a == taker.outcome then b else a, Book())
        taker.side match
          case Side.Buy =>
            first(theirs.buys)
              .filter(q => taker.price + q.price >= x.unit)
              .map(q => Candidate(q, x.unit - q.price, Through.Split))
          case Side.Sell =>
            first(theirs.sells)
              .filter(q => taker.price + q.price <= x.unit)
              .map(q => Candidate(q, x.unit - q.price, Through.Merge))
      case _ => None
    (own, other) match
      case (Some(o), Some(p)) =>
        val poolBetter = taker.side match
          case Side.Buy  => p.takerPrice < o.takerPrice
          case Side.Sell => p.takerPrice > o.takerPrice
        Some(if poolBetter then p else o)
      case (o, p) => o.orElse(p)

  /**
   * One trade with a candidate, and the fills it completes: resting order first, then the taker.
   */
  private def trade(x: Exchange, taker: Order, c: Candidate, at: Instant): Vector[ExchangeEvent] =
    val quantity = math.min(taker.remaining, c.resting.remaining)
    val sequence = x.nextTrade
    // The resting order trades at its own price; the taker at the candidate's price for it.
    def leg(o: Order, price: Long): Leg = o.side match
      case Side.Buy =>
        val released = quantity * (o.price - price)
        Leg(
          o.owner,
          o.id,
          o.outcome,
          o.side,
          charge = quantity * price,
          paid = 0,
          released = released,
          releaseOrdinal = if released > 0 then o.releases + 1 else 0
        )
      case Side.Sell =>
        Leg(o.owner, o.id, o.outcome, o.side, 0, quantity * price, 0, 0)
    val takerLeg   = leg(taker, c.takerPrice)
    val restingLeg = leg(c.resting, c.resting.price)
    val legs = (c.through, taker.side) match
      case (Through.Direct, Side.Sell) => Vector(restingLeg, takerLeg)
      case _                           => Vector(takerLeg, restingLeg)
    val prices = SortedMap(taker.outcome -> c.takerPrice) ++
      Option.when(c.through != Through.Direct)(c.resting.outcome -> c.resting.price)
    val made = ExchangeEvent.TradeMade(
      TradeId.of(sequence),
      sequence,
      c.through,
      quantity,
      prices,
      legs,
      at
    )
    val filled = Vector(c.resting, taker).collect {
      case o if o.remaining == quantity => ExchangeEvent.OrderFilled(o.id, at)
    }
    made +: filled

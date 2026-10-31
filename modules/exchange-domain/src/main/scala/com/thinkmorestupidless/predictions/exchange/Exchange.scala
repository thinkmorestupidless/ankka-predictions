package com.thinkmorestupidless.predictions.exchange

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}

import java.time.Instant
import scala.collection.immutable.SortedMap

/**
 * One market's exchange, as a value: every outcome's book, the orders it accepted, the positions,
 * the pool and the time of its last event (spec 005 FR-001). Ordered collections throughout, so
 * `==` is replay equality.
 */
final case class Exchange(
    market: MarketId = MarketId(""),
    outcomes: Vector[OutcomeId] = Vector.empty,
    unit: Long = 0,
    maxOrderCost: Long = 0,
    maxPosition: Long = 0,
    state: ExchangeState = ExchangeState.Empty,
    books: SortedMap[OutcomeId, Book] = SortedMap.empty,
    orders: SortedMap[OrderId, Order] = SortedMap.empty,
    positions: SortedMap[OwnerId, SortedMap[OutcomeId, Position]] = SortedMap.empty,
    setMoves: SortedMap[SetKey, SetMove] = SortedMap.empty,
    setsOutstanding: Long = 0,
    remainder: Long = 0,
    lastPrices: SortedMap[OutcomeId, Long] = SortedMap.empty,
    nextOrder: Long = 1,
    nextTrade: Long = 1,
    lastTime: Option[Instant] = None,
    finality: Option[Finality] = None
):

  def position(owner: OwnerId, outcome: OutcomeId): Position =
    positions.get(owner).flatMap(_.get(outcome)).getOrElse(Position())

  /** The pool: one unit per set outstanding, and the remainder a void kept (spec 005 FR-015). */
  def pool: Long = setsOutstanding * unit + remainder

  private[exchange] def withPosition(owner: OwnerId, outcome: OutcomeId)(
      f: Position => Position
  ): Exchange =
    val byOutcome = positions.getOrElse(owner, SortedMap.empty[OutcomeId, Position])
    copy(positions =
      positions.updated(owner, byOutcome.updated(outcome, f(position(owner, outcome))))
    )

  private[exchange] def withOrder(id: OrderId)(f: Order => Order): Exchange =
    orders.get(id).fold(this)(o => copy(orders = orders.updated(id, f(o))))

  private[exchange] def withoutResting(id: OrderId): Exchange =
    orders.get(id).fold(this) { o =>
      copy(books = books.updatedWith(o.outcome)(_.map(_.remove(id))))
    }

object Exchange:

  val empty: Exchange = Exchange()

  /** What a buy of `quantity` at `price` costs, and what the consumer reserves for it. */
  def cost(quantity: Long, price: Long): Long = quantity * price

  /** Snapshots, under the manifest `exchange`; the events' serializer is in their companion. */
  given Serializer[Exchange] = Codecs.serializer[Exchange]("exchange")

  /**
   * A command applied to an exchange: a refusal with no change, or the next value and its events,
   * where the next value is the events replayed onto this one (spec 005 FR-002).
   *
   * Spec 004 builds the out-of-order check and `Open`, which the components start from; every other
   * rule is spec 005's, and until it lands each other command is refused as not built.
   */
  def apply(exchange: Exchange, command: ExchangeCommand): Either[Refusal, Decision] =
    exchange.lastTime.filter(command.at.isBefore) match
      case Some(last) => Left(Refusals.outOfOrder(last, command.at))
      case None =>
        command match
          case c: ExchangeCommand.Open => open(exchange, c)
          case other                   => Left(Refusals.notBuilt(other.name))

  private def open(exchange: Exchange, c: ExchangeCommand.Open): Either[Refusal, Decision] =
    if exchange.state != ExchangeState.Empty then Left(Refusals.state(exchange.state))
    else
      decide(
        exchange,
        Vector(
          ExchangeEvent.Opened(c.market, c.outcomes, c.unit, c.maxOrderCost, c.maxPosition, c.at)
        )
      )

  private def decide(exchange: Exchange, events: Vector[ExchangeEvent]): Either[Refusal, Decision] =
    Right(Decision(events.foldLeft(exchange)(replay), events))

  /** An event applied to an exchange: the next value. Total over every case. */
  def replay(exchange: Exchange, event: ExchangeEvent): Exchange =
    import ExchangeEvent.*
    val next = event match
      case e: Opened =>
        exchange.copy(
          market = e.market,
          outcomes = e.outcomes,
          unit = e.unit,
          maxOrderCost = e.maxOrderCost,
          maxPosition = e.maxPosition,
          state = ExchangeState.Open,
          books = SortedMap.from(e.outcomes.map(_ -> Book()))
        )

      case e: OrderPlaced =>
        val order = Order(
          e.order,
          e.owner,
          e.outcome,
          e.side,
          e.price,
          e.quantity,
          e.quantity,
          e.duration,
          OrderState.Resting,
          e.cost,
          e.sequence,
          releases = 0
        )
        val placed = exchange.copy(
          orders = exchange.orders.updated(e.order, order),
          books = exchange.books.updatedWith(e.outcome)(
            _.map(_.rest(e.side, Resting(e.order, e.price, e.sequence)))
          ),
          nextOrder = e.sequence + 1
        )
        placed.withPosition(e.owner, e.outcome)(p =>
          if e.side == Side.Sell then p.copy(restingUnderSells = p.restingUnderSells + e.quantity)
          else p.copy(restingToBuy = p.restingToBuy + e.quantity)
        )

      case e: TradeMade =>
        val filled = e.legs.foldLeft(exchange) { (x, leg) =>
          x.withOrder(leg.order)(o =>
            o.copy(
              remaining = o.remaining - e.quantity,
              state = OrderState.FilledInPart,
              releases = if leg.released > 0 then leg.releaseOrdinal else o.releases
            )
          )
        }
        val traded = moved(filled, e)
        val sets = e.through match
          case Through.Direct => traded.setsOutstanding
          case Through.Split  => traded.setsOutstanding + e.quantity
          case Through.Merge  => traded.setsOutstanding - e.quantity
        traded.copy(
          setsOutstanding = sets,
          lastPrices = traded.lastPrices ++ e.prices,
          nextTrade = e.sequence + 1
        )

      case e: OrderFilled =>
        exchange.withoutResting(e.order).withOrder(e.order)(_.copy(state = OrderState.Filled))

      case e: OrderCancelled => ended(exchange, e.order, OrderState.Cancelled, e.releaseOrdinal)
      case e: OrderExpired   => ended(exchange, e.order, OrderState.Expired, e.releaseOrdinal)

      case e: SetsSplit =>
        moved(exchange, e).copy(
          setMoves =
            exchange.setMoves.updated(e.key, SetMove(SetKind.Split, e.owner, e.sets, e.at)),
          setsOutstanding = exchange.setsOutstanding + e.sets
        )

      case e: SetsMerged =>
        moved(exchange, e).copy(
          setMoves =
            exchange.setMoves.updated(e.key, SetMove(SetKind.Merge, e.owner, e.sets, e.at)),
          setsOutstanding = exchange.setsOutstanding - e.sets
        )

      case _: Suspended => exchange.copy(state = ExchangeState.Suspended)
      case _: Resumed   => exchange.copy(state = ExchangeState.Open)
      case _: Closed    => exchange.copy(state = ExchangeState.Closed)

      case e: PositionPaid => moved(exchange, e)

      case e: RemainderKept => exchange.copy(remainder = exchange.remainder + e.amount)

      case e: Final =>
        exchange.copy(state = ExchangeState.Final, setsOutstanding = 0, finality = Some(e.finality))
    next.copy(lastTime = Some(event.at))

  private def moved(exchange: Exchange, event: ExchangeEvent): Exchange =
    PositionMoves
      .of(event, exchange.outcomes, exchange.unit)
      .foldLeft(exchange)((x, m) => x.withPosition(m.owner, m.outcome)(m.move))

  private def ended(exchange: Exchange, id: OrderId, state: OrderState, ordinal: Int): Exchange =
    exchange.orders.get(id).fold(exchange) { order =>
      val freed = exchange.withPosition(order.owner, order.outcome)(p =>
        if order.side == Side.Sell then
          p.copy(restingUnderSells = p.restingUnderSells - order.remaining)
        else p.copy(restingToBuy = p.restingToBuy - order.remaining)
      )
      freed
        .withoutResting(id)
        .withOrder(id)(o =>
          o.copy(state = state, releases = if ordinal > 0 then ordinal else o.releases)
        )
    }

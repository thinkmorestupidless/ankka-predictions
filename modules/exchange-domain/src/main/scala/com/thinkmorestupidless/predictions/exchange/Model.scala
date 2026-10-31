package com.thinkmorestupidless.predictions.exchange

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec

import java.time.Instant

/** Which side of a book an order is on. */
enum Side:
  case Buy, Sell

object Side:
  def word(s: Side): String  = s.toString.toLowerCase
  given JsonValueCodec[Side] = wordCodec(values, word)

/** How long an order stands (spec 005 FR-008). */
enum OrderDuration:
  /** Its remainder rests until it fills, is cancelled or the exchange closes. */
  case RestUntilClose

  /** It fills what it can at once; the remainder is cancelled. */
  case FillAtOnce

  /** It fills whole at once or is refused. */
  case FillWhole

object OrderDuration:
  def word(d: OrderDuration): String = d match
    case RestUntilClose => "rest-until-close"
    case FillAtOnce     => "fill-at-once"
    case FillWhole      => "fill-whole"
  given JsonValueCodec[OrderDuration] = wordCodec(values, word)

enum OrderState:
  case Resting, FilledInPart, Filled, Cancelled, Expired

object OrderState:
  def word(s: OrderState): String = s match
    case Resting      => "resting"
    case FilledInPart => "filled-in-part"
    case Filled       => "filled"
    case Cancelled    => "cancelled"
    case Expired      => "expired"
  given JsonValueCodec[OrderState] = wordCodec(values, word)

/** The exchange's state: `empty → open ↔ suspended → closed → final`. */
enum ExchangeState:
  case Empty, Open, Suspended, Closed, Final

object ExchangeState:
  def word(s: ExchangeState): String  = s.toString.toLowerCase
  given JsonValueCodec[ExchangeState] = wordCodec(values, word)

/**
 * How a trade was made (spec 005 FR-011): between a buy and a sell, by splitting a set between two
 * buys, or by merging a set from two sells.
 */
enum Through:
  case Direct, Split, Merge

object Through:
  def word(t: Through): String  = t.toString.toLowerCase
  given JsonValueCodec[Through] = wordCodec(values, word)

/**
 * One side of a trade: its order's outcome and side, what it is charged from its reservation, what
 * it is paid, and what of its reservation the trade frees under the order's next release ordinal (0
 * when it frees nothing). The outcome and side make a trade readable without its orders: a split's
 * legs are two buys of different outcomes, a merge's two sells. Spec 005 FR-018; research R14.
 */
final case class Leg(
    owner: OwnerId,
    order: OrderId,
    outcome: OutcomeId,
    side: Side,
    charge: Long,
    paid: Long,
    released: Long,
    releaseOrdinal: Int
)

/** How a final exchange paid: one outcome resolved, or the market void. */
enum Finality:
  case Resolved(outcome: OutcomeId)
  case Void

/** An order the exchange accepted (spec 005, key entities). */
final case class Order(
    id: OrderId,
    owner: OwnerId,
    outcome: OutcomeId,
    side: Side,
    price: Long,
    quantity: Long,
    remaining: Long,
    duration: OrderDuration,
    state: OrderState,
    /** A buy's cost, quantity times price; zero for a sell. */
    cost: Long,
    /** The order of acceptance, which breaks a tie in price. */
    sequence: Long,
    /** The last release ordinal this order used; its next release is one more. */
    releases: Int
)

/** An order resting in a book: enough to keep the book in price and then acceptance order. */
final case class Resting(order: OrderId, price: Long, sequence: Long)

/**
 * One outcome's resting orders: buys highest price first, sells lowest; oldest first within a
 * price.
 */
final case class Book(buys: Vector[Resting] = Vector.empty, sells: Vector[Resting] = Vector.empty):

  def rest(side: Side, r: Resting): Book = side match
    case Side.Buy  => copy(buys = (buys :+ r).sortBy(x => (-x.price, x.sequence)))
    case Side.Sell => copy(sells = (sells :+ r).sortBy(x => (x.price, x.sequence)))

  def remove(order: OrderId): Book =
    copy(buys = buys.filterNot(_.order == order), sells = sells.filterNot(_.order == order))

/** An owner's holding in one outcome. */
final case class Position(quantity: Long = 0, restingUnderSells: Long = 0, cost: Long = 0)

enum SetKind:
  case Split, Merge

object SetKind:
  def word(k: SetKind): String  = k.toString.toLowerCase
  given JsonValueCodec[SetKind] = wordCodec(values, word)

/** An explicit split or merge, kept by its key so that a repeat is answered as the first. */
final case class SetMove(kind: SetKind, owner: OwnerId, sets: Long, at: Instant)

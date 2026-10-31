package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.{Codecs, Serializer}
import com.thinkmorestupidless.predictions.exchange.*

import scala.collection.immutable.SortedMap

/**
 * The last price of every outcome that has traded, and the sequence of the exchange's journal it is
 * current to (ankka-modules spec 023 FR-005: a client that misses a message sees the gap).
 */
final case class Prices(prices: SortedMap[OutcomeId, Long], sequence: Long)

/** One outcome's book by price, best first on each side, current to `sequence`. */
final case class Depth(
    outcome: OutcomeId,
    buys: Vector[Level],
    sells: Vector[Level],
    sequence: Long
)

/** An owner's positions in one market, by outcome. */
final case class Holdings(owner: OwnerId, positions: SortedMap[OutcomeId, Position])

object Prices:
  given Serializer[Prices] = Codecs.serializer[Prices]("exchange-prices")

object Depth:
  given Serializer[Depth] = Codecs.serializer[Depth]("exchange-depth")

  /** The levels of `exchange`'s book of `outcome` (the domain's `depth`), current to `sequence`. */
  def of(exchange: Exchange, outcome: OutcomeId, sequence: Long): Depth =
    val (buys, sells) = exchange.depth(outcome)
    Depth(outcome, buys, sells, sequence)

object Holdings:
  given Serializer[Holdings] = Codecs.serializer[Holdings]("exchange-holdings")

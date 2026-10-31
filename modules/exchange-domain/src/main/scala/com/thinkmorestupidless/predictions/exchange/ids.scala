package com.thinkmorestupidless.predictions.exchange

import com.github.plokhotnyuk.jsoniter_scala.core.*

// The ids the exchange compares and nothing more. Each is a value class over a string, written in
// JSON as the string (`contracts/ports.md`, "Ids"); the consumer decides what each contains.

/** The market this exchange trades: the exchange entity's id. */
final case class MarketId(value: String) extends AnyVal

/** One of a market's outcomes, by its name. */
final case class OutcomeId(value: String) extends AnyVal

/** Who holds a position or places an order: a player id, to the consumer. */
final case class OwnerId(value: String) extends AnyVal

/** An order, by the id the consumer gave it; also the key of its reservation and its releases. */
final case class OrderId(value: String) extends AnyVal

/** A trade, by the id the exchange gave it from its sequence. */
final case class TradeId(value: String) extends AnyVal

/** The key of an explicit split or merge, given by the consumer. */
final case class SetKey(value: String) extends AnyVal

/** A position at finality, the key of its payout: its owner and outcome within the market. */
final case class PositionKey(value: String) extends AnyVal

object MarketId:
  given Ordering[MarketId] = Ordering.by(_.value)
object OutcomeId:
  given Ordering[OutcomeId] = Ordering.by(_.value)
object OwnerId:
  given Ordering[OwnerId] = Ordering.by(_.value)
object OrderId:
  given Ordering[OrderId] = Ordering.by(_.value)
object TradeId:
  given Ordering[TradeId] = Ordering.by(_.value)

  /** The exchange's trade id for its sequence number: `t-1` for the first trade. */
  def of(sequence: Long): TradeId = TradeId(s"t-$sequence")
object SetKey:
  given Ordering[SetKey] = Ordering.by(_.value)
object PositionKey:
  given Ordering[PositionKey] = Ordering.by(_.value)

  /** The key of the payout of `owner`'s position in `outcome`. */
  def of(owner: OwnerId, outcome: OutcomeId): PositionKey =
    PositionKey(s"${owner.value}/${outcome.value}")

/**
 * A string codec for a status-word enum, so a fieldless case is written `"buy"`, not
 * `{"type":"Buy"}` (ankka's shared codec config). Given in each enum's companion, so every
 * derivation site sees the same one.
 */
private[predictions] def wordCodec[E](values: Array[E], word: E => String): JsonValueCodec[E] =
  val byWord = values.map(v => word(v) -> v).toMap
  new JsonValueCodec[E]:
    def nullValue: E                             = null.asInstanceOf[E]
    def encodeValue(x: E, out: JsonWriter): Unit = out.writeVal(word(x))
    def decodeValue(in: JsonReader, default: E): E =
      val w = in.readString(null)
      byWord.getOrElse(w, in.decodeError(s"unknown value '$w'"))

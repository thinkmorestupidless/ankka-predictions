package com.thinkmorestupidless.predictions.markets

import com.github.plokhotnyuk.jsoniter_scala.core.*

// The markets domain's own ids; it shares no module with the exchange (research R1). Each is a
// value class over a string, written in JSON as the string.

/** A market: the market entity's id, and the exchange's that trades it. */
final case class MarketId(value: String) extends AnyVal

/** One of a market's outcomes, by its name. */
final case class OutcomeId(value: String) extends AnyVal

/** The operator the consumer says acted. */
final case class OperatorId(value: String) extends AnyVal

object MarketId:
  given Ordering[MarketId] = Ordering.by(_.value)
object OutcomeId:
  given Ordering[OutcomeId] = Ordering.by(_.value)
object OperatorId:
  given Ordering[OperatorId] = Ordering.by(_.value)

/** A string codec for a status-word enum, as the exchange domain's. */
private[predictions] def wordCodec[E](values: Array[E], word: E => String): JsonValueCodec[E] =
  val byWord = values.map(v => word(v) -> v).toMap
  new JsonValueCodec[E]:
    def nullValue: E                             = null.asInstanceOf[E]
    def encodeValue(x: E, out: JsonWriter): Unit = out.writeVal(word(x))
    def decodeValue(in: JsonReader, default: E): E =
      val w = in.readString(null)
      byWord.getOrElse(w, in.decodeError(s"unknown value '$w'"))

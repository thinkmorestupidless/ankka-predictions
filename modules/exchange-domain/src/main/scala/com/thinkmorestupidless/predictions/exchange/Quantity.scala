package com.thinkmorestupidless.predictions.exchange

import scala.util.Try

/**
 * A quantity as the exchange takes it: a positive whole number. For the edge that receives one as
 * text or as a decimal (the consumer's endpoint, the step definitions), so that "not a positive
 * whole number" is one rule, refused naming the quantity as it was given.
 */
object Quantity:

  def of(text: String): Either[Refusal, Long] =
    Try(BigDecimal(text.trim)).toOption.fold(Left(Refusals.quantity(text)))(of(_, text))

  def of(n: BigDecimal): Either[Refusal, Long] = of(n, n.toString)

  private def of(n: BigDecimal, text: String): Either[Refusal, Long] =
    if n.isWhole && n > 0 && n.isValidLong then Right(n.toLong) else Left(Refusals.quantity(text))

package com.thinkmorestupidless.predictions.exchange

/** `Quantity.of` over the texts the features use, and the decimal the consumer may decode. */
class QuantitySuite extends munit.FunSuite:

  test("a positive whole number is taken") {
    assertEquals(Quantity.of("10"), Right(10L))
    assertEquals(Quantity.of(BigDecimal(3)), Right(3L))
  }

  test("anything else is refused naming the quantity as given") {
    Vector("0", "-1", "2.5", "ten", "").foreach(text =>
      assertEquals(Quantity.of(text), Left(Refusal("quantity", Map("quantity" -> text))), text)
    )
  }

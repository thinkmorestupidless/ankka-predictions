package com.thinkmorestupidless.predictions.exchange

import java.time.Instant
import scala.collection.immutable.SortedMap

/** One event of every case, for the serializer suites and the journal fixtures. */
object Samples:

  def everyEvent(t: Instant): Vector[ExchangeEvent] =
    import ExchangeEvent.*
    val yes = OutcomeId("yes")
    Vector(
      Opened(MarketId("m-1"), Vector(yes, OutcomeId("no")), 100, 100_000, 10_000, t),
      OrderPlaced(
        OrderId("b-1"),
        OwnerId("ada"),
        yes,
        Side.Buy,
        60,
        10,
        OrderDuration.RestUntilClose,
        600,
        1,
        t
      ),
      TradeMade(
        TradeId.of(1),
        1,
        Through.Direct,
        10,
        SortedMap(yes -> 55L),
        Vector(
          Leg(OwnerId("ada"), OrderId("b-1"), 550, 0, 50, 1),
          Leg(OwnerId("bob"), OrderId("s-1"), 0, 550, 0, 0)
        ),
        t
      ),
      OrderFilled(OrderId("b-1"), t),
      OrderCancelled(OrderId("b-2"), OwnerId("ada"), 600, 1, t),
      OrderExpired(OrderId("b-3"), OwnerId("ada"), 600, 1, t),
      SetsSplit(SetKey("k-1"), OwnerId("ada"), 10, 1000, t),
      SetsMerged(SetKey("k-2"), OwnerId("ada"), 5, 500, t),
      Suspended(t),
      Resumed(t),
      Closed(t),
      PositionPaid(PositionKey.of(OwnerId("ada"), yes), OwnerId("ada"), yes, 10, 1000, t),
      RemainderKept(1, t),
      Final(Finality.Resolved(yes), t),
      Final(Finality.Void, t)
    )

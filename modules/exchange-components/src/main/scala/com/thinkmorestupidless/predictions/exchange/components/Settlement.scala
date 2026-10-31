package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.ComponentId
import com.thinkmorestupidless.ankka.sdk.{ChangeSource, Consumer, ConsumerContext}
import com.thinkmorestupidless.predictions.exchange.*

/**
 * Moves the money the exchange's events say moved, through the ledger, at least once: each call
 * keyed as the event says (spec 005 FR-018), so a repeat is answered as the first was. A ledger
 * that throws is asked again by the runtime; a ledger that refuses is logged with the key and the
 * event is passed, since asking again would be refused again for ever.
 */
final class Settlement(ledger: LedgerPort) extends Consumer[ExchangeEvent, Unit]:

  def onMessage(event: ExchangeEvent): Effect =
    val market = MarketId(messageContext.subject)
    import ExchangeEvent.*
    event match
      case e: OrderCancelled if e.released > 0 =>
        answered(market, s"release ${e.order.value}/${e.releaseOrdinal}")(
          ledger.release(market, e.order, e.releaseOrdinal, Money(e.released, e.owner))
        )
      case e: OrderExpired if e.released > 0 =>
        answered(market, s"release ${e.order.value}/${e.releaseOrdinal}")(
          ledger.release(market, e.order, e.releaseOrdinal, Money(e.released, e.owner))
        )
      case e: TradeMade =>
        answered(market, s"settle ${e.trade.value}")(
          ledger.settle(market, e.trade, e.through, e.legs)
        )
      case e: SetsSplit =>
        answered(market, s"charge ${e.key.value}")(
          ledger.charge(market, e.key, Money(e.charge, e.owner))
        )
      case e: SetsMerged =>
        answered(market, s"pay ${e.key.value}")(ledger.pay(market, e.key, Money(e.paid, e.owner)))
      case e: PositionPaid if e.paid > 0 =>
        answered(market, s"payout ${e.key.value}")(
          ledger.payout(market, e.key, Money(e.paid, e.owner))
        )
      case _ => effects.done()

  private def answered(market: MarketId, what: String)(answer: LedgerAnswer): Effect =
    answer match
      case LedgerAnswer.Done => effects.done()
      case refused =>
        Settlement.log.log(
          System.Logger.Level.WARNING,
          s"the ledger answered $refused to $what in ${market.value}; the event is passed"
        )
        effects.done()

object Settlement:
  private val log = System.getLogger("com.thinkmorestupidless.predictions.exchange.settlement")

/** The settlement consumer's declaration, built from the consumer's ledger. */
final class SettlementConsumer(ledger: LedgerPort)
    extends Consumer.Companion[Settlement, ExchangeEvent, Unit](
      ComponentId("exchange-settlement"),
      ChangeSource.eventsOf(ExchangeEntity)
    ):
  def create(ctx: ConsumerContext): Settlement = new Settlement(ledger)

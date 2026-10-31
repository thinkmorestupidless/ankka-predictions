package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.predictions.exchange.*

import java.util.concurrent.{ConcurrentLinkedQueue, CountDownLatch, TimeUnit}
import java.util.concurrent.atomic.AtomicInteger
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters.*

/** One call a scripted port was asked: what, in which market, under which key, and its answer. */
final case class LedgerCall(
    what: String,
    market: MarketId,
    key: String,
    amount: Long,
    answer: Either[String, LedgerAnswer]
)

/**
 * A ledger a test scripts per owner: done, insufficient, refused with a reason, or a throw some
 * number of times before an answer. A repeat under a key is answered as the first was, as the
 * consumer's ledger must (spec 004 FR-008); every call is recorded, repeats included.
 */
final class ScriptedLedger extends LedgerPort:

  private enum Script:
    case Answer(answer: LedgerAnswer)
    case Throw(times: Int, answer: LedgerAnswer)

  private val scripts  = TrieMap.empty[OwnerId, Script]
  private val thrown   = TrieMap.empty[OwnerId, AtomicInteger]
  private val answered = TrieMap.empty[(String, String), LedgerAnswer]
  private val holds    = TrieMap.empty[String, (CountDownLatch, CountDownLatch)]
  private val log      = ConcurrentLinkedQueue[LedgerCall]()

  def answer(owner: OwnerId, answer: LedgerAnswer): Unit = scripts(owner) = Script.Answer(answer)

  def throwing(owner: OwnerId, times: Int, thenAnswer: LedgerAnswer = LedgerAnswer.Done): Unit =
    scripts(owner) = Script.Throw(times, thenAnswer)
    thrown(owner) = AtomicInteger()

  /**
   * Hold the next call of `what` until `let` is called: the call is recorded and answered when let
   * go. `held` waits until such a call has arrived.
   */
  def hold(what: String): Unit    = holds(what) = (CountDownLatch(1), CountDownLatch(1))
  def held(what: String): Boolean = holds.get(what).exists(_._1.await(20, TimeUnit.SECONDS))
  def let(what: String): Unit     = holds.remove(what).foreach(_._2.countDown())

  def calls: Vector[LedgerCall]               = log.asScala.toVector
  def calls(what: String): Vector[LedgerCall] = calls.filter(_.what == what)
  def keys(what: String): Vector[String]      = calls(what).map(_.key)
  def clear(): Unit                           = { log.clear(); answered.clear() }

  def reserve(market: MarketId, order: OrderId, money: Money): LedgerAnswer =
    asked("reserve", market, order.value, money.amount, money.owner)

  def release(market: MarketId, order: OrderId, ordinal: Int, money: Money): LedgerAnswer =
    asked("release", market, s"${order.value}/$ordinal", money.amount, money.owner)

  def settle(market: MarketId, trade: TradeId, through: Through, legs: Vector[Leg]): LedgerAnswer =
    asked(
      "settle",
      market,
      trade.value,
      legs.map(_.charge).sum,
      legs.headOption.fold(OwnerId(""))(_.owner)
    )

  def charge(market: MarketId, key: SetKey, money: Money): LedgerAnswer =
    asked("charge", market, key.value, money.amount, money.owner)

  def pay(market: MarketId, key: SetKey, money: Money): LedgerAnswer =
    asked("pay", market, key.value, money.amount, money.owner)

  def payout(market: MarketId, key: PositionKey, money: Money): LedgerAnswer =
    asked("payout", market, key.value, money.amount, money.owner)

  private def asked(
      what: String,
      market: MarketId,
      key: String,
      amount: Long,
      owner: OwnerId
  ): LedgerAnswer =
    holds.get(what).foreach { (arrived, go) =>
      arrived.countDown()
      go.await(60, TimeUnit.SECONDS): Unit
    }
    val qualified = s"${market.value}/$key"
    answered.get(what -> qualified) match
      case Some(first) =>
        log.add(LedgerCall(what, market, key, amount, Right(first))): Unit
        first
      case None =>
        val answer = scripts.get(owner) match
          case Some(Script.Throw(times, _)) if thrown(owner).getAndIncrement() < times =>
            log.add(LedgerCall(what, market, key, amount, Left("threw"))): Unit
            throw IllegalStateException(s"the scripted ledger threw for ${owner.value}")
          case Some(Script.Throw(_, after)) => after
          case Some(Script.Answer(a))       => a
          case None                         => LedgerAnswer.Done
        answered(what -> qualified) = answer
        log.add(LedgerCall(what, market, key, amount, Right(answer))): Unit
        answer

/** A gate a test scripts per owner; an owner it was not told of is allowed. */
final class ScriptedGate extends GatePort:

  private val scripts = TrieMap.empty[OwnerId, GateAnswer]
  private val log     = ConcurrentLinkedQueue[(OwnerId, MarketId, OrderId)]()

  def answer(owner: OwnerId, answer: GateAnswer): Unit = scripts(owner) = answer

  def asked: Vector[(OwnerId, MarketId, OrderId)] = log.asScala.toVector

  def mayPlace(owner: OwnerId, market: MarketId, order: ExchangeCommand.Place): GateAnswer =
    log.add((owner, market, order.order)): Unit
    scripts.getOrElse(owner, GateAnswer.Allow)

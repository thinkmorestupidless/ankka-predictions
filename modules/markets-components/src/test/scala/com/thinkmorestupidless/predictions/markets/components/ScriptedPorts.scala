package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.predictions.markets.*

import java.util.concurrent.ConcurrentLinkedQueue
import scala.collection.concurrent.TrieMap
import scala.jdk.CollectionConverters.*

/**
 * A resolution port a test scripts per operator: allowed, or refused with a reason. An operator it
 * was not told of is allowed. Every question is recorded with the permission asked for.
 */
final class ScriptedResolution extends ResolutionPort:

  private val scripts = TrieMap.empty[OperatorId, ResolutionAnswer]
  private val log     = ConcurrentLinkedQueue[(String, OperatorId)]()

  def answer(operator: OperatorId, answer: ResolutionAnswer): Unit = scripts(operator) = answer

  def asked: Vector[(String, OperatorId)] = log.asScala.toVector

  private def ask(permission: String, operator: OperatorId): ResolutionAnswer =
    log.add(permission -> operator): Unit
    scripts.getOrElse(operator, ResolutionAnswer.Allow)

  def mayDraft(operator: OperatorId): ResolutionAnswer = ask("draft", operator)
  def mayApprove(operator: OperatorId, market: MarketId): ResolutionAnswer =
    ask("approve", operator)
  def mayResolve(operator: OperatorId, market: MarketId): ResolutionAnswer =
    ask("resolve", operator)
  def mayVoid(operator: OperatorId, market: MarketId): ResolutionAnswer  = ask("void", operator)
  def mayClose(operator: OperatorId, market: MarketId): ResolutionAnswer = ask("close", operator)
  def maySuspend(operator: OperatorId, market: MarketId): ResolutionAnswer =
    ask("suspend", operator)

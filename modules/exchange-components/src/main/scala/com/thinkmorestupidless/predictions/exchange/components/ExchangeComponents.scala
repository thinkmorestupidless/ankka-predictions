package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.ComponentDescriptor

/**
 * The exchange's components, built from the consumer's ports. The library registers none of them:
 * the consumer passes `descriptors` to its own service (research R5).
 */
final class ExchangeComponents(ports: ExchangePorts):
  val exchange: ExchangeEntity.type  = ExchangeEntity
  val placement: PlacementWorkflow   = PlacementWorkflow(ports)
  val positions: PositionRows.type   = PositionRows
  val settlement: SettlementConsumer = SettlementConsumer(ports.ledger)
  val timers: ExchangeTimers.type    = ExchangeTimers

  def descriptors: Vector[ComponentDescriptor] = Vector(
    exchange.descriptor,
    placement.descriptor,
    positions.descriptor,
    settlement.descriptor,
    timers.descriptor
  )

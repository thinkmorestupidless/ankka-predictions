package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.ComponentDescriptor

/**
 * The markets' components, built from the consumer's ports. The library registers none of them: the
 * consumer passes `descriptors` to its own service (research R5).
 */
final class MarketsComponents(ports: MarketPorts):
  val market: MarketEntityCompanion    = MarketEntityCompanion(ports.resolution)
  val catalogue: CatalogueRows.type    = CatalogueRows
  val timers: MarketTimersCompanion    = MarketTimersCompanion(market)
  val schedule: MarketScheduleConsumer = MarketScheduleConsumer(market, timers, ports.timers)

  def descriptors: Vector[ComponentDescriptor] = Vector(
    market.descriptor,
    catalogue.descriptor,
    timers.descriptor,
    schedule.descriptor
  )

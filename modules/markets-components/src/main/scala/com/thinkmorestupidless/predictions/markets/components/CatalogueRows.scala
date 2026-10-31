package com.thinkmorestupidless.predictions.markets.components

import com.thinkmorestupidless.ankka.core.{ComponentId, Serializer}
import com.thinkmorestupidless.ankka.sdk.{View, ViewComponentContext}
import com.thinkmorestupidless.predictions.markets.*

/**
 * The markets by state and by category, each row the market replayed from its events. Kept minimal:
 * a consumer's product catalogue (what a player browses, in what order) is its own.
 */
final class CatalogueView extends View[MarketEvent, Market]:
  def onChange(event: MarketEvent): Effect =
    effects.updateRow(Market.replay(rowState.getOrElse(Market.empty), event))

object CatalogueRows
    extends View.Companion[CatalogueView, MarketEvent, Market](
      ComponentId("market-catalogue"),
      MarketEntityCompanion.events,
      summon[Serializer[Market]]
    ):

  def create(ctx: ViewComponentContext): CatalogueView = new CatalogueView

  val byState = query("by-state")(
    s"SELECT payload FROM $table WHERE payload::jsonb->>'state' = :state ORDER BY row_key"
  )

  val byCategory = query("by-category")(
    s"SELECT payload FROM $table WHERE payload::jsonb->'definition'->>'category' = :category ORDER BY row_key"
  )

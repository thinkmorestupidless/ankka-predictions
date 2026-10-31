package com.thinkmorestupidless.predictions.exchange.components

import com.thinkmorestupidless.ankka.core.{Codecs, ComponentId, Serializer}
import com.thinkmorestupidless.ankka.sdk.{ChangeSource, KeyedView, ViewComponentContext}
import com.thinkmorestupidless.predictions.exchange.*

/**
 * A row of the positions view: a market's terms, kept under the market's id so a split can be read
 * out to every outcome, or one owner's holding in one outcome, under `<market>/<owner>/<outcome>`.
 */
enum PositionRow:
  case Terms(market: MarketId, outcomes: Vector[OutcomeId], unit: Long)
  case Held(market: MarketId, owner: OwnerId, outcome: OutcomeId, quantity: Long, cost: Long)

object PositionRow:
  given Serializer[PositionRow] = Codecs.serializer[PositionRow]("exchange-position-row")

/**
 * Every owner's holdings across markets, from the exchange's events by the rule replay applies
 * (`PositionMoves`). A holding back to nothing is removed. Shares resting under a sell are the
 * entity's to answer (`position`), since an event of a cancel does not say what it rested.
 */
final class PositionRowsView extends KeyedView[PositionRow]:

  def onEvent(event: ExchangeEvent, change: Change): Effect =
    val market = MarketId(change.subject)
    event match
      case e: ExchangeEvent.Opened =>
        effects.updateRow(market.value, PositionRow.Terms(market, e.outcomes, e.unit))
      case other =>
        change.rows.get(market.value) match
          case Some(PositionRow.Terms(_, outcomes, unit)) =>
            val moved = PositionMoves
              .of(other, outcomes, unit)
              .foldLeft(Map.empty[String, PositionRow.Held]) { (rows, m) =>
                val key = PositionRows.key(market, m.owner, m.outcome)
                val held = rows
                  .get(key)
                  .orElse(change.rows.get(key).collect { case h: PositionRow.Held => h })
                val was = held.fold(Position())(h => Position(h.quantity, 0, h.cost))
                val now = m.move(was)
                rows.updated(
                  key,
                  PositionRow.Held(market, m.owner, m.outcome, now.quantity, now.cost)
                )
              }
            val (gone, kept) = moved.partition((_, h) => h.quantity == 0 && h.cost == 0)
            effects.updateRows(kept) ++ effects.deleteRows(gone.keys)
          case _ => effects.ignore()

object PositionRows
    extends KeyedView.Companion[PositionRowsView, PositionRow](
      ComponentId("exchange-positions"),
      summon[Serializer[PositionRow]]
    ):

  def create(ctx: ViewComponentContext): PositionRowsView = new PositionRowsView

  val events = source(ChangeSource.eventsOf(ExchangeEntity))(_.onEvent)

  /** An owner's holdings in every market, by market and outcome. */
  val byOwner = query("by-owner")(
    s"SELECT payload FROM $table WHERE payload::jsonb->>'type' = 'Held' AND payload::jsonb->>'owner' = :owner ORDER BY row_key"
  )

  def key(market: MarketId, owner: OwnerId, outcome: OutcomeId): String =
    s"${market.value}/${owner.value}/${outcome.value}"

package com.thinkmorestupidless.predictions.exchange

/** What one event does to one owner's holding in one outcome. */
final case class PositionMove(owner: OwnerId, outcome: OutcomeId, move: Position => Position)

/**
 * The holdings an event changes, read from the event and the exchange's outcomes and unit alone:
 * the rule `Exchange.replay` applies, and the one a view of positions applies to its rows, so the
 * two cannot drift. A sell's share of its cost is taken in proportion to the quantity sold.
 */
object PositionMoves:

  def of(event: ExchangeEvent, outcomes: Vector[OutcomeId], unit: Long): Vector[PositionMove] =
    import ExchangeEvent.*
    event match
      case e: TradeMade =>
        e.legs.map { leg =>
          leg.side match
            case Side.Buy =>
              PositionMove(
                leg.owner,
                leg.outcome,
                p => p.copy(quantity = p.quantity + e.quantity, cost = p.cost + leg.charge)
              )
            case Side.Sell =>
              PositionMove(
                leg.owner,
                leg.outcome,
                p =>
                  p.copy(
                    quantity = p.quantity - e.quantity,
                    restingUnderSells = p.restingUnderSells - e.quantity,
                    cost = p.cost - share(p, e.quantity)
                  )
              )
        }

      case e: SetsSplit =>
        val perShare = if outcomes.isEmpty then 0L else unit / outcomes.size
        outcomes.map(outcome =>
          PositionMove(
            e.owner,
            outcome,
            p => p.copy(quantity = p.quantity + e.sets, cost = p.cost + e.sets * perShare)
          )
        )

      case e: SetsMerged =>
        outcomes.map(outcome =>
          PositionMove(
            e.owner,
            outcome,
            p => p.copy(quantity = p.quantity - e.sets, cost = p.cost - share(p, e.sets))
          )
        )

      case e: PositionPaid =>
        Vector(PositionMove(e.owner, e.outcome, _.copy(quantity = 0, cost = 0)))

      case _ => Vector.empty

  private def share(p: Position, sold: Long): Long =
    if p.quantity == 0 then 0L else p.cost * sold / p.quantity

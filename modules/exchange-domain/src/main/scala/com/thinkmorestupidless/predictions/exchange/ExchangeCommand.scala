package com.thinkmorestupidless.predictions.exchange

import java.time.Instant

/**
 * What can be asked of an exchange (spec 005 FR-002). Every command carries the time it applies at;
 * the domain reads no clock.
 */
enum ExchangeCommand:
  def at: Instant

  /** Open the exchange of `market` with its outcomes, unit and limits. */
  case Open(
      market: MarketId,
      outcomes: Vector[OutcomeId],
      unit: Long,
      maxOrderCost: Long,
      maxPosition: Long,
      at: Instant
  )

  case Place(
      order: OrderId,
      owner: OwnerId,
      outcome: OutcomeId,
      side: Side,
      price: Long,
      quantity: Long,
      duration: OrderDuration,
      at: Instant
  )

  case Cancel(order: OrderId, owner: OwnerId, at: Instant)
  case Split(key: SetKey, owner: OwnerId, sets: Long, at: Instant)
  case Merge(key: SetKey, owner: OwnerId, sets: Long, at: Instant)
  case Suspend(at: Instant)
  case Resume(at: Instant)
  case Close(at: Instant)
  case Finalise(finality: Finality, at: Instant)

  /** The command's wire name (`contracts/artefacts.md`). */
  def name: String = this match
    case _: Open     => "open"
    case _: Place    => "place"
    case _: Cancel   => "cancel"
    case _: Split    => "split"
    case _: Merge    => "merge"
    case _: Suspend  => "suspend"
    case _: Resume   => "resume"
    case _: Close    => "close"
    case _: Finalise => "finalise"

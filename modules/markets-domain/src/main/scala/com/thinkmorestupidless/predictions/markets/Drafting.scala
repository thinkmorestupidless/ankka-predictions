package com.thinkmorestupidless.predictions.markets

import java.time.{Instant, ZoneOffset}

/**
 * A definition's field rules and which fields a change may touch in each state (spec 006 FR-004,
 * FR-005; research R3, R4). Draft and change both use them; the first rule broken is the refusal.
 */
object Drafting:

  /** The fields' rules in the order research R3 gives, against the command's time. */
  def check(d: Definition, at: Instant): Option[Refusal] =
    val names = d.outcomes.map(_.value)
    val opens = d.openTime.getOrElse(at)
    if !within(d.question, 1, 500) then Some(Refusals.field("question"))
    else if length(d.description) > 5_000 then Some(Refusals.field("description"))
    else if blank(d.category) then Some(Refusals.field("category"))
    else if names.size < 2 || names.size > 20 || names.exists(!within(_, 1, 100)) ||
      names.distinct.size != names.size
    then Some(Refusals.field("outcomes"))
    else if blank(d.currency) then Some(Refusals.field("currency"))
    else if !d.close.isAfter(opens) then Some(Refusals.field("close"))
    else if d.expectedResolution.isBefore(d.close.atZone(ZoneOffset.UTC).toLocalDate) then
      Some(Refusals.field("expected resolution date"))
    else if !within(d.criteria, 1, 2_000) then Some(Refusals.field("resolution criteria"))
    else if !within(d.authority, 1, 200) then Some(Refusals.field("authority"))
    else None

  /**
   * Whether a change to `d` may be made to the market at `at`: a draft any field, an approved
   * market not the question, the outcomes or the currency and only with an open time still ahead,
   * an open one to a void one only the description, a final or empty one nothing; then the field
   * rules; then that something changed.
   */
  def change(market: Market, d: Definition, at: Instant): Option[Refusal] =
    market.definition match
      case None => Some(Refusals.state(market.state))
      case Some(was) =>
        val fixed = market.state match
          case MarketState.Draft => false
          case MarketState.Approved =>
            d.question != was.question || d.outcomes != was.outcomes || d.currency != was.currency
          case MarketState.Open | MarketState.Suspended | MarketState.Closed |
              MarketState.Resolved | MarketState.Void =>
            d.copy(description = was.description) != was
          case MarketState.Empty | MarketState.Final => true
        // An approved market keeps an open time ahead, so it is always due to open (research R4).
        val noOpening =
          market.state == MarketState.Approved && d.openTime.forall(!_.isAfter(at))
        if fixed then Some(Refusals.state(market.state))
        else if noOpening then Some(Refusals.field("open time"))
        else if d == was then Some(Refusals.noChange)
        else if d.copy(description = was.description) == was then
          Option.when(length(d.description) > 5_000)(Refusals.field("description"))
        else check(d, at)

  /** Blank: empty or white space only (research R3). */
  def blank(s: String): Boolean = s.isBlank

  private def length(s: String): Int = s.codePointCount(0, s.length)

  private def within(s: String, min: Int, max: Int): Boolean =
    !blank(s) && length(s) >= min && length(s) <= max

Feature: The exchange and markets domains are values
  An exchange and a market are values. A command applied to a value yields a refusal naming its
  reason or the next value and the events that led to it; an event applied to a value yields the
  next value. Nothing in a domain reads a clock, a wallet, a player or a network.

  Scenario: a book rebuilt from its events alone is the book that produced them
    Given an exchange "m-1" that has accepted "twenty" orders and made "six" trades
    When the exchange's events are replayed onto an empty exchange in sequence
    Then the rebuilt exchange equals the exchange that produced them
    And the rebuilt book of "yes" in "m-1" equals the book that produced them

  Scenario: a refused command leaves the book it was applied to unchanged
    Given an exchange "m-1" with "three" resting orders in the book of "yes"
    When a buy order at price "100" is applied to the exchange
    Then the command is refused naming the price
    And the exchange is unchanged
    And no event is produced

  Scenario: a market rebuilt from its events alone is the market that produced them
    Given a market "m-1" that has been approved, opened, closed and resolved
    When the market's events are replayed onto an empty market in sequence
    Then the rebuilt market equals the market that produced them

  Scenario: the time a rule needs is carried by the command, not read
    Given an exchange "m-1" whose last event is at "10:00"
    When a close carrying the time "09:59" is applied to the exchange
    Then the command is refused as out of order
    And the exchange is unchanged

  Scenario: an owner's order that reaches the book behind a later event is accepted at the book's time
    Given an exchange "m-1" whose last event is at "10:00"
    When an order carrying the time "09:59" is applied to the exchange
    Then the order is accepted and its events carry the time "10:00"

  Scenario: every event that moves money names the amount, the owner and what it is keyed by
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "bob" holds "10" shares of "yes" in "m-1"
    And "bob" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-1" for "10" shares of "yes" in "m-1" at price "60"
    Then the trade event names "ada" charged "550" keyed by the order "b-1" and the trade
    And the trade event names "bob" paid "550" keyed by the trade
    And the trade event names "ada" released "50" keyed by the order "b-1"

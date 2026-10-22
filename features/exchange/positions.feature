Feature: Positions
  A trade moves shares between positions; a sell needs shares the player holds and has not
  already offered under another sell order; a position's cost follows its shares.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "bob" holds "100" shares of "yes" in "m-1" at a cost of "5000"

  Scenario: a trade adds the quantity to the buyer's position and removes it from the seller's
    Given "bob" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-1" for "10" shares of "yes" in "m-1" at price "55"
    Then "ada" holds "10" shares of "yes" in "m-1"
    And "bob" holds "90" shares of "yes" in "m-1"

  Scenario: a sell order for more shares than the player holds is refused
    When "bob" places a sell order "s-2" for "150" shares of "yes" in "m-1" at price "55"
    Then the order is refused naming the quantity held

  Scenario: a sell order for shares already resting under another sell order is refused
    Given "bob" placed a sell order "s-3" for "80" shares of "yes" in "m-1" at price "55"
    When "bob" places a sell order "s-4" for "30" shares of "yes" in "m-1" at price "56"
    Then the order is refused naming the quantity held

  Scenario: a cancelled sell order frees its shares to be sold again
    Given "bob" placed a sell order "s-5" for "80" shares of "yes" in "m-1" at price "55"
    And "bob" cancelled the order "s-5"
    When "bob" places a sell order "s-6" for "80" shares of "yes" in "m-1" at price "56"
    Then the order "s-6" rests in the book of "yes" in "m-1" at price "56" for "80" shares

  Scenario: a position shows its quantity and its cost
    When the position of "bob" in "yes" of "m-1" is read
    Then the position of "bob" in "yes" of "m-1" shows "100" shares at a cost of "5000"

  Scenario: a buy adds quantity times price to the position's cost
    Given "bob" placed a sell order "s-7" for "10" shares of "yes" in "m-1" at price "55"
    And "ada" placed a buy order "b-2" for "10" shares of "yes" in "m-1" at price "55"
    When "bob" places a sell order "s-8" for "10" shares of "yes" in "m-1" at price "60"
    And "ada" places a buy order "b-3" for "10" shares of "yes" in "m-1" at price "60"
    Then the position of "ada" in "yes" of "m-1" shows "20" shares at a cost of "1150"

  Scenario: a sale removes the sold shares' part of the position's cost
    Given "bob" placed a sell order "s-9" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-4" for "10" shares of "yes" in "m-1" at price "55"
    Then the position of "bob" in "yes" of "m-1" shows "90" shares at a cost of "4500"

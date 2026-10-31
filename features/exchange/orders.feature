Feature: Placing and cancelling orders
  An order is applied to a market's exchange with its owner, outcome, side, price, quantity and
  duration. The exchange refuses what it cannot accept naming the reason, answers a repeat as it
  answered the first, and lets only the owner cancel. Money is never read: a buy order's cost is
  what the exchange says it is, and the consumer reserves it before the order is applied.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "bob" holds "100" shares of "yes" in "m-1"

  Scenario: a buy order at a price and a quantity rests in the outcome's book at a cost of quantity times price
    When "ada" places a buy order "o-1" for "10" shares of "yes" in "m-1" at price "60"
    Then the order "o-1" rests in the book of "yes" in "m-1" at price "60" for "10" shares
    And the cost of the order "o-1" is "600"

  Scenario Outline: an order at a price outside one to one less than the unit is refused
    When "ada" places a buy order "o-2" for "10" shares of "yes" in "m-1" at price "<price>"
    Then the order is refused naming the price

    Examples:
      | price |
      | 0     |
      | 100   |
      | -5    |

  Scenario Outline: an order for a quantity that is not a positive whole number is refused
    When "ada" places a buy order "o-3" for "<quantity>" shares of "yes" in "m-1" at price "60"
    Then the order is refused naming the quantity

    Examples:
      | quantity |
      | 0        |
      | 2.5      |
      | -1       |

  Scenario: an order naming an outcome the market does not have is refused
    When "ada" places a buy order "o-4" for "10" shares of "maybe" in "m-1" at price "60"
    Then the order is refused naming the outcome

  Scenario: an order on an exchange that is not open is refused naming the state
    Given a closed exchange "m-2" with the outcomes "yes" and "no" and a unit of "100"
    When "ada" places a buy order "o-5" for "10" shares of "yes" in "m-2" at price "60"
    Then the order is refused naming the state "closed"

  Scenario: a buy order whose cost is above the maximum order cost is refused naming the limit
    Given the maximum order cost of "m-1" is "500"
    When "ada" places a buy order "o-6" for "10" shares of "yes" in "m-1" at price "60"
    Then the order is refused naming the maximum order cost

  Scenario: a buy order that would take the position above the maximum position is refused naming the limit
    Given the maximum position of "m-1" is "50"
    And "ada" holds "45" shares of "yes" in "m-1"
    When "ada" places a buy order "o-7" for "10" shares of "yes" in "m-1" at price "60"
    Then the order is refused naming the maximum position

  Scenario: a player cancels a resting buy order and its remaining cost is released
    Given "ada" placed a buy order "o-8" for "10" shares of "yes" in "m-1" at price "60"
    When "ada" cancels the order "o-8"
    Then the order "o-8" is cancelled
    And nothing rests in the book of "yes" in "m-1"
    And "ada" is released "600"

  Scenario: a player's cancel of another player's order is refused
    Given "ada" placed a buy order "o-9" for "10" shares of "yes" in "m-1" at price "60"
    When "bob" cancels the order "o-9"
    Then the cancel is refused naming the owner
    And the order "o-9" rests in the book of "yes" in "m-1" at price "60" for "10" shares

  Scenario: a cancel of an order that is filled is refused naming the state
    Given "bob" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    And "ada" placed a buy order "o-10" for "10" shares of "yes" in "m-1" at price "60"
    When "ada" cancels the order "o-10"
    Then the cancel is refused naming the state "filled"

  Scenario: a repeat of an order under the same id is answered as the first was and changes nothing
    Given "ada" placed a buy order "o-11" for "10" shares of "yes" in "m-1" at price "60"
    When "ada" places a buy order "o-11" for "10" shares of "yes" in "m-1" at price "60"
    Then the answer is the one the first order "o-11" received
    And the order "o-11" rests in the book of "yes" in "m-1" at price "60" for "10" shares
    And no event is produced

  Scenario: an order reusing the id of a different order is refused naming the id
    Given "ada" placed a buy order "o-12" for "10" shares of "yes" in "m-1" at price "60"
    When "ada" places a buy order "o-12" for "20" shares of "yes" in "m-1" at price "60"
    Then the order is refused naming the id "o-12"
    And the order "o-12" rests in the book of "yes" in "m-1" at price "60" for "10" shares

  Scenario: an order on a suspended exchange is refused naming the state
    Given a suspended exchange "m-3" with the outcomes "yes" and "no" and a unit of "100"
    When "ada" places a buy order "o-20" for "10" shares of "yes" in "m-3" at price "60"
    Then the order is refused naming the state "suspended"

  Scenario: a player cancels a resting order on a suspended exchange and its cost is released
    Given "ada" placed a buy order "o-21" for "10" shares of "yes" in "m-1" at price "60"
    And the exchange "m-1" is suspended
    When "ada" cancels the order "o-21"
    Then the order "o-21" is cancelled
    And "ada" is released "600"

  Scenario: a resumed exchange trades again with the orders that rested
    Given "ada" placed a buy order "o-22" for "10" shares of "yes" in "m-1" at price "60"
    And the exchange "m-1" is suspended
    When the exchange "m-1" resumes
    And "bob" places a sell order "o-23" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "60" between the orders "o-22" and "o-23"

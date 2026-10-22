Feature: Close and settlement
  A close expires every resting order and releases its cost; an exchange made final pays every
  position from the pool, once.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "ada" split "10" sets in "m-1"
    And "ada" sold "10" shares of "no" in "m-1" to "bob" at price "40"

  Scenario: a close expires every resting order and releases every cost
    Given "bob" placed a buy order "b-1" for "10" shares of "yes" in "m-1" at price "50"
    When the exchange "m-1" closes
    Then the order "b-1" is expired
    And nothing rests in the book of "yes" in "m-1"
    And "bob" is released "500"

  Scenario: an order on a closed exchange is refused
    Given the exchange "m-1" closed
    When "bob" places a buy order "b-2" for "10" shares of "yes" in "m-1" at price "50"
    Then the order is refused naming the state "closed"

  Scenario: an exchange made final resolved pays one unit per share of the resolved outcome, nothing for the other outcomes, and empties the pool
    Given the exchange "m-1" closed
    When the exchange "m-1" is made final resolved to "yes"
    Then "ada" is paid "1000" for the position in "yes" of "m-1"
    And "bob" is paid nothing
    And the pool of "m-1" holds "0"

  Scenario: an exchange made final void pays every share of every outcome an equal part of one unit
    Given the exchange "m-1" closed
    When the exchange "m-1" is made final void
    Then "ada" is paid "500" for the position in "yes" of "m-1"
    And "bob" is paid "500" for the position in "no" of "m-1"
    And the pool of "m-1" holds "0"

  Scenario: a void whose equal part does not divide keeps the remainder in the pool and records it
    Given an open exchange "m-3" with the outcomes "red", "green" and "blue" and a unit of "100"
    And "cy" split "10" sets in "m-3"
    And the exchange "m-3" closed
    When the exchange "m-3" is made final void
    Then "cy" is paid "330" for the position in "red" of "m-3"
    And "cy" is paid "330" for the position in "green" of "m-3"
    And "cy" is paid "330" for the position in "blue" of "m-3"
    And the pool of "m-3" holds "10" as its remainder

  Scenario: an exchange made final again pays nothing more
    Given the exchange "m-1" closed
    And the exchange "m-1" is made final resolved to "yes"
    When the exchange "m-1" is made final resolved to "yes" again
    Then no event is produced
    And the pool of "m-1" holds "0"

  Scenario: an open exchange cannot be made final
    When the exchange "m-1" is made final resolved to "yes"
    Then the command is refused naming the state "open"
    And the pool of "m-1" holds "1000"

  Scenario: a closed exchange pays nothing until it is made final
    Given the exchange "m-1" closed
    When the pool of "m-1" is read
    Then the pool of "m-1" holds "1000"
    And "ada" is paid nothing

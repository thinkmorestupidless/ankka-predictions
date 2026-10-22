Feature: Prices
  Every outcome has a last price, a best bid, a best ask and a depth, read from the exchange.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "bob" holds "100" shares of "yes" in "m-1"

  Scenario: a trade sets the outcome's last price
    Given "bob" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-1" for "10" shares of "yes" in "m-1" at price "60"
    Then the last price of "yes" in "m-1" is "55"

  Scenario: a trade through the pool sets the last price of both outcomes
    Given "bob" placed a buy order "b-2" for "10" shares of "no" in "m-1" at price "45"
    When "ada" places a buy order "b-3" for "10" shares of "yes" in "m-1" at price "60"
    Then the last price of "yes" in "m-1" is "55"
    And the last price of "no" in "m-1" is "45"

  Scenario: the best bid is the highest resting buy price and the best ask the lowest resting sell price
    Given "ada" placed a buy order "b-4" for "10" shares of "yes" in "m-1" at price "50"
    And "ada" placed a buy order "b-5" for "10" shares of "yes" in "m-1" at price "52"
    And "bob" placed a sell order "s-2" for "10" shares of "yes" in "m-1" at price "58"
    And "bob" placed a sell order "s-3" for "10" shares of "yes" in "m-1" at price "56"
    When the prices of "yes" in "m-1" are read
    Then the best bid of "yes" in "m-1" is "52" and the best ask is "56"

  Scenario: the depth lists the quantity resting at each price
    Given "ada" placed a buy order "b-6" for "10" shares of "yes" in "m-1" at price "50"
    And "ada" placed a buy order "b-7" for "15" shares of "yes" in "m-1" at price "50"
    And "bob" placed a sell order "s-4" for "20" shares of "yes" in "m-1" at price "58"
    When the depth of "yes" in "m-1" is read
    Then the depth shows "25" shares to buy at "50" and "20" shares to sell at "58"

  Scenario: an outcome with no trade has no last price
    When the prices of "no" in "m-1" are read
    Then "no" in "m-1" has no last price

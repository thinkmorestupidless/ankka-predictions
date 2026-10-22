Feature: Matching
  Orders match by price and then by time, at the resting order's price, with partial fills. The
  same orders in the same sequence always produce the same trades.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"
    And "bob" holds "100" shares of "yes" in "m-1"
    And "cy" holds "100" shares of "yes" in "m-1"

  Scenario: a buy order matches the lowest resting sell at or below its price at the resting order's price
    Given "bob" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    And "cy" placed a sell order "s-2" for "10" shares of "yes" in "m-1" at price "58"
    When "ada" places a buy order "b-1" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-1" and "s-1"
    And the order "s-2" rests in the book of "yes" in "m-1" at price "58" for "10" shares

  Scenario: a buy order matches several resting sells in order of price and then time until it is filled
    Given "bob" placed a sell order "s-3" for "10" shares of "yes" in "m-1" at price "55"
    And "cy" placed a sell order "s-4" for "10" shares of "yes" in "m-1" at price "58"
    When "ada" places a buy order "b-2" for "15" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-2" and "s-3"
    And one trade is made for "5" shares at price "58" between the orders "b-2" and "s-4"
    And the order "b-2" is filled
    And the order "s-4" rests in the book of "yes" in "m-1" at price "58" for "5" shares

  Scenario: a buy order larger than every matching sell is filled in part and the rest rests
    Given "bob" placed a sell order "s-5" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-3" for "25" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-3" and "s-5"
    And the order "b-3" rests in the book of "yes" in "m-1" at price "60" for "15" shares

  Scenario: a buy order smaller than the resting sell fills in part and the sell keeps resting with what remains
    Given "bob" placed a sell order "s-6" for "30" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-4" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-4" and "s-6"
    And the order "s-6" rests in the book of "yes" in "m-1" at price "55" for "20" shares

  Scenario: a buy order matches no sell above its price and rests whole
    Given "bob" placed a sell order "s-7" for "10" shares of "yes" in "m-1" at price "65"
    When "ada" places a buy order "b-5" for "10" shares of "yes" in "m-1" at price "60"
    Then no trade is made
    And the order "b-5" rests in the book of "yes" in "m-1" at price "60" for "10" shares

  Scenario: a sell order matches the highest resting buy at or above its price at the resting order's price
    Given "ada" placed a buy order "b-6" for "10" shares of "yes" in "m-1" at price "62"
    And "cy" placed a buy order "b-7" for "10" shares of "yes" in "m-1" at price "60"
    When "bob" places a sell order "s-8" for "10" shares of "yes" in "m-1" at price "58"
    Then one trade is made for "10" shares at price "62" between the orders "b-6" and "s-8"
    And the order "b-7" rests in the book of "yes" in "m-1" at price "60" for "10" shares

  Scenario: two resting orders at one price are matched oldest first
    Given "bob" placed a sell order "s-9" for "10" shares of "yes" in "m-1" at price "55"
    And "cy" placed a sell order "s-10" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-8" for "10" shares of "yes" in "m-1" at price "55"
    Then one trade is made for "10" shares at price "55" between the orders "b-8" and "s-9"
    And the order "s-10" rests in the book of "yes" in "m-1" at price "55" for "10" shares

  Scenario: an order to fill what it can at once and cancel the rest leaves nothing resting
    Given "bob" placed a sell order "s-11" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-9" for "25" shares of "yes" in "m-1" at price "60" to fill at once and cancel the rest
    Then one trade is made for "10" shares at price "55" between the orders "b-9" and "s-11"
    And the order "b-9" is cancelled for "15" shares
    And "ada" is released "900"
    And nothing rests in the book of "yes" in "m-1"

  Scenario: an order to fill whole at once or not at all is refused when the book cannot fill it whole
    Given "bob" placed a sell order "s-12" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-10" for "25" shares of "yes" in "m-1" at price "60" to fill whole or not at all
    Then the order is refused as not fillable whole
    And no trade is made
    And the order "s-12" rests in the book of "yes" in "m-1" at price "55" for "10" shares

  Scenario: a trade at a better price than the buy order charges the trade's price and releases the difference
    Given "bob" placed a sell order "s-13" for "10" shares of "yes" in "m-1" at price "55"
    When "ada" places a buy order "b-11" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-11" and "s-13"
    And the trade charges "ada" "550" and pays "bob" "550"
    And "ada" is released "50"

  Scenario: the same orders in the same sequence produce the same trades
    Given the orders "s-14", "s-15", "b-12" and "b-13" were placed in that order and made the trades "t-1" and "t-2"
    When the same orders are placed again in the same order on a fresh exchange
    Then the trades made are the same as "t-1" and "t-2" in price, quantity and orders

  Scenario: an order never matches an order of the same player
    Given "bob" placed a sell order "s-16" for "10" shares of "yes" in "m-1" at price "55"
    And "cy" placed a sell order "s-17" for "10" shares of "yes" in "m-1" at price "56"
    When "bob" places a buy order "b-14" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "56" between the orders "b-14" and "s-17"
    And the order "s-16" rests in the book of "yes" in "m-1" at price "55" for "10" shares

  Scenario: each trade carries the next sequence number of the exchange
    Given "bob" placed a sell order "s-18" for "10" shares of "yes" in "m-1" at price "55"
    And "cy" placed a sell order "s-19" for "10" shares of "yes" in "m-1" at price "58"
    When "ada" places a buy order "b-15" for "20" shares of "yes" in "m-1" at price "60"
    Then the trade between "b-15" and "s-18" carries the sequence number "1"
    And the trade between "b-15" and "s-19" carries the sequence number "2"

Feature: Complete sets and the pool
  A complete set is bought from and sold to the pool for one unit, and in a two-outcome market
  buys and sells of opposite outcomes meet through the pool. The pool holds one unit for every
  set outstanding; the exchange counts it and the consumer's ledger holds the money.

  Background:
    Given an open exchange "m-1" with the outcomes "yes" and "no" and a unit of "100"

  Scenario: a split pays one unit per set to the pool and gives one share of every outcome
    When "ada" splits "10" sets in "m-1" under the key "sp-1"
    Then "ada" holds "10" shares of "yes" in "m-1"
    And "ada" holds "10" shares of "no" in "m-1"
    And "ada" is charged "1000"
    And the pool of "m-1" holds "1000"

  Scenario: a split that would take a position above the maximum position is refused naming the limit
    Given the maximum position of "m-1" is "50"
    When "ada" splits "60" sets in "m-1" under the key "sp-2"
    Then the split is refused naming the maximum position
    And the pool of "m-1" holds "0"

  Scenario: a merge gives one share of every outcome and receives one unit per set from the pool
    Given "ada" split "10" sets in "m-1"
    When "ada" merges "4" sets in "m-1" under the key "mg-1"
    Then "ada" holds "6" shares of "yes" in "m-1"
    And "ada" holds "6" shares of "no" in "m-1"
    And "ada" is paid "400"
    And the pool of "m-1" holds "600"

  Scenario: a merge of more sets than the player holds of every outcome is refused
    Given "ada" split "10" sets in "m-1"
    And "ada" sold "5" shares of "no" in "m-1" to "bob" at price "40"
    When "ada" merges "8" sets in "m-1" under the key "mg-2"
    Then the merge is refused naming the quantity held

  Scenario: a split or a merge repeated under its key is answered as the first was and changes nothing
    Given "ada" split "10" sets in "m-1" under the key "sp-3"
    When "ada" splits "10" sets in "m-1" under the key "sp-3"
    Then the answer is the one the first split "sp-3" received
    And "ada" holds "10" shares of "yes" in "m-1"
    And the pool of "m-1" holds "1000"
    And no event is produced

  Scenario: the pool holds one unit for every set outstanding
    Given "ada" split "10" sets in "m-1"
    And "bob" split "5" sets in "m-1"
    And "ada" sold "5" shares of "no" in "m-1" to "bob" at price "40"
    When the pool of "m-1" is read
    Then the pool of "m-1" holds "1500"
    And "15" sets are outstanding in "m-1"

  Scenario: every outcome has the same total of shares held, which is the sets outstanding
    Given "ada" split "10" sets in "m-1"
    And "bob" split "5" sets in "m-1"
    And "ada" sold "5" shares of "no" in "m-1" to "bob" at price "40"
    When the positions of "m-1" are read
    Then "15" shares of "yes" are held in "m-1" in total
    And "15" shares of "no" are held in "m-1" in total

  Scenario: in a two-outcome market a buy of one outcome matches a buy of the other whose prices make one unit or more by splitting a set between them
    Given "bob" placed a buy order "b-1" for "10" shares of "no" in "m-1" at price "45"
    When "ada" places a buy order "b-2" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares between the orders "b-2" and "b-1" by splitting a set
    And "ada" holds "10" shares of "yes" in "m-1"
    And "bob" holds "10" shares of "no" in "m-1"
    And the trade charges "ada" "550" and charges "bob" "450"
    And "ada" is released "50"
    And the pool of "m-1" holds "1000"

  Scenario: in a two-outcome market a buy matches a resting sell of its own outcome before a buy of the other at the same price
    Given "ada" split "10" sets in "m-1"
    And "ada" placed a sell order "s-1" for "10" shares of "yes" in "m-1" at price "55"
    And "bob" placed a buy order "b-3" for "10" shares of "no" in "m-1" at price "45"
    When "cy" places a buy order "b-4" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares at price "55" between the orders "b-4" and "s-1"
    And the order "b-3" rests in the book of "no" in "m-1" at price "45" for "10" shares

  Scenario: in a two-outcome market a buy takes the better price of a resting sell of its own outcome and a buy of the other
    Given "ada" split "10" sets in "m-1"
    And "ada" placed a sell order "s-2" for "10" shares of "yes" in "m-1" at price "58"
    And "bob" placed a buy order "b-5" for "10" shares of "no" in "m-1" at price "45"
    When "cy" places a buy order "b-6" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares between the orders "b-6" and "b-5" by splitting a set
    And the trade charges "cy" "550" and charges "bob" "450"
    And the order "s-2" rests in the book of "yes" in "m-1" at price "58" for "10" shares

  Scenario: in a two-outcome market a sell of one outcome matches a sell of the other whose prices make one unit or less by merging a set into the pool
    Given "ada" split "10" sets in "m-1"
    And "bob" split "10" sets in "m-1"
    And "bob" placed a sell order "s-3" for "10" shares of "no" in "m-1" at price "38"
    When "ada" places a sell order "s-4" for "10" shares of "yes" in "m-1" at price "60"
    Then one trade is made for "10" shares between the orders "s-4" and "s-3" by merging a set
    And "ada" holds "0" shares of "yes" in "m-1"
    And "bob" holds "0" shares of "no" in "m-1"
    And the trade pays "bob" "380" and pays "ada" "620"
    And the pool of "m-1" holds "1000"

  Scenario: a market with more than two outcomes matches directly and through splits and merges only
    Given an open exchange "m-3" with the outcomes "red", "green" and "blue" and a unit of "100"
    And "bob" placed a buy order "b-7" for "10" shares of "green" in "m-3" at price "40"
    And "cy" placed a buy order "b-8" for "10" shares of "blue" in "m-3" at price "40"
    When "ada" places a buy order "b-9" for "10" shares of "red" in "m-3" at price "40"
    Then no trade is made
    And the order "b-9" rests in the book of "red" in "m-3" at price "40" for "10" shares

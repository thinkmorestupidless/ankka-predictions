Feature: Settlement
  A trade moves money from the buyer's reservation to the seller's available money, and a
  resolution pays each position.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"
    And a player "bob" with a wallet in "GBP" with available money "500" and reserved money "0"

  Scenario: a trade moves quantity times price from the buyer's reservation to the seller's available money
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-1"
    When the trade "t-1" settles "10" shares at price "60" from the order "o-1" of "ada" to "bob"
    Then "ada" has available money "400" and reserved money "0" in "GBP"
    And "bob" has available money "1100" and reserved money "0" in "GBP"

  Scenario: a trade at a price better than the buyer's order releases the difference to the buyer's available money
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-2" at price "60"
    When the trade "t-2" settles "10" shares at price "55" from the order "o-2" of "ada" to "bob"
    Then "ada" has available money "450" and reserved money "0" in "GBP"
    And "bob" has available money "1050" and reserved money "0" in "GBP"

  Scenario: a trade larger than the buyer's reservation is refused and moves nothing
    Given "300" minor units are reserved for "ada" in "GBP" under the order "o-3"
    When the trade "t-3" settles "10" shares at price "60" from the order "o-3" of "ada" to "bob"
    Then the trade is refused
    And "ada" has available money "700" and reserved money "300" in "GBP"
    And "bob" has available money "500" and reserved money "0" in "GBP"

  Scenario: a repeat of a trade under the same key is answered as the first was and moves nothing more
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-4"
    And the trade "t-4" settled "10" shares at price "60" from the order "o-4" of "ada" to "bob"
    When the trade "t-4" settles "10" shares at price "60" from the order "o-4" of "ada" to "bob"
    Then the answer is the one the first trade "t-4" received
    And "bob" has available money "1100" and reserved money "0" in "GBP"

  Scenario: a payout at resolution credits the position's payout to available money
    When the position "pos-1" of "bob" in the market "m-1" is paid "1500" minor units of "GBP"
    Then "bob" has available money "2000" and reserved money "0" in "GBP"

  Scenario: a repeat of a payout for the same market and position pays nothing more
    Given the position "pos-2" of "bob" in the market "m-1" was paid "1500" minor units of "GBP"
    When the position "pos-2" of "bob" in the market "m-1" is paid "1500" minor units of "GBP"
    Then the answer is the one the first payout of "pos-2" received
    And "bob" has available money "2000" and reserved money "0" in "GBP"

  Scenario: a trade between a player and themselves is refused
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-5"
    When the trade "t-5" settles "10" shares at price "60" from the order "o-5" of "ada" to "ada"
    Then the trade is refused
    And "ada" has available money "400" and reserved money "600" in "GBP"

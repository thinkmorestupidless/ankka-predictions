Feature: Money facts
  Every change to a wallet is a fact, and a wallet is reconstructable from its facts.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"
    And a player "bob" with a wallet in "GBP" with available money "500" and reserved money "0"

  Scenario: every change to a wallet is published as one fact carrying the balances after it
    When "400" minor units are reserved for "ada" in "GBP" under the order "o-1"
    Then one fact "reservation placed" is published for the wallet of "ada" in "GBP"
    And the fact carries available money "600" and reserved money "400"

  Scenario: a trade publishes one fact for the buyer's wallet and one for the seller's
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-2"
    When the trade "t-1" settles "10" shares at price "60" from the order "o-2" of "ada" to "bob"
    Then one fact "trade settled" is published for the wallet of "ada" in "GBP"
    And one fact "trade settled" is published for the wallet of "bob" in "GBP"

  Scenario: folding a wallet's facts in order gives the wallet's balances
    Given "600" minor units are reserved for "ada" in "GBP" under the order "o-3"
    And the trade "t-2" settled "5" shares at price "60" from the order "o-3" of "ada" to "bob"
    And "300" minor units of the reservation under "o-3" are released
    And "200" minor units are held for "ada" in "GBP" under the withdrawal request "w-1"
    When the facts of the wallet of "ada" in "GBP" are folded in order
    Then the folded balances are available money "500" and reserved money "200"
    And "ada" has available money "500" and reserved money "200" in "GBP"

  Scenario: a refused deposit is published as a fact naming the refusal and leaves the balances as they were
    Given the deposit gate answers excluded for "ada"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-1"
    Then one fact "deposit refused" is published for the wallet of "ada" in "GBP"
    And the fact names the refusal "excluded"
    And the fact carries available money "1000" and reserved money "0"

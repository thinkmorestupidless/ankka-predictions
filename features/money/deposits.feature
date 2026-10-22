Feature: Deposits
  Money enters a player's wallet from the payments stack, keyed by its payment, after the
  deposit gate has allowed it.

  Background:
    Given a player "ada" with no wallet in "GBP"

  Scenario: a first deposit creates the player's wallet in that currency and credits available money
    Given the deposit gate allows deposits for "ada"
    When a deposit of "5000" minor units of "GBP" for "ada" arrives under the payment "p-1"
    Then "ada" has a wallet in "GBP" with available money "5000" and reserved money "0"

  Scenario: a deposit the deposit gate allows is credited and answers the balances and the transaction
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate allows deposits for "ada"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-2"
    Then the deposit is credited
    And the answer carries available money "3500", reserved money "0" and the transaction of the deposit

  Scenario: a deposit by an excluded player is refused as excluded and credits nothing
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate answers excluded for "ada"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-3"
    Then the deposit is refused as excluded
    And "ada" has available money "1000" in "GBP"

  Scenario: a deposit by a player cooling off is refused naming the end of the break
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate answers cooling off until "2026-11-01T00:00:00Z" for "ada"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-4"
    Then the deposit is refused as cooling off until "2026-11-01T00:00:00Z"
    And "ada" has available money "1000" in "GBP"

  Scenario: a deposit over a deposit limit is refused naming the period, the limit, the amount used and the amount remaining
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate answers over limit for "ada" with period "weekly", limit "10000", used "9000" and remaining "1000"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-5"
    Then the deposit is refused as over limit with period "weekly", limit "10000", used "9000" and remaining "1000"
    And "ada" has available money "1000" in "GBP"

  Scenario: a deposit when the deposit gate cannot be reached is refused as unavailable and credits nothing
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate cannot be reached
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-6"
    Then the deposit is refused as unavailable
    And "ada" has available money "1000" in "GBP"

  Scenario: a repeat of a deposit under the same payment is answered as the first was and credits nothing more
    Given "ada" has a wallet in "GBP" with available money "1000"
    And the deposit gate allows deposits for "ada"
    And a deposit of "2500" minor units of "GBP" for "ada" was credited under the payment "p-7"
    When a deposit of "2500" minor units of "GBP" for "ada" arrives under the payment "p-7"
    Then the answer is the one the first deposit under "p-7" received
    And "ada" has available money "3500" in "GBP"

  Scenario: the deposit sums the deposit gate is asked cover the last day, week and month of credited deposits
    Given "ada" has a wallet in "GBP" with available money "0"
    And the deposit gate allows deposits for "ada"
    And a deposit of "100" minor units of "GBP" for "ada" was credited "2" hours ago
    And a deposit of "200" minor units of "GBP" for "ada" was credited "3" days ago
    And a deposit of "400" minor units of "GBP" for "ada" was credited "20" days ago
    And a deposit of "800" minor units of "GBP" for "ada" was credited "40" days ago
    When a deposit of "50" minor units of "GBP" for "ada" arrives under the payment "p-8"
    Then the deposit gate was asked with deposit sums "100" over the last day, "300" over the last week and "700" over the last month

  Scenario: a deposit whose amount is not a whole number of minor units is refused naming the amount
    Given the deposit gate allows deposits for "ada"
    When a deposit of "25.00" minor units of "GBP" for "ada" arrives under the payment "p-9"
    Then the deposit is refused naming the amount "25.00"
    And "ada" has no wallet in "GBP"

  Scenario: a later outcome of a payment is recorded against the payment and answers whether it was the first time
    Given "ada" has a wallet in "GBP" with available money "1000"
    And a deposit of "2500" minor units of "GBP" for "ada" was credited under the payment "p-10"
    When the outcome "refunded" of the payment "p-10" is recorded
    Then the outcome is recorded as the first time
    When the outcome "refunded" of the payment "p-10" is recorded
    Then the outcome is recorded as not the first time

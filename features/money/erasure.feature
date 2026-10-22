Feature: Erasure of a player's wallet
  A player's erasure leaves the ledger readable.

  Scenario: a player's erasure leaves every amount, id and time of their wallet readable
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"
    And an operator "olga" adjusted the wallet of "ada" in "GBP" by "-250" minor units with the reason "duplicate credit" of kind "correction" under the key "adj-1"
    When the erasure of "ada" is filed
    Then "ada" has available money "750" and reserved money "0" in "GBP"
    And every transaction of "ada" in "GBP" keeps its amount, its id and its time
    And the reason of the adjustment "adj-1" is erased

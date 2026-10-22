Feature: Adjustments
  An operator corrects a wallet with a keyed, reasoned adjustment.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"
    And an operator "olga"

  Scenario: an operator's adjustment with a reason and a kind changes available money and records the operator
    When "olga" adjusts the wallet of "ada" in "GBP" by "-250" minor units with the reason "duplicate credit" of kind "correction" under the key "adj-1"
    Then "ada" has available money "750" and reserved money "0" in "GBP"
    And the adjustment records "olga" as its actor

  Scenario: an adjustment that would take available money below zero is refused
    When "olga" adjusts the wallet of "ada" in "GBP" by "-1250" minor units with the reason "clawback" of kind "correction" under the key "adj-2"
    Then the adjustment is refused
    And "ada" has available money "1000" and reserved money "0" in "GBP"

  Scenario: a repeat of an adjustment under the same key is answered as the first was
    Given "olga" adjusted the wallet of "ada" in "GBP" by "-250" minor units with the reason "duplicate credit" of kind "correction" under the key "adj-3"
    When "olga" adjusts the wallet of "ada" in "GBP" by "-250" minor units with the reason "duplicate credit" of kind "correction" under the key "adj-3"
    Then the answer is the one the first adjustment under "adj-3" received
    And "ada" has available money "750" and reserved money "0" in "GBP"

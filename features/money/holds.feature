Feature: Holds
  A withdrawal request holds money while it is reviewed and paid.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"

  Scenario: a hold preview answers the available money as the most withdrawable amount and no confirmation required
    When a hold of "700" minor units is previewed for "ada" in "GBP"
    Then the preview answers most withdrawable "1000" and no confirmation required

  Scenario: a hold moves the amount from available money to reserved money under the request
    When "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-1"
    Then "ada" has available money "300" and reserved money "700" in "GBP"

  Scenario: a hold larger than available money is refused as insufficient
    When "1200" minor units are held for "ada" in "GBP" under the withdrawal request "w-2"
    Then the hold is refused as insufficient
    And "ada" has available money "1000" and reserved money "0" in "GBP"

  Scenario: a hold placed while reservations rest takes only from available money
    Given "400" minor units are reserved for "ada" in "GBP" under the order "o-1"
    When "500" minor units are held for "ada" in "GBP" under the withdrawal request "w-3"
    Then "ada" has available money "100" and reserved money "900" in "GBP"
    And "400" minor units stay reserved under the order "o-1"

  Scenario: a release returns the held amount to available money naming the outcome
    Given "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-4"
    When the hold under "w-4" is released with the outcome "cancelled"
    Then "ada" has available money "1000" and reserved money "0" in "GBP"

  Scenario: a confirmation removes the held amount from the wallet
    Given "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-5"
    When the hold under "w-5" is confirmed
    Then "ada" has available money "300" and reserved money "0" in "GBP"

  Scenario: a reversal of a confirmed hold returns the amount to available money
    Given "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-6"
    And the hold under "w-6" was confirmed
    When the hold under "w-6" is reversed
    Then "ada" has available money "1000" and reserved money "0" in "GBP"

  Scenario: a release of a hold already confirmed is refused
    Given "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-7"
    And the hold under "w-7" was confirmed
    When the hold under "w-7" is released with the outcome "cancelled"
    Then the release is refused naming the hold as confirmed
    And "ada" has available money "300" and reserved money "0" in "GBP"

  Scenario: a repeat of a hold under the same request is answered as the first was
    Given "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-8"
    When "700" minor units are held for "ada" in "GBP" under the withdrawal request "w-8"
    Then the answer is the one the first hold under "w-8" received
    And "ada" has available money "300" and reserved money "700" in "GBP"

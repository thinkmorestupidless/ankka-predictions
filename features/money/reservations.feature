Feature: Reservations
  Money is reserved against an order before the order reaches a book, and released when the
  order no longer needs it.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"

  Scenario: a reservation moves the amount from available money to reserved money
    When "400" minor units are reserved for "ada" in "GBP" under the order "o-1"
    Then "ada" has available money "600" and reserved money "400" in "GBP"

  Scenario: a reservation larger than available money is refused as insufficient and moves nothing
    When "1200" minor units are reserved for "ada" in "GBP" under the order "o-2"
    Then the reservation is refused as insufficient
    And "ada" has available money "1000" and reserved money "0" in "GBP"

  Scenario: a release returns part of a reservation to available money and keeps the rest reserved
    Given "400" minor units are reserved for "ada" in "GBP" under the order "o-3"
    When "150" minor units of the reservation under "o-3" are released
    Then "ada" has available money "750" and reserved money "250" in "GBP"

  Scenario: a release of a whole reservation leaves nothing reserved under that order
    Given "400" minor units are reserved for "ada" in "GBP" under the order "o-4"
    When "400" minor units of the reservation under "o-4" are released
    Then "ada" has available money "1000" and reserved money "0" in "GBP"
    And nothing is reserved under the order "o-4"

  Scenario: a release larger than the reservation is refused and moves nothing
    Given "400" minor units are reserved for "ada" in "GBP" under the order "o-5"
    When "500" minor units of the reservation under "o-5" are released
    Then the release is refused
    And "ada" has available money "600" and reserved money "400" in "GBP"

  Scenario: a repeat of a reservation under the same order is answered as the first was and reserves nothing more
    Given "400" minor units are reserved for "ada" in "GBP" under the order "o-6"
    When "400" minor units are reserved for "ada" in "GBP" under the order "o-6"
    Then the answer is the one the first reservation under "o-6" received
    And "ada" has available money "600" and reserved money "400" in "GBP"

Feature: Projects and grants
  The product is two projects, and only the granted routes cross between them.

  Background:
    Given the "predictions" project and the "payments" project are deployed
    And a player "ada" with a wallet in "GBP" with available money "1000" and reserved money "0"

  Scenario: the payments project's merchant may credit a deposit on the wallet
    Given the deposit gate allows deposits for "ada"
    When the service "merchant" of the "payments" project deposits "500" minor units of "GBP" for "ada" under the payment "p-1"
    Then the deposit is credited

  Scenario: a service that was not granted the deposit route is refused
    When the service "ingest" of the "payments" project deposits "500" minor units of "GBP" for "ada" under the payment "p-2"
    Then the deposit is refused as not granted
    And "ada" has available money "1000" in "GBP"

  Scenario: a service within the predictions project may reserve money without a grant
    When the service "exchange" of the "predictions" project reserves "400" minor units for "ada" in "GBP" under the order "o-1"
    Then "ada" has available money "600" and reserved money "400" in "GBP"

  Scenario: a request naming a brand other than the product's matches no route
    When the service "merchant" of the "payments" project deposits "500" minor units of "GBP" for "ada" naming the brand "goldvibe"
    Then the request matches no route
    And "ada" has available money "1000" in "GBP"

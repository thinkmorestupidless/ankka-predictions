Feature: Reading a wallet
  A player reads their own wallet; nobody reads another player's through the player's routes.

  Background:
    Given a player "ada" with a wallet in "GBP" with available money "1250" and reserved money "400"

  Scenario: a player reads their own balance as available and reserved money in major units
    When "ada" reads their balance in "GBP"
    Then the balance shows available money "12.50" and reserved money "4.00"

  Scenario: a player's request about another player's wallet is refused as a mismatch
    Given a player "bob" with a wallet in "GBP" with available money "100" and reserved money "0"
    When "ada" reads the balance of "bob" in "GBP"
    Then the read is refused as a mismatch

  Scenario: a player reads their transactions by type and period, paged
    Given "ada" has "30" transactions of type "deposit" in "GBP" over the last "10" days
    When "ada" reads their transactions of type "deposit" over the last "7" days, page "1" of size "20"
    Then "20" transactions are listed, newest first
    And the total of matching transactions is "21"

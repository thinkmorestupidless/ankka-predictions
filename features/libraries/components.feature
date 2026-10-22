Feature: The components stand on ports
  The entities, views and timers are written against the domains, with the outside world behind a
  ledger port, a gate port and a resolution port the consumer supplies. The consumer registers the
  components; the library registers nothing.

  Background:
    Given an open market "m-1" with the outcomes "yes" and "no" in "GBP"
    And a player "ada" with an active account
    And a ledger port that allows "ada"
    And a gate port that allows "ada"

  Scenario: a ledger that refuses the reservation leaves the book unchanged and the order resting nowhere
    Given a ledger port that refuses "ada" as insufficient
    When "ada" places a buy order "o-1" for "10" shares of "yes" in "m-1" at price "60"
    Then the order is refused as insufficient
    And nothing rests in the book of "yes" in "m-1"

  Scenario: a gate that refuses the order reserves nothing and names the gate's reason
    Given a gate port that refuses "ada" with the reason "cooling off"
    When "ada" places a buy order "o-2" for "10" shares of "yes" in "m-1" at price "60"
    Then the order is refused with the reason "cooling off"
    And the ledger port was not asked to reserve

  Scenario: a trade is settled through the ledger keyed by the trade, and a repeat is answered as the first was
    Given a player "bob" with an active account
    And a ledger port that allows "bob"
    And a gate port that allows "bob"
    And "bob" holds "10" shares of "yes" in "m-1"
    And a resting sell order by "bob" for "10" shares of "yes" in "m-1" at price "60"
    When "ada" places a buy order "o-3" for "10" shares of "yes" in "m-1" at price "60"
    Then the ledger port is asked to settle the trade "t-1" keyed by "t-1"
    And asking the ledger port to settle the trade "t-1" keyed by "t-1" again is answered as the first was

  Scenario: a resolution the resolution port refuses changes nothing
    Given a resolution port that refuses "eve"
    When "eve" resolves "m-1" to "yes"
    Then the resolution is refused
    And "m-1" is an open market

  Scenario: the components are registered by the consumer and never by the library
    Given a service that takes the components and registers none of them
    When the service starts
    Then no book and no market is served

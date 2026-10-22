Feature: The journal is a contract
  The libraries own their events, wire names and manifests, and a consumer's journal holds them.
  Every release keeps a journal written by an earlier release replaying to the same values.

  Scenario: a journal written by an earlier release replays on a later one to the same values
    Given a journal of a book written by the release "1.0.0"
    When the journal is replayed by the release "1.1.0"
    Then the rebuilt book equals the book the release "1.0.0" rebuilds

  Scenario: an event a release no longer produces still replays
    Given a journal with an event the release "1.1.0" no longer produces
    When the journal is replayed by the release "1.1.0"
    Then the event is applied as the release "1.0.0" applied it

  Scenario: an event whose manifest is unknown fails the replay naming the manifest
    Given a journal with an event with the manifest "future.v9"
    When the journal is replayed by the release "1.1.0"
    Then the replay fails naming the manifest "future.v9"
    And no event after it is applied

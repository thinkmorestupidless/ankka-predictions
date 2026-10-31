Feature: A market's life
  A market is drafted, changed while a draft, approved, and opened on its open time or at once.
  The market checks its own shape and refuses what breaks it naming the field; who may act on it
  is the consumer's resolution port, and the market records the operator it is told acted.

  Background:
    Given an operator "olga"

  Scenario: a draft market with a question, a category, two outcomes, a currency, a close and resolution criteria is kept as a draft
    When "olga" drafts a market "m-1" asking "Will Newcastle finish in the top four?" in the category "sport" with the outcomes "yes" and "no" in "GBP" closing at "2027-05-24T00:00:00Z" with the resolution criteria "The final Premier League table of the 2026-27 season" and the authority "premierleague.com"
    Then the market "m-1" is a draft market

  Scenario: a draft with fewer than two outcomes is refused
    When "olga" drafts a market "m-2" with the outcomes "yes"
    Then the draft is refused naming the outcomes

  Scenario: a draft with two outcomes of the same name is refused
    When "olga" drafts a market "m-3" with the outcomes "yes" and "yes"
    Then the draft is refused naming the outcomes

  Scenario: a draft with more than twenty outcomes is refused
    When "olga" drafts a market "m-4" with "21" outcomes
    Then the draft is refused naming the outcomes

  Scenario: a draft whose close is not after its open time is refused
    When "olga" drafts a market "m-5" opening at "2027-01-01T00:00:00Z" and closing at "2026-12-31T00:00:00Z"
    Then the draft is refused naming the close

  Scenario: a draft whose expected resolution date is before its close is refused
    When "olga" drafts a market "m-6" closing at "2027-05-24T00:00:00Z" with the expected resolution date "2027-05-01"
    Then the draft is refused naming the expected resolution date

  Scenario: a draft with an empty question is refused
    When "olga" drafts a market "m-7" asking ""
    Then the draft is refused naming the question

  Scenario: an approved market with no open time opens at once
    Given a draft market "m-8" with no open time
    When "olga" approves the market "m-8"
    Then the market "m-8" is an open market

  Scenario: an approved market with an open time opens when that time comes
    Given a draft market "m-9" opening at "2026-11-01T09:00:00Z"
    And "olga" approved the market "m-9"
    And the market "m-9" is an approved market
    When the time "2026-11-01T09:00:00Z" comes
    Then the market "m-9" is an open market

  Scenario: an approved market is not opened before its open time
    Given an approved market "m-10" opening at "2026-11-01T09:00:00Z"
    When the market "m-10" is opened at "2026-11-01T08:00:00Z"
    Then the opening is refused naming the open time
    And the market "m-10" is an approved market

  Scenario: an approved market whose open time has passed opens at once
    Given a draft market "m-11" opening at "2026-10-01T09:00:00Z"
    When "olga" approves the market "m-11" at "2026-10-21T10:00:00Z"
    Then the market "m-11" is an open market

  Scenario: a draft whose close has passed may not be approved
    Given a draft market "m-12" closing at "2026-10-01T00:00:00Z"
    When "olga" approves the market "m-12" at "2026-10-21T10:00:00Z"
    Then the approval is refused naming the close
    And the market "m-12" is a draft market

  Scenario: a draft may be changed by an operator until it is approved
    Given a draft market "m-13" asking "Will it rain?"
    When "olga" changes the question of the market "m-13" to "Will it rain in Newcastle on Sunday?"
    Then the market "m-13" asks "Will it rain in Newcastle on Sunday?"

  Scenario: an approved market's question and outcomes may not be changed
    Given an approved market "m-14" asking "Will it rain?"
    When "olga" changes the question of the market "m-14" to "Will it snow?"
    Then the change is refused naming the state "approved"
    And the market "m-14" asks "Will it rain?"

  Scenario: an approved market's resolution criteria may be changed before it opens
    Given an approved market "m-24"
    When "olga" changes the resolution criteria of the market "m-24" to "The final Premier League table, as published"
    Then the market "m-24" has the resolution criteria "The final Premier League table, as published"

  Scenario: an open market's resolution criteria may not be changed
    Given an open market "m-25" closing at "2027-05-24T00:00:00Z"
    When "olga" changes the resolution criteria of the market "m-25" to "The table on 1 May"
    Then the change is refused naming the state "open"

  Scenario: an open market's close may be moved later with a reason
    Given an open market "m-15" closing at "2027-05-24T00:00:00Z"
    When "olga" moves the close of the market "m-15" to "2027-05-31T00:00:00Z" with the reason "the season was extended"
    Then the market "m-15" closes at "2027-05-31T00:00:00Z"
    And the market "m-15" records the reason "the season was extended"

  Scenario: an open market's close may not be moved earlier
    Given an open market "m-16" closing at "2027-05-24T00:00:00Z"
    When "olga" moves the close of the market "m-16" to "2027-05-01T00:00:00Z" with the reason "a mistake"
    Then the change is refused naming the close

  Scenario: an operator suspends an open market naming a reason
    Given an open market "m-20" closing at "2027-05-24T00:00:00Z"
    When "olga" suspends the market "m-20" with the reason "the fixture is under investigation"
    Then the market "m-20" is a suspended market
    And the market "m-20" records the reason "the fixture is under investigation"

  Scenario: an operator resumes a suspended market
    Given a suspended market "m-21" closing at "2027-05-24T00:00:00Z"
    When "olga" resumes the market "m-21"
    Then the market "m-21" is an open market

  Scenario: a market that is not open may not be suspended
    Given a draft market "m-22" with no open time
    When "olga" suspends the market "m-22" with the reason "too early"
    Then the suspend is refused naming the state "draft"

  Scenario: a suspended market closes when its close comes
    Given a suspended market "m-23" closing at "2027-05-24T00:00:00Z"
    When the time "2027-05-24T00:00:00Z" comes
    Then the market "m-23" is a closed market

  Scenario: a market records who did each change and when
    Given a draft market "m-17" drafted by "olga" at "2026-10-20T09:00:00Z"
    When "olga" approves the market "m-17" at "2026-10-20T10:00:00Z"
    Then the market "m-17" shows drafted by "olga" at "2026-10-20T09:00:00Z"
    And the market "m-17" shows approved by "olga" at "2026-10-20T10:00:00Z"

  Scenario: a market tells the time it is next due to change
    Given an approved market "m-18" opening at "2026-11-01T09:00:00Z" and closing at "2027-05-24T00:00:00Z"
    When the market "m-18" is read
    Then the market "m-18" is next due at "2026-11-01T09:00:00Z"
    When the time "2026-11-01T09:00:00Z" comes
    Then the market "m-18" is next due at "2027-05-24T00:00:00Z"

Feature: Closing and resolving a market
  A market closes on its close or early; an operator resolves it on evidence, may correct the
  resolution within the review window, or voids it; it is final when the window passes.

  Background:
    Given an operator "olga"
    And the review window is "24" hours

  Scenario: an open market closes when its close comes
    Given an open market "m-1" closing at "2027-05-24T00:00:00Z"
    When the time "2027-05-24T00:00:00Z" comes
    Then the market "m-1" is a closed market

  Scenario: an operator closes an open market early naming a reason
    Given an open market "m-2" closing at "2027-05-24T00:00:00Z"
    When "olga" closes the market "m-2" with the reason "the season ended early"
    Then the market "m-2" is a closed market
    And the market "m-2" records the reason "the season ended early"

  Scenario: a closed market is resolved to one of its outcomes on evidence by its authority
    Given a closed market "m-3" with the outcomes "yes" and "no" and the authority "premierleague.com"
    When "olga" resolves the market "m-3" to "yes" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fourth"
    Then the market "m-3" is a resolved market
    And the resolution of "m-3" names the outcome "yes", the evidence and the authority "premierleague.com"

  Scenario: a resolution naming no evidence is refused
    Given a closed market "m-4" with the outcomes "yes" and "no"
    When "olga" resolves the market "m-4" to "yes" on no evidence
    Then the resolution is refused naming the evidence

  Scenario: a resolution naming no authority is refused
    Given a closed market "m-20" with the outcomes "yes" and "no"
    When "olga" resolves the market "m-20" to "yes" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fourth" by no authority
    Then the resolution is refused naming the authority

  Scenario: a resolution naming an outcome the market does not have is refused
    Given a closed market "m-5" with the outcomes "yes" and "no"
    When "olga" resolves the market "m-5" to "maybe" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fourth"
    Then the resolution is refused naming the outcome

  Scenario: an open market may not be resolved
    Given an open market "m-6" with the outcomes "yes" and "no"
    When "olga" resolves the market "m-6" to "yes" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fourth"
    Then the resolution is refused naming the state "open"

  Scenario: a resolution is corrected within the review window on new evidence
    Given a resolved market "m-7" resolved to "yes" "2" hours ago
    When "olga" corrects the resolution of "m-7" to "no" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fifth after a points deduction"
    Then the resolution of "m-7" names the outcome "no"
    And the resolution of "m-7" revises the first

  Scenario: a resolution may not be corrected after the review window
    Given a resolved market "m-8" resolved to "yes" "25" hours ago
    When "olga" corrects the resolution of "m-8" to "no" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fifth"
    Then the correction is refused naming the review window

  Scenario: a correction naming the outcome already resolved is refused as no change
    Given a resolved market "m-9" resolved to "yes" "2" hours ago
    When "olga" corrects the resolution of "m-9" to "yes" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fourth"
    Then the correction is refused as no change

  Scenario: a correction restarts the review window
    Given a resolved market "m-10" resolved to "yes" at "2027-05-25T10:00:00Z"
    When "olga" corrects the resolution of "m-10" to "no" on the evidence "premierleague.com" at "https://premierleague.com/tables" saying "Newcastle finished fifth" at "2027-05-26T08:00:00Z"
    Then the review window of "m-10" ends at "2027-05-27T08:00:00Z"

  Scenario: a resolution is final when the review window passes
    Given a resolved market "m-11" resolved to "yes" at "2027-05-25T10:00:00Z"
    When the time "2027-05-26T10:00:00Z" comes
    Then the market "m-11" is a final market

  Scenario: a resolved market is not made final before the review window passes
    Given a resolved market "m-12" resolved to "yes" at "2027-05-25T10:00:00Z"
    When the market "m-12" is made final at "2027-05-26T09:00:00Z"
    Then the change is refused naming the review window
    And the market "m-12" is a resolved market

  Scenario: an approved market is voided with a reason and evidence
    Given an approved market "m-13"
    When "olga" voids the market "m-13" with the reason "the question was ambiguous" on the evidence "premierleague.com" at "https://premierleague.com/news" saying "the fixture was cancelled"
    Then the market "m-13" is a void market

  Scenario: a void is final when the review window passes
    Given a void market "m-14" voided at "2027-05-25T10:00:00Z"
    When the time "2027-05-26T10:00:00Z" comes
    Then the market "m-14" is a final market

  Scenario: a resolved market is voided within its review window
    Given a resolved market "m-18" resolved to "yes" "2" hours ago
    When "olga" voids the market "m-18" with the reason "the question had no answer" on the evidence "premierleague.com" at "https://premierleague.com/news" saying "the season was abandoned"
    Then the market "m-18" is a void market

  Scenario: a resolved market may not be voided after the review window
    Given a resolved market "m-19" resolved to "yes" "25" hours ago
    When "olga" voids the market "m-19" with the reason "the question had no answer" on the evidence "premierleague.com" at "https://premierleague.com/news" saying "the season was abandoned"
    Then the void is refused naming the review window

  Scenario: a final market may not be voided
    Given a final market "m-15"
    When "olga" voids the market "m-15" with the reason "a mistake" on the evidence "premierleague.com" at "https://premierleague.com/news" saying "the fixture was cancelled"
    Then the void is refused naming the state "final"

  Scenario: a draft market may not be voided
    Given a draft market "m-16"
    When "olga" voids the market "m-16" with the reason "a mistake" on the evidence "premierleague.com" at "https://premierleague.com/news" saying "the fixture was cancelled"
    Then the void is refused naming the state "draft"

  Scenario: a change carrying a time earlier than the market's last change is refused as out of order
    Given a resolved market "m-17" resolved to "yes" at "2027-05-25T10:00:00Z"
    When "olga" closes the market "m-17" at "2027-05-25T09:00:00Z"
    Then the change is refused as out of order
    And the market "m-17" is a resolved market

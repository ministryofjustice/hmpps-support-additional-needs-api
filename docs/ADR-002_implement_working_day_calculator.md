# ADR: Implement a Working Day Calculator within the service

**Status:** Accepted  
**Date:** 2025-07-03

---

## Context

There is a requirement for Plan Creation Schedule dates to be calculated based on Working Days (eg. 5 Working Days from today), and that Working Days has
been defined as Monday to Friday and excluding any Bank Holidays.

In order to obtain Bank Holiday data, there is a [Government maintained service that returns Bank Holidays](https://www.api.gov.uk/gds/bank-holidays/#bank-holidays).

At time of writing there are [several DPS services that implement their own code to retrieve the Bank Holidays data](https://github.com/search?q=org%3Aministryofjustice+%22%2Fbank-holidays.json%22+language%3AKotlin&type=code&l=Kotlin)
and there has been some discussion as to whether the SAN service should follow this pattern, or a common/share service or component
should be developed that implements the code and logic in one place.

---

## Decision

It was decided to **implement the code and logic locally within SAN** following the basic approaches of the existing services,
but to write it in such a way that it can be easily identified and extracted at some point in the future.

To identify the Working Day and Bank Holiday related code that has been implemented in SAN, use the git merge commit that added this file.
All other files committed in this merge are the relevant code and config.

---

## Consequences

### Positive:
- Quickest development option
- Tried and tested approach

### Negative:
- Duplicated approach and code across DPS services

---

## Alternatives Considered

- **Create a shared/common service or component**  
  Implementing this code as either a DPS hosted API service, or as a kotlin library would reduce the duplication; but:
  - would take longer for the SAN team to deliver the SAN commitments
  - would require a migration path for existing services
  - unclear which team would own and maintain the shared service/component

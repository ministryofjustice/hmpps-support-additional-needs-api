package uk.gov.justice.digital.hmpps.supportadditionalneedsapi

import java.util.UUID

/**
 * Builder functions for common data items that are not aligned to a specific domain, REST model or JPA entity; such as
 * prison numbers, times and dates etc.
 */

fun randomValidPrisonNumber(): String {
  fun randomLetters(count: Int) = (1..count).map { ('A'..'Z').random() }.joinToString("")
  fun randomNumbers(count: Int) = (1..count).map { ('0'..'9').random() }.joinToString("")
  return "${randomLetters(1)}${randomNumbers(4)}${randomLetters(2)}"
}

fun aValidReference(): UUID = UUID.randomUUID()

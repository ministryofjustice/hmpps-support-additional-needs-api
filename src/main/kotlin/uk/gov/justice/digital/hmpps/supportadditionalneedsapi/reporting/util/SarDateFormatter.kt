package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.util

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object SarDateFormatter {
  private val dateTimeFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm:ss")
  private val dateFormatter = DateTimeFormatter.ofPattern("dd MMMM yyyy")
  private val ukZone = ZoneId.of("Europe/London")

  fun formatInstant(instant: Instant?): String = instant?.let {
    val ukDateTime = it.atZone(ukZone)
    ukDateTime.format(dateTimeFormatter) + " (${ukDateTime.zone.getId()})"
  } ?: "Not recorded"

  fun formatOffsetDateTime(dateTime: OffsetDateTime?): String = dateTime?.let {
    val ukDateTime = it.atZoneSameInstant(ukZone)
    ukDateTime.format(dateTimeFormatter) + " (${ukDateTime.zone.getId()})"
  } ?: "Not recorded"

  fun formatLocalDate(date: LocalDate?): String = date?.format(dateFormatter) ?: "Not recorded"

  fun formatUser(username: String?): String = username ?: "System"

  fun formatBoolean(value: Boolean): String = if (value) "Yes" else "No"
}

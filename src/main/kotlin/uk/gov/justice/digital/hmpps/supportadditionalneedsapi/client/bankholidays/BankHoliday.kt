package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays

import java.time.LocalDate

data class BankHoliday(
  val title: String,
  val date: LocalDate,
  val notes: String = "",
  val bunting: Boolean = false,
)

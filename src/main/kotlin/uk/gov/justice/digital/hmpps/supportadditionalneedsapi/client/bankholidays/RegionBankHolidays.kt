package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays

data class RegionBankHolidays(
  val division: String,
  val events: List<BankHoliday>,
)

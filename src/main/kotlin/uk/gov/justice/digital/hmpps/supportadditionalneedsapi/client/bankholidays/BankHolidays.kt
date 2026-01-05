package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays

import com.fasterxml.jackson.annotation.JsonProperty

data class BankHolidays(
  @param:JsonProperty("england-and-wales")
  val englandAndWales: RegionBankHolidays,

  val scotland: RegionBankHolidays,

  @param:JsonProperty("northern-ireland")
  val northernIreland: RegionBankHolidays,
)

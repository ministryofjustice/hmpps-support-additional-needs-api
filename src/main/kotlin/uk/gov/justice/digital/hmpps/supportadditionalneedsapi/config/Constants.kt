package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import java.time.LocalDate

class Constants {
  companion object {
    const val PLAN_DEADLINE_DAYS_TO_ADD: Long = 5
    const val REVIEW_DEADLINE_DAYS_TO_ADD: Long = 5
    val IN_THE_FUTURE_DATE: LocalDate = LocalDate.of(2099, 12, 31)
    const val DEFAULT_PRISON_ID = "N/A"
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import java.time.LocalDate

/**
 * If the specified [PlanStatus] does not support a deadline date, or the specified date is 2099-12-31, return null
 * Else return the specified date.
 */
fun deadlineDateIfSupportedByPlanStatus(planStatus: PlanStatus, deadlineDate: LocalDate?): LocalDate? = when {
  planStatus in listOf(PlanStatus.NO_PLAN, PlanStatus.INACTIVE_PLAN, PlanStatus.PLAN_DECLINED, PlanStatus.NEEDS_PLAN) -> null
  Constants.IN_THE_FUTURE_DATE == deadlineDate -> null
  else -> deadlineDate
}

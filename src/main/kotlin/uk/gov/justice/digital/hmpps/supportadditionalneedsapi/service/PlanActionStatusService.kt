package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PrisonerOverviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleExemptionReason

@Service
class PlanActionStatusService(
  private val searchService: SearchService,
  private val prisonerOverviewRepository: PrisonerOverviewRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,

) {
  fun getPlanActionStatus(prisonNumber: String): PlanActionStatus {
    val prisonerOverview = prisonerOverviewRepository.findByPrisonNumber(prisonNumber)
    val status = searchService.determinePlanStatus(prisonerOverview)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    return PlanActionStatus(
      status = status,
      planCreationDeadlineDate = prisonerOverview?.planCreationDeadlineDate,
      reviewDeadlineDate = prisonerOverview?.reviewDeadlineDate,
      exemptionDetail = planCreationSchedule?.exemptionDetail,
      exemptionReason = planCreationSchedule?.exemptionReason?.let {
        PlanCreationScheduleExemptionReason.forValue(it)
      },
    )
  }
}

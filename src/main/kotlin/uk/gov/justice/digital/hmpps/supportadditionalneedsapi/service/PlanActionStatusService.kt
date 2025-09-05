package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PrisonerOverviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.InstantMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleExemptionReason

@Service
class PlanActionStatusService(
  private val searchService: SearchService,
  private val prisonerOverviewRepository: PrisonerOverviewRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val userService: ManageUserService,
  private val instantMapper: InstantMapper,
) {
  fun getPlanActionStatus(prisonNumber: String): PlanActionStatus {
    val prisonerOverview = prisonerOverviewRepository.findByPrisonNumber(prisonNumber)
    val status = searchService.determinePlanStatus(prisonerOverview)
    val isDeclined = prisonerOverview?.planDeclined == true

    // Only get the planCreationSchedule if we need exemption details
    val planCreationSchedule = if (isDeclined) {
      planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    } else {
      null
    }

    return PlanActionStatus(
      status = status,
      planCreationDeadlineDate = prisonerOverview?.planCreationDeadlineDate,
      reviewDeadlineDate = prisonerOverview?.reviewDeadlineDate,

      // Exemption fields only populated when the plan has been declined
      exemptionDetail = if (isDeclined) planCreationSchedule?.exemptionDetail else null,
      exemptionReason = if (isDeclined) {
        planCreationSchedule?.exemptionReason?.let { PlanCreationScheduleExemptionReason.forValue(it) }
      } else {
        null
      },
      exemptionRecordedBy = if (isDeclined) {
        planCreationSchedule?.updatedBy?.let { userService.getUserDetails(it).name }
      } else {
        null
      },
      exemptionRecordedAt = if (isDeclined) {
        planCreationSchedule?.updatedAt?.let { instantMapper.toOffsetDateTime(it) }
      } else {
        null
      },
    )
  }
}

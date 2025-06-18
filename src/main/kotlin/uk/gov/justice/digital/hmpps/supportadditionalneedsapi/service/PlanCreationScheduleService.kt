package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse

@Service
class PlanCreationScheduleService(
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
) {
  fun getSchedules(prisonId: String): PlanCreationSchedulesResponse = PlanCreationSchedulesResponse(
    planCreationScheduleHistoryRepository.findAllByPrisonNumber(prisonId)
      .map { planCreationScheduleHistoryMapper.toModel(it) },
  )

  fun exemptSchedule(prisonNumber: String, status: PlanCreationScheduleStatus) {
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?.takeIf { it.status == PlanCreationScheduleStatus.SCHEDULED }
      ?.let {
        it.status = status
        planCreationScheduleRepository.save(it)
        savePlanCreationScheduleHistory(it)
      }
  }

  private fun savePlanCreationScheduleHistory(planCreationScheduleEntity: PlanCreationScheduleEntity) {
    with(planCreationScheduleEntity) {
      val historyEntry = PlanCreationScheduleHistoryEntity(
        version = planCreationScheduleHistoryRepository.findMaxVersionByScheduleReference(reference)
          ?.plus(1) ?: 1,
        reference = reference,
        prisonNumber = prisonNumber,
        createdAtPrison = createdAtPrison,
        updatedAtPrison = updatedAtPrison,
        updatedAt = updatedAt!!,
        createdAt = createdAt!!,
        updatedBy = updatedBy!!,
        createdBy = createdBy!!,
        status = status,
        exemptionReason = exemptionReason,
        deadlineDate = deadlineDate,
      )
      planCreationScheduleHistoryRepository.save(historyEntry)
    }
  }
}

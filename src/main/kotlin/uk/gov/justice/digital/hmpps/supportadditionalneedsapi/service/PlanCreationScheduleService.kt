package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PlanCreationScheduleService(
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
  private val educationSupportPlanService: EducationSupportPlanService,
  private val educationService: EducationService,
  private val needService: NeedService,
) {

  /**
   * This is called whenever:
   * - An education message is processed
   * - An ALN screener message is processed
   * - SAN Condition or Challenge is created or made inactive.
   */
  @Transactional
  fun attemptToCreate(prisonNumber: String) {
    try {
      educationSupportPlanService.getPlan(prisonNumber)
      return // Plan exists, so exit
    } catch (e: PlanNotFoundException) {
      // No plan found — carry on
    }

    if (planCreationScheduleRepository.findByPrisonNumber(prisonNumber) != null) return

    if (educationService.inEducation(prisonNumber) && needService.hasNeed(prisonNumber)) {
      // Create a new schedule
      val planCreationSchedule = PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        status = PlanCreationScheduleStatus.SCHEDULED,
        deadlineDate = getDeadlineDate(),
        createdAtPrison = "N/A",
        updatedAtPrison = "N/A",
      )
      planCreationScheduleRepository.saveAndFlush(planCreationSchedule).also { savePlanCreationScheduleHistory(it) }
    }
  }

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

  // TODO This needs to be a date from the PES contract date.
  private fun getDeadlineDate(): LocalDate = LocalDate.now().plus(10, ChronoUnit.DAYS)
}

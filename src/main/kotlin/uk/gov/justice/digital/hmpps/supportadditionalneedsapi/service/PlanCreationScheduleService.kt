package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Service
class PlanCreationScheduleService(
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
  private val educationSupportPlanRepository: ElspPlanRepository,
  private val educationService: EducationService,
  private val needService: NeedService,
  private val eventPublisher: EventPublisher,
  @Value("\${pes_contract_date:}") val pesContractDate: LocalDate,
) {

  /**
   * This is called whenever:
   * - An education message is processed
   * - An ALN screener message is processed
   * - SAN Condition or Challenge is created or made inactive.
   */
  @Transactional
  fun attemptToCreate(prisonNumber: String) {
    if (educationSupportPlanRepository.findByPrisonNumber(prisonNumber) != null) return

    // already have a schedule so exit here.
    if (planCreationScheduleRepository.findByPrisonNumber(prisonNumber) != null) return

    if (educationService.inEducation(prisonNumber) && needService.hasNeed(prisonNumber)) {
      // Create a new schedule
      val planCreationSchedule = PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        status = PlanCreationScheduleStatus.SCHEDULED,
        deadlineDate = getDeadlineDate(),
        createdAtPrison = "N/A",
        updatedAtPrison = "N/A",
        version = 1,
      )
      planCreationScheduleRepository.saveAndFlush(planCreationSchedule)
      eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
    }
  }

  fun getSchedules(prisonId: String, includeAllHistory: Boolean): PlanCreationSchedulesResponse {
    val schedules = planCreationScheduleHistoryRepository
      .findAllByPrisonNumberOrderByVersionAsc(prisonId)

    val models = if (includeAllHistory) {
      schedules.map { planCreationScheduleHistoryMapper.toModel(it) }
    } else {
      schedules.maxByOrNull { it.version!! }
        ?.let { listOf(planCreationScheduleHistoryMapper.toModel(it)) }
        ?: emptyList()
    }

    return PlanCreationSchedulesResponse(models)
  }

  fun exemptSchedule(
    prisonNumber: String,
    status: PlanCreationScheduleStatus,
    exemptionReason: String? = null,
    exemptionDetail: String? = null,
    updatedAtPrison: String = "N/A",
    clearDeadlineDate: Boolean = false,
  ) {
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?.takeIf { it.status == PlanCreationScheduleStatus.SCHEDULED }
      ?.let {
        it.status = status
        it.exemptionReason = exemptionReason
        it.exemptionDetail = exemptionDetail
        it.updatedAtPrison = updatedAtPrison
        if (clearDeadlineDate) {
          it.deadlineDate = null
        }
        planCreationScheduleRepository.save(it)
        eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
      }
  }

  fun getDeadlineDate(): LocalDate {
    val todayPlusTen = LocalDate.now().plus(10, ChronoUnit.DAYS)
    val pesPlusTen = pesContractDate.plus(10, ChronoUnit.DAYS)
    return maxOf(todayPlusTen, pesPlusTen)
  }

  fun completeSchedule(prisonNumber: String, prisonId: String) {
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?.takeIf { it.status == PlanCreationScheduleStatus.SCHEDULED }
      ?.let {
        it.status = PlanCreationScheduleStatus.COMPLETED
        it.updatedAtPrison = prisonId
        planCreationScheduleRepository.save(it)
        eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
      }
  }
}

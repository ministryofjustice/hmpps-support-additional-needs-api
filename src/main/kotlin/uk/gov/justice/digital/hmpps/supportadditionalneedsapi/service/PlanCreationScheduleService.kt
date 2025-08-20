package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanCreationScheduleNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanCreationScheduleStateException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class PlanCreationScheduleService(
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
  private val needService: NeedService,
  private val eventPublisher: EventPublisher,
  @Value("\${pes_contract_date:}") val pesContractDate: LocalDate,
  private val elspPlanRepository: ElspPlanRepository,
  private val workingDayService: WorkingDayService,
) {

  private fun updateSchedule(
    schedule: PlanCreationScheduleEntity,
    newStatus: PlanCreationScheduleStatus,
    prisonId: String = "N/A",
    deadlineDate: LocalDate?,
    earliestStartDate: LocalDate?,
    prisonNumber: String,
  ) {
    schedule.status = newStatus
    schedule.updatedAtPrison = prisonId
    schedule.deadlineDate = deadlineDate
    schedule.earliestStartDate = earliestStartDate
    planCreationScheduleRepository.saveAndFlush(schedule)
    eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
  }

  fun createSchedule(prisonNumber: String, prisonId: String = "N/A", deadlineDate: LocalDate?, earliestStartDate: LocalDate?) {
    if (needService.hasNeed(prisonNumber)) {
      log.debug("Person [$prisonNumber] was in education and has a need")
      // Create a new schedule
      val planCreationSchedule = PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        status = PlanCreationScheduleStatus.SCHEDULED,
        deadlineDate = deadlineDate,
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
        needSources = needService.getNeedSources(prisonNumber),
        earliestStartDate = earliestStartDate,
      )
      log.debug("saving plan creation schedule for prisoner $prisonNumber")
      planCreationScheduleRepository.saveAndFlush(planCreationSchedule)
      eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
    }
  }

  fun getSchedules(prisonId: String, includeAllHistory: Boolean): PlanCreationSchedulesResponse {
    val schedules = planCreationScheduleHistoryRepository
      .findAllByPrisonNumberOrderByVersionAsc(prisonId)

    val completedPlan = elspPlanRepository.findByPrisonNumber(prisonId)

    val models = if (includeAllHistory) {
      schedules.map { planCreationScheduleHistoryMapper.toModel(it, completedPlan) }
    } else {
      schedules.maxByOrNull { it.version!! }
        ?.let { listOf(planCreationScheduleHistoryMapper.toModel(it, completedPlan)) }
        ?: emptyList()
    }

    return PlanCreationSchedulesResponse(models)
  }

  @Transactional
  fun exemptScheduleWithValidate(
    prisonNumber: String,
    status: PlanCreationScheduleStatus,
    exemptionReason: String? = null,
    exemptionDetail: String? = null,
    updatedAtPrison: String = "N/A",
    clearDeadlineDate: Boolean = false,
  ) {
    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?: throw PlanCreationScheduleNotFoundException(prisonNumber)

    if (schedule.status != PlanCreationScheduleStatus.SCHEDULED) {
      throw PlanCreationScheduleStateException(
        prisonNumber,
        PlanCreationScheduleStatus.SCHEDULED,
        schedule.status,
      )
    }

    exemptSchedule(
      prisonNumber = prisonNumber,
      status = status,
      exemptionReason = exemptionReason,
      exemptionDetail = exemptionDetail,
      updatedAtPrison = updatedAtPrison,
      clearDeadlineDate = clearDeadlineDate,
    )
  }

  @Transactional
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

  fun getDeadlineDate(educationStartDate: LocalDate): LocalDate {
    val startDatePlusFive = workingDayService.getNextWorkingDayNDaysFromDate(5, educationStartDate)
    val pesPlusFive = workingDayService.getNextWorkingDayNDaysFromDate(5, pesContractDate)
    return maxOf(startDatePlusFive, pesPlusFive)
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

  fun createOrUpdateDueToEducationUpdate(prisonNumber: String, startDate: LocalDate, fundingType: String) {
    val isPES = fundingType.equals("PES", ignoreCase = true)
    val earliestStart = if (isPES) startDate else null
    val deadline = if (isPES) getDeadlineDate(startDate) else null

    val existing = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?: return createSchedule(prisonNumber = prisonNumber, deadlineDate = deadline, earliestStartDate = earliestStart)

    val alreadyCorrect =
      existing.status == PlanCreationScheduleStatus.SCHEDULED &&
        existing.earliestStartDate == earliestStart &&
        existing.deadlineDate == deadline

    if (alreadyCorrect) return

    updateSchedule(
      prisonNumber = prisonNumber,
      deadlineDate = deadline,
      earliestStartDate = earliestStart,
      schedule = existing,
      newStatus = PlanCreationScheduleStatus.SCHEDULED,
    )
  }

  @Transactional
  fun createOrUpdateDueToNeedChange(prisonNumber: String) {
    val existing = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    if (existing == null) {
      return createSchedule(prisonNumber = prisonNumber, deadlineDate = null, earliestStartDate = null)
    }
    // if they already have a schedule then do nothing.
  }
}

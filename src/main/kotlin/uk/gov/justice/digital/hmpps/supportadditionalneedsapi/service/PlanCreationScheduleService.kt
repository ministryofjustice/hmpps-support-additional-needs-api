package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.PLAN_DEADLINE_DAYS_TO_ADD
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
    prisonId: String,
    deadlineDate: LocalDate,
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

  fun createSchedule(
    prisonNumber: String,
    prisonId: String,
    deadlineDate: LocalDate,
    earliestStartDate: LocalDate?,
  ) {
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
    updatedAtPrison: String,
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
      prisonId = updatedAtPrison,
      clearDeadlineDate = clearDeadlineDate,
    )
  }

  @Transactional
  fun exemptSchedule(
    prisonNumber: String,
    status: PlanCreationScheduleStatus,
    exemptionReason: String? = null,
    exemptionDetail: String? = null,
    prisonId: String,
    clearDeadlineDate: Boolean = false,
  ) {
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?.takeIf { it.status == PlanCreationScheduleStatus.SCHEDULED || it.status == PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY }
      ?.let {
        it.status = status
        it.exemptionReason = exemptionReason
        it.exemptionDetail = exemptionDetail
        it.updatedAtPrison = prisonId
        if (clearDeadlineDate) {
          it.deadlineDate = IN_THE_FUTURE_DATE
        }
        planCreationScheduleRepository.save(it)
        eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
      }
  }

  fun getDeadlineDate(educationStartDate: LocalDate): LocalDate {
    val startDatePlusFive =
      workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, educationStartDate)
    val pesPlusFive = workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, pesContractDate)
    return maxOf(startDatePlusFive, pesPlusFive)
  }

  // Can only complete a schedule if it is SCHEDULED or EXEMPT_PRISONER_NOT_COMPLY the latter is because a person may
  // decide to create a plan despite previously not wanting to.
  fun completeSchedule(prisonNumber: String, prisonId: String) {
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?.takeIf { it.status == PlanCreationScheduleStatus.SCHEDULED || it.status == PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY }
      ?.let {
        it.status = PlanCreationScheduleStatus.COMPLETED
        it.updatedAtPrison = prisonId
        planCreationScheduleRepository.save(it)
        eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
      }
  }

  fun createOrUpdateDueToEducationUpdate(prisonNumber: String, startDate: LocalDate, fundingType: String, subjectToKPIRules: Boolean, prisonId: String) {
    val isKPI = fundingType.equals("PES", ignoreCase = true) && subjectToKPIRules
    val earliestStart = if (isKPI) startDate else null
    val deadline = if (isKPI) getDeadlineDate(startDate) else IN_THE_FUTURE_DATE

    val existing = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
      ?: return createSchedule(prisonNumber = prisonNumber, deadlineDate = deadline, earliestStartDate = earliestStart, prisonId = prisonId)

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
      prisonId = prisonId,
    )
  }

  @Transactional
  fun createOrUpdateDueToNeedChange(
    prisonNumber: String,
    educationStartDate: LocalDate?,
    alnAssessmentDate: LocalDate?,
    prisonId: String,
  ) {
    val existing = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    if (existing == null) {
      // need to do another check in here to see if the ALN assessment was done before the education start
      // if it was then we need to create a schedule where the deadline/earliestStartDate date is based on the education start date
      if (alnAssessmentDate != null && educationStartDate != null) {
        if (educationStartDate >= alnAssessmentDate) {
          return createSchedule(
            prisonNumber = prisonNumber,
            deadlineDate = getDeadlineDate(educationStartDate),
            earliestStartDate = educationStartDate,
            prisonId = prisonId,
          )
        }
      }
      return createSchedule(prisonNumber = prisonNumber, deadlineDate = IN_THE_FUTURE_DATE, earliestStartDate = null, prisonId = prisonId)
    } else {
      // need to do check in here to see if the ALN assessment was done before the education start
      // if it was then we need to UPDATE the schedule where the deadline/earliestStartDate date is based on the education start date
      if (alnAssessmentDate != null && educationStartDate != null) {
        if (educationStartDate >= alnAssessmentDate) {
          updateSchedule(
            prisonNumber = prisonNumber,
            schedule = existing,
            newStatus = existing.status,
            earliestStartDate = educationStartDate,
            deadlineDate = getDeadlineDate(educationStartDate),
            prisonId = prisonId,
          )
        }
      }
    }
  }
}

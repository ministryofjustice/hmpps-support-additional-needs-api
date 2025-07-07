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
import java.time.LocalDate
import java.time.temporal.ChronoUnit

private val log = KotlinLogging.logger {}

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
  fun attemptToCreate(prisonNumber: String, prisonId: String = "N/A") {
    log.debug("Attempting to create a new plan creation schedule for prisoner $prisonNumber")
    if (educationSupportPlanRepository.findByPrisonNumber(prisonNumber) != null) return

    // already have a schedule so exit here.
    if (planCreationScheduleRepository.findByPrisonNumber(prisonNumber) != null) return

    if (educationService.inEducation(prisonNumber) && needService.hasNeed(prisonNumber)) {
      log.debug("Person [$prisonNumber] was in education and has a need")
      // Create a new schedule
      val planCreationSchedule = PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        status = PlanCreationScheduleStatus.SCHEDULED,
        deadlineDate = getDeadlineDate(),
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
        needSources = needService.getNeedSources(prisonNumber),
      )
      log.debug("saving plan creation schedule for prisoner $prisonNumber")
      planCreationScheduleRepository.saveAndFlush(planCreationSchedule)
      eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
    }
  }

  /**
   * This is called whenever:
   * - An education message is processed
   * - An ALN screener message is processed
   * - SAN Condition or Challenge is created or made inactive.
   *
   * if there is a schedule and the person no longer has a need or is no longer in education
   * then set the deadline date to null - only reset the deadline date in the case where the person
   * is back in education.
   */
  @Transactional
  fun attemptToUpdate(prisonNumber: String, prisonId: String = "N/A") {
    log.debug("Attempting to update a plan creation schedule for prisoner $prisonNumber")

    if (educationSupportPlanRepository.findByPrisonNumber(prisonNumber) != null) return

    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber) ?: return
    if (planCreationSchedule.status == PlanCreationScheduleStatus.COMPLETED) return

    val inEducation = educationService.inEducation(prisonNumber)
    val hasNeed = needService.hasNeed(prisonNumber)

    when {

      inEducation && hasNeed && planCreationSchedule.status == PlanCreationScheduleStatus.SCHEDULED -> {
        log.debug("Prisoner $prisonNumber has a need and is in education do nothing to the schedule")
      }

      !inEducation && planCreationSchedule.status == PlanCreationScheduleStatus.SCHEDULED -> {
        log.debug("Prisoner $prisonNumber is no longer in education, clearing deadline date, setting status to EXEMPT_NOT_IN_EDUCATION")
        updatePlan(
            planCreationSchedule,
            PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
            prisonId,
            null,
            prisonNumber,
        )
      }

      inEducation && hasNeed && planCreationSchedule.status == PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION -> {
        val deadlineDate = getDeadlineDate()
        log.debug("Prisoner $prisonNumber is back in education, setting deadline date to $deadlineDate, setting status to SCHEDULED")
        updatePlan(
            planCreationSchedule,
            PlanCreationScheduleStatus.SCHEDULED,
            prisonId,
            deadlineDate,
            prisonNumber,
        )
      }

      !hasNeed && planCreationSchedule.status == PlanCreationScheduleStatus.SCHEDULED -> {
        log.debug("Prisoner $prisonNumber no longer has a need, clearing deadline date, setting status to EXEMPT_NO_NEED ")
        updatePlan(
            planCreationSchedule,
            PlanCreationScheduleStatus.EXEMPT_NO_NEED,
            prisonId,
            null,
            prisonNumber,
        )
      }

      hasNeed && planCreationSchedule.status == PlanCreationScheduleStatus.EXEMPT_NO_NEED -> {
        log.debug("Prisoner $prisonNumber has a need but is already in education, leaving deadline date null setting status to SHEDULED")
        updatePlan(
            planCreationSchedule,
            PlanCreationScheduleStatus.SCHEDULED,
            prisonId,
            null,
            prisonNumber,
        )
      }
    }
  }

  private fun updatePlan(
    schedule: PlanCreationScheduleEntity,
    newStatus: PlanCreationScheduleStatus,
    prisonId: String,
    deadlineDate: LocalDate?,
    prisonNumber: String,
  ) {
    schedule.status = newStatus
    schedule.updatedAtPrison = prisonId
    schedule.deadlineDate = deadlineDate
    planCreationScheduleRepository.saveAndFlush(schedule)
    eventPublisher.createAndPublishPlanCreationSchedule(prisonNumber)
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
    val todayPlusTen = LocalDate.now().plus(5, ChronoUnit.DAYS)
    val pesPlusTen = pesContractDate.plus(5, ChronoUnit.DAYS)
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

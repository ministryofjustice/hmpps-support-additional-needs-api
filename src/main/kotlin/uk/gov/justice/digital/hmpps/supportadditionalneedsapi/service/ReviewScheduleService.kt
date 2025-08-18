package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class ReviewScheduleService(
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository,
  private val reviewScheduleHistoryMapper: ReviewScheduleHistoryMapper,
  private val eventPublisher: EventPublisher,
  @Value("\${pes_contract_date:}") val pesContractDate: LocalDate,
  private val workingDayService: WorkingDayService,
) {

  fun getSchedules(prisonId: String): ReviewSchedulesResponse = ReviewSchedulesResponse(
    reviewScheduleHistoryRepository.findAllByPrisonNumber(prisonId).map { reviewScheduleHistoryMapper.toModel(it) },
  )

  fun exemptSchedule(prisonNumber: String, status: ReviewScheduleStatus) {
    reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      ?.takeIf { it.status == ReviewScheduleStatus.SCHEDULED }
      ?.let {
        it.status = status
        reviewScheduleRepository.save(it)
        eventPublisher.createAndPublishReviewScheduleEvent(prisonNumber)
      }
  }

  fun createReviewSchedule(prisonNumber: String, reviewDate: LocalDate?, prisonId: String) {
    val currentSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    if (currentSchedule != null && currentSchedule.status == ReviewScheduleStatus.SCHEDULED) {
      log.info("Review schedule for prison $prisonNumber is already scheduled")
      return
    }
    val reviewScheduleEntity = ReviewScheduleEntity(
      prisonNumber = prisonNumber,
      deadlineDate = reviewDate,
      status = ReviewScheduleStatus.SCHEDULED,
      createdAtPrison = prisonId,
      updatedAtPrison = prisonId,
    )
    reviewScheduleRepository.save(reviewScheduleEntity)
    eventPublisher.createAndPublishReviewScheduleEvent(prisonNumber)
  }

  fun createOrUpdateDueToEducationUpdate(prisonNumber: String, startDate: LocalDate, fundingType: String) {
    val existing = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    val proposedDeadline = getDeadlineDate(startDate)

    // No SCHEDULED review schedule - create a fresh one with the new deadline
    if (existing == null || existing.status != ReviewScheduleStatus.SCHEDULED) {
      createReviewSchedule(
        prisonNumber = prisonNumber,
        reviewDate = proposedDeadline,
        prisonId = "N/A",
      )
      return
    }

    // Choose the earlier of the two deadlines; if unchanged do nothing
    val current = existing.deadlineDate
    val desired = if (current == null) proposedDeadline else minOf(current, proposedDeadline)

    if (current != desired) {
      existing.deadlineDate = desired
      reviewScheduleRepository.save(existing)
      eventPublisher.createAndPublishReviewScheduleEvent(prisonNumber)
    }
  }

  fun getDeadlineDate(educationStartDate: LocalDate): LocalDate {
    val startDatePlusFive = workingDayService.getNextWorkingDayNDaysFromDate(5, educationStartDate)
    val pesPlusFive = workingDayService.getNextWorkingDayNDaysFromDate(5, pesContractDate)
    return maxOf(startDatePlusFive, pesPlusFive)
  }

  fun createOrUpdateDueToNeedChange(prisonNumber: String) {
    reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber) ?: createReviewSchedule(
      prisonNumber = prisonNumber,
      reviewDate = null,
      prisonId = "N/A",
    )
  }
}

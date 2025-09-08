package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.REVIEW_DEADLINE_DAYS_TO_ADD
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
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
  private val reviewRepository: ElspReviewRepository,
) {

  fun getSchedules(prisonNumber: String): ReviewSchedulesResponse {
    // create a map of all the completed reviews:
    val reviews = reviewRepository.findAllByPrisonNumber(prisonNumber).associateBy { it.reviewScheduleReference }
    return ReviewSchedulesResponse(
      reviewScheduleHistoryRepository.findAllByPrisonNumber(prisonNumber)
        .map { reviewScheduleHistoryMapper.toModel(it, reviews) },
    )
  }

  @Transactional
  fun exemptSchedule(prisonNumber: String, status: ReviewScheduleStatus) {
    reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      ?.takeIf { it.status == ReviewScheduleStatus.SCHEDULED }
      ?.let {
        it.status = status
        reviewScheduleRepository.save(it)
        eventPublisher.createAndPublishReviewScheduleEvent(prisonNumber)
      }
  }

  @Transactional
  fun createReviewSchedule(prisonNumber: String, reviewDate: LocalDate, prisonId: String) {
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

  @Transactional
  fun completeExistingAndCreateNextReviewSchedule(
    prisonNumber: String,
    reviewDate: LocalDate,
    prisonId: String,
    existingReviewSchedule: ReviewScheduleEntity,
  ) {
    existingReviewSchedule.status = ReviewScheduleStatus.COMPLETED
    reviewScheduleRepository.save(existingReviewSchedule)
    createReviewSchedule(prisonNumber, reviewDate, prisonId)
  }

  @Transactional
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
    val desired = minOf(current, proposedDeadline)

    if (current != desired) {
      existing.deadlineDate = desired
      reviewScheduleRepository.save(existing)
      eventPublisher.createAndPublishReviewScheduleEvent(prisonNumber)
      log.info("Review schedule deadline date updated to $desired for $prisonNumber, days to add was configured as $REVIEW_DEADLINE_DAYS_TO_ADD days")
    }
  }

  fun getDeadlineDate(educationStartDate: LocalDate): LocalDate {
    val startDatePlusFive = workingDayService.getNextWorkingDayNDaysFromDate(REVIEW_DEADLINE_DAYS_TO_ADD, educationStartDate)
    val pesPlusFive = workingDayService.getNextWorkingDayNDaysFromDate(REVIEW_DEADLINE_DAYS_TO_ADD, pesContractDate)
    return maxOf(startDatePlusFive, pesPlusFive)
  }

  @Transactional
  fun createOrUpdateDueToNeedChange(prisonNumber: String) {
    val existing = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    if (existing == null || existing.status != ReviewScheduleStatus.SCHEDULED) {
      createReviewSchedule(
        prisonNumber = prisonNumber,
        reviewDate = IN_THE_FUTURE_DATE,
        prisonId = "N/A",
      )
    }
    // if they already have a schedule with the status SCHEDULED then do nothing.
  }
}

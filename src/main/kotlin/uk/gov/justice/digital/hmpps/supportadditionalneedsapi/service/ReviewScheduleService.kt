package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class ReviewScheduleService(
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository,
  private val reviewScheduleHistoryMapper: ReviewScheduleHistoryMapper,
  private val eventPublisher: EventPublisher,
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
}

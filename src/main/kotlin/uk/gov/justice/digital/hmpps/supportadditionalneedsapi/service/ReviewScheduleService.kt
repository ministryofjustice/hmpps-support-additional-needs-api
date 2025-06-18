package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse

@Service
class ReviewScheduleService(
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository,
  private val reviewScheduleHistoryMapper: ReviewScheduleHistoryMapper,
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
        saveReviewScheduleHistory(it)
      }
  }

  private fun saveReviewScheduleHistory(reviewScheduleEntity: ReviewScheduleEntity) {
    with(reviewScheduleEntity) {
      val historyEntry = ReviewScheduleHistoryEntity(
        version = reviewScheduleHistoryRepository.findMaxVersionByReviewScheduleReference(reference)
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
      reviewScheduleHistoryRepository.save(historyEntry)
    }
  }
}

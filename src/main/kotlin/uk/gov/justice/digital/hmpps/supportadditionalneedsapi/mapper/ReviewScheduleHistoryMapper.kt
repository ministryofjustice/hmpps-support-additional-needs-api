package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus as ReviewScheduleStatusEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleStatus as ReviewScheduleStatusModel

@Component
class ReviewScheduleHistoryMapper(private val instantMapper: InstantMapper) {

  fun toModel(
    entity: ReviewScheduleHistoryEntity,
  ): ReviewScheduleResponse = with(entity) {
    ReviewScheduleResponse(
      reference = reference,
      deadlineDate = deadlineDate,
      status = toReviewScheduleStatus(status),
      createdBy = createdBy,
      createdByDisplayName = "TODO",
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy,
      updatedByDisplayName = "TODO",
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      exemptionReason = exemptionReason,
      version = version,
    )
  }

  private fun toReviewScheduleStatus(status: ReviewScheduleStatusEntity): ReviewScheduleStatusModel = when (status) {
    ReviewScheduleStatusEntity.SCHEDULED -> ReviewScheduleStatusModel.SCHEDULED
    ReviewScheduleStatusEntity.COMPLETED -> ReviewScheduleStatusModel.COMPLETED
    ReviewScheduleStatusEntity.EXEMPT_SYSTEM_TECHNICAL_ISSUE -> ReviewScheduleStatusModel.EXEMPT_SYSTEM_TECHNICAL_ISSUE
    ReviewScheduleStatusEntity.EXEMPT_PRISONER_TRANSFER -> ReviewScheduleStatusModel.EXEMPT_PRISONER_TRANSFER
    ReviewScheduleStatusEntity.EXEMPT_PRISONER_RELEASE -> ReviewScheduleStatusModel.EXEMPT_PRISONER_RELEASE
    ReviewScheduleStatusEntity.EXEMPT_PRISONER_DEATH -> ReviewScheduleStatusModel.EXEMPT_PRISONER_DEATH
    ReviewScheduleStatusEntity.EXEMPT_PRISONER_MERGE -> ReviewScheduleStatusModel.EXEMPT_PRISONER_MERGE
    ReviewScheduleStatusEntity.EXEMPT_UNKNOWN -> ReviewScheduleStatusModel.EXEMPT_UNKNOWN
  }
}

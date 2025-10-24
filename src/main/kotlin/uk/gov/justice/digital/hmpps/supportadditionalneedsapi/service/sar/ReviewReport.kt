package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewHistoryEntity
import java.time.Instant

data class ReviewReport(
  val prisonerDeclinedFeedback: String,
  val createdBy: String,
  val createdAt: Instant,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: Instant,
  val updatedAtPrison: String,
  val prisonerFeedback: String?,
  val reviewerFeedback: String?,
  val reviewCreatedByJobRole: String?,
)

fun ElspReviewHistoryEntity.toReportModel(): ReviewReport = ReviewReport(
  prisonerDeclinedFeedback = toYesNo(prisonerDeclinedFeedback),
  prisonerFeedback = prisonerFeedback,
  reviewerFeedback = reviewerFeedback,
  reviewCreatedByJobRole = reviewCreatedByJobRole,
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
)

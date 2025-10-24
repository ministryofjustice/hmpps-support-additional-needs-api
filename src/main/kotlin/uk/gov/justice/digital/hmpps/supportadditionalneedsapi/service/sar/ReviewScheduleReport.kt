package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import java.time.Instant
import java.time.LocalDate

data class ReviewScheduleReport(
  val deadlineDate: LocalDate,
  val createdBy: String,
  val createdAt: Instant,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: Instant,
  val updatedAtPrison: String,
  val status: String,
  val exemptionReason: String?,
  val version: Int?,
)

fun ReviewScheduleHistoryEntity.toReportModel(): ReviewScheduleReport = ReviewScheduleReport(
  deadlineDate = deadlineDate,
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  status = toCommaSeparatedString(listOf(status)),
  exemptionReason = exemptionReason,
  version = version,
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import java.time.OffsetDateTime

data class EducationSupportPlanReport(
  val hasCurrentEhcp: String,
  val individualSupport: String,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: OffsetDateTime,
  val updatedAtPrison: String,
  val teachingAdjustments: String? = null,
  val specificTeachingSkills: String? = null,
  val examAccessArrangements: String? = null,
  val lnspSupport: String? = null,
  val lnspSupportHours: Int? = null,
  val detail: String? = null,
)

fun EducationSupportPlanResponse.toReportModel(): EducationSupportPlanReport = EducationSupportPlanReport(
  hasCurrentEhcp = toYesNo(hasCurrentEhcp),
  individualSupport = individualSupport,
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  teachingAdjustments = teachingAdjustments,
  specificTeachingSkills = specificTeachingSkills,
  examAccessArrangements = examAccessArrangements,
  lnspSupport = lnspSupport,
  lnspSupportHours = lnspSupportHours,
  detail = detail,
)

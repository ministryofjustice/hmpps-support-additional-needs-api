package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import java.time.Instant

data class EducationSupportPlanReport(
  val individualSupport: String,
  val createdBy: String,
  val createdAt: Instant,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: Instant,
  val updatedAtPrison: String,
  val hasCurrentEhcp: Boolean? = null,
  val teachingAdjustments: String? = null,
  val specificTeachingSkills: String? = null,
  val examAccessArrangements: String? = null,
  val lnspSupport: String? = null,
  val lnspSupportHours: Int? = null,
  val detail: String? = null,
  val planCreatedByName: String? = null,
  val planCreatedByJobRole: String? = null,
  val otherContributors: List<OtherContributorReport> = emptyList(),
)

data class OtherContributorReport(
  val name: String,
  val jobRole: String,
)

fun ElspPlanHistoryEntity.toReportModel(hasCurrentEhcp: Boolean?): EducationSupportPlanReport = EducationSupportPlanReport(
  individualSupport = individualSupport,
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  hasCurrentEhcp = hasCurrentEhcp,
  teachingAdjustments = teachingAdjustments,
  specificTeachingSkills = specificTeachingSkills,
  examAccessArrangements = examAccessArrangements,
  lnspSupport = lnspSupport,
  lnspSupportHours = lnspSupportHours,
  detail = detail,
  planCreatedByName = planCreatedByName,
  planCreatedByJobRole = planCreatedByJobRole,
  otherContributors = otherContributors.map { OtherContributorReport(name = it.name, jobRole = it.jobRole) },
)

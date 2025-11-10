package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.model

import java.util.UUID

data class SarChronologicalReport(
  val prisonNumber: String,
  val reportGeneratedAt: String,
  val dateRangeFrom: String?,
  val dateRangeTo: String?,
  val plansWithReviews: List<PlanWithReviews>,
  val challenges: List<ChallengeHistory>,
  val strengths: List<StrengthHistory>,
  val conditions: List<ConditionHistory>,
  val supportStrategies: List<SupportStrategyData>,
  val planCreationSchedules: List<PlanCreationScheduleData>,
  val reviewSchedules: List<ReviewScheduleData>,
  val alnScreeners: List<ALNScreenerData>,
)

data class PlanWithReviews(
  val planId: UUID,
  val prisonNumber: String,
  val planVersions: List<PlanVersion>,
  val reviews: List<ReviewWithPlanEdits>,
)

data class PlanVersion(
  val version: Int,
  val planType: String?,
  val mainGoals: String?,
  val prisonName: String,
  val status: String?,
  val statusDetails: String?,
  val exemptionReason: String?,
  val supportedBy: String?,
  val lddSupportHours: String?,
  val lnspSupportHours: String?,
  val supportHoursComments: String?,
  val otherContributors: List<ContributorData>,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
  val changes: List<FieldChange>,
)

data class ReviewWithPlanEdits(
  val reviewId: UUID,
  val reviewVersion: Int,
  val reviewCompletedDate: String?,
  val attendingPeople: List<String>,
  val keyEvents: String?,
  val progressSummary: String?,
  val supportGoals: String?,
  val mainGoals: String?,
  val reviewType: String?,
  val otherContributors: List<ContributorData>,
  val reviewCreatedAt: String,
  val reviewCreatedBy: String?,
  val reviewUpdatedAt: String,
  val reviewUpdatedBy: String?,
  val planEditAfterReview: PlanVersion?,
)

data class ChallengeHistory(
  val challengeId: UUID,
  val challengeType: String?,
  val fromALNScreener: Boolean,
  val versions: List<ChallengeVersion>,
)

data class ChallengeVersion(
  val version: Int,
  val symptoms: String?,
  val howIdentified: String?,
  val howIdentifiedOther: String?,
  val active: Boolean,
  val archiveReason: String?,
  val prisonName: String,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
  val changes: List<FieldChange>,
)

data class StrengthHistory(
  val strengthId: UUID,
  val strengthType: String?,
  val fromALNScreener: Boolean,
  val versions: List<StrengthVersion>,
)

data class StrengthVersion(
  val version: Int,
  val description: String?,
  val active: Boolean,
  val archiveReason: String?,
  val prisonName: String,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
  val changes: List<FieldChange>,
)

data class ConditionHistory(
  val conditionId: UUID,
  val conditionType: String?,
  val versions: List<ConditionVersion>,
)

data class ConditionVersion(
  val version: Int,
  val description: String?,
  val medication: String?,
  val active: Boolean,
  val archiveReason: String?,
  val prisonName: String,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
  val changes: List<FieldChange>,
)

data class ContributorData(
  val name: String?,
  val role: String?,
  val email: String?,
)

data class FieldChange(
  val field: String,
  val oldValue: String?,
  val newValue: String?,
)

data class SupportStrategyData(
  val id: UUID,
  val strategyType: String?,
  val description: String?,
  val active: Boolean,
  val archiveReason: String?,
  val createdAtPrison: String,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
)

data class PlanCreationScheduleData(
  val id: UUID,
  val version: Int,
  val deadlineDate: String?,
  val exemptionReason: String?,
  val sentenceType: String?,
  val sentenceLengthYears: Int?,
  val sentenceLengthMonths: Int?,
  val sentenceLengthDays: Int?,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
)

data class ReviewScheduleData(
  val id: UUID,
  val planId: UUID,
  val version: Int,
  val deadlineDate: String?,
  val exemptionReason: String?,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
)

data class ALNScreenerData(
  val id: UUID,
  val completedDate: String?,
  val challenges: List<String>,
  val strengths: List<String>,
  val createdAt: String,
  val createdBy: String?,
  val updatedAt: String,
  val updatedBy: String?,
)

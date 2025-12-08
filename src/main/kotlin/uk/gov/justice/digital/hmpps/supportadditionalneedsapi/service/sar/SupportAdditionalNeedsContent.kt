package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

data class SupportAdditionalNeedsContent(
  // TODO Add in the EHCP status
  val educationSupportPlans: List<EducationSupportPlanReport>,
  val reviews: List<ReviewReport>,
  val challenges: List<ChallengeReport>,
  val strengths: List<StrengthReport>,
  val conditions: List<ConditionReport>,
  val planCreationSchedules: List<PlanCreationScheduleReport>,
  val reviewSchedules: List<ReviewScheduleReport>,
  val alnScreeners: List<AlnScreenerReport>,
) {
  fun hasContent(): Boolean = educationSupportPlans.isNotEmpty() ||
    challenges.isNotEmpty() ||
    strengths.isNotEmpty() ||
    conditions.isNotEmpty() ||
    planCreationSchedules.isNotEmpty() ||
    reviewSchedules.isNotEmpty() ||
    reviews.isNotEmpty() ||
    alnScreeners.isNotEmpty()
}

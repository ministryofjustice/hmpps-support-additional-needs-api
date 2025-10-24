package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

data class SupportAdditionalNeedsContent(
  val educationSupportPlans: List<EducationSupportPlanReport>,
  val challenges: List<ChallengeReport>,
  val strengths: List<StrengthReport>,
  val conditions: List<ConditionReport>,
) {
  fun hasContent(): Boolean = educationSupportPlans.isNotEmpty() || challenges.isNotEmpty() || strengths.isNotEmpty() || conditions.isNotEmpty() // add the other elements as they come on board
}

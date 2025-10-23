package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

data class SupportAdditionalNeedsContent(
  val educationSupportPlan: EducationSupportPlanReport?,
  val challenges: List<ChallengeReport>,
) {
  fun hasContent(): Boolean = educationSupportPlan != null || challenges.isNotEmpty() // add the other elements as they come on board
}

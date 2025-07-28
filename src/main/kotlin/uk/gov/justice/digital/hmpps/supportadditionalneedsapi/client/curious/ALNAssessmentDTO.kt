package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

import com.fasterxml.jackson.annotation.JsonProperty

data class ALNAssessmentDTO(
  @JsonProperty("v2")
  val data: ALNData?,
) {
  val alnAssessments: List<ALNAssessment>?
    get() = data?.assessments?.aln
}

data class ALNData(
  val assessments: Assessments?,
  val prn: String?,
)

data class Assessments(
  val aln: List<ALNAssessment>?,
)

data class ALNAssessment(
  val assessmentDate: String?,
  val assessmentOutcome: String?,
  val establishmentId: String?,
  val establishmentName: String?,
  val hasPrisonerConsent: String?,
  val stakeholderReferral: String?,
)

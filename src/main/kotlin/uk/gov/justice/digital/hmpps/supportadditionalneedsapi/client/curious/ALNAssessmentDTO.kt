package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

data class ALNAssessmentDTO(
  val v2: V2?,
)

data class V2(
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

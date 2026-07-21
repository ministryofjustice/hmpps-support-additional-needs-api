package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

import java.time.LocalDate

fun aValidEducationDto(
  v2EducationList: List<Education> = listOf(aValidV2Education()),
): EducationDTO = EducationDTO(
  educationData = v2EducationList,
)

fun aValidEducationDto(
  v2Education: Education = aValidV2Education(),
): EducationDTO = aValidEducationDto(listOf(v2Education))

fun aValidV2Education(
  prn: String = "A1234BC",
  establishmentId: String = "CFI",
  establishmentName: String = "CARDIFF (HMP)",
  qualificationCode: String = "60322457",
  qualificationName: String = "Award in Cycle Maintenance",
  fundingType: String = "PES",
  completionStatus: CuriousEducationCompletionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
  learningStartDate: LocalDate = LocalDate.now().minusWeeks(2),
  learningPlannedEndDate: LocalDate? = LocalDate.now().plusMonths(4),
  learningActualEndDate: LocalDate? = null,
  isAccredited: Boolean? = null,
  learnerOnRemand: String? = null,
  aimType: String? = null,
  deliveryApproach: String? = null,
  deliveryLocationpostcode: String? = null,
  outcome: String? = null,
  outcomeDate: LocalDate? = null,
  outcomeGrade: String? = null,
  withdrawalReason: String? = null,
  withdrawalReasonAgreed: String? = null,
  withdrawalReviewed: Boolean? = null,
): Education = Education(
  aimType = aimType,
  completionStatus = completionStatus.value,
  deliveryApproach = deliveryApproach,
  deliveryLocationpostcode = deliveryLocationpostcode,
  establishmentId = establishmentId,
  establishmentName = establishmentName,
  fundingType = fundingType,
  isAccredited = isAccredited,
  learnerOnRemand = learnerOnRemand,
  learningActualEndDate = learningActualEndDate,
  learningPlannedEndDate = learningPlannedEndDate,
  learningStartDate = learningStartDate,
  outcome = outcome,
  outcomeDate = outcomeDate,
  outcomeGrade = outcomeGrade,
  prn = prn,
  qualificationCode = qualificationCode,
  qualificationName = qualificationName,
  withdrawalReason = withdrawalReason,
  withdrawalReasonAgreed = withdrawalReasonAgreed,
  withdrawalReviewed = withdrawalReviewed,
)

enum class CuriousEducationCompletionStatus(val value: String) {
  IN_PROGRESS("The learner is continuing or intending to continue the learning activities leading to the learning aim"),
  COMPLETED("The learner has completed the learning activities leading to the learning aim"),
  WITHDRAWN("The learner has withdrawn from the learning activities leading to the learning aim"),
  TEMPORARILY_WITHDRAWN("Learner has temporarily withdrawn from the aim due to an agreed break in learning"),
}

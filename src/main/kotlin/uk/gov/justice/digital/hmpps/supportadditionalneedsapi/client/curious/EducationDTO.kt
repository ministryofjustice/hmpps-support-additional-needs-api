package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

import com.fasterxml.jackson.annotation.JsonProperty
import java.time.LocalDate

data class EducationDTO(
  @param:JsonProperty("v2")
  val educationData: List<Education>,
)

data class Education(
  val aimType: String? = null,
  val completionStatus: String? = null,
  val deliveryApproach: String? = null,
  val deliveryLocationpostcode: String? = null,
  val establishmentId: String? = null,
  val establishmentName: String? = null,
  val fundingType: String,
  val isAccredited: Boolean? = null,
  val learnerOnRemand: String? = null,
  val learningActualEndDate: LocalDate? = null,
  val learningPlannedEndDate: LocalDate? = null,
  val learningStartDate: LocalDate,
  val outcome: String? = null,
  val outcomeDate: LocalDate? = null,
  val outcomeGrade: String? = null,
  val prn: String,
  val qualificationCode: String,
  val qualificationName: String? = null,
  val withdrawalReason: String? = null,
  val withdrawalReasonAgreed: String? = null,
  val withdrawalReviewed: Boolean? = null,
)

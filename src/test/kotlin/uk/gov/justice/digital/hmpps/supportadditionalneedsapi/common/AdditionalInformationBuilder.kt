package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerMergedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerMergedAdditionalInformation.Reason
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReleasedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.util.*

fun aValidPrisonerReceivedAdditionalInformation(
  prisonNumber: String = randomValidPrisonNumber(),
  prisonId: String = "BXI",
  reason: PrisonerReceivedAdditionalInformation.Reason = PrisonerReceivedAdditionalInformation.Reason.ADMISSION,
  details: String? = "ACTIVE IN:ADM-N",
  currentLocation: PrisonerReceivedAdditionalInformation.Location = PrisonerReceivedAdditionalInformation.Location.IN_PRISON,
  currentPrisonStatus: PrisonerReceivedAdditionalInformation.PrisonStatus = PrisonerReceivedAdditionalInformation.PrisonStatus.UNDER_PRISON_CARE,
  nomisMovementReasonCode: String = "N",
): PrisonerReceivedAdditionalInformation = PrisonerReceivedAdditionalInformation(
  nomsNumber = prisonNumber,
  reason = reason,
  details = details,
  currentLocation = currentLocation,
  prisonId = prisonId,
  nomisMovementReasonCode = nomisMovementReasonCode,
  currentPrisonStatus = currentPrisonStatus,
)

fun aValidPrisonerReleasedAdditionalInformation(
  prisonNumber: String = randomValidPrisonNumber(),
  prisonId: String = "BXI",
  reason: PrisonerReleasedAdditionalInformation.Reason = PrisonerReleasedAdditionalInformation.Reason.RELEASED,
  details: String? = "Movement reason code CR",
  currentLocation: PrisonerReleasedAdditionalInformation.Location = PrisonerReleasedAdditionalInformation.Location.OUTSIDE_PRISON,
  currentPrisonStatus: PrisonerReleasedAdditionalInformation.PrisonStatus = PrisonerReleasedAdditionalInformation.PrisonStatus.NOT_UNDER_PRISON_CARE,
  nomisMovementReasonCode: String = "CR",
): PrisonerReleasedAdditionalInformation = PrisonerReleasedAdditionalInformation(
  nomsNumber = prisonNumber,
  reason = reason,
  details = details,
  currentLocation = currentLocation,
  prisonId = prisonId,
  nomisMovementReasonCode = nomisMovementReasonCode,
  currentPrisonStatus = currentPrisonStatus,
)

fun aValidPrisonerMergedAdditionalInformation(
  prisonNumber: String = randomValidPrisonNumber(),
  removedNomsNumber: String,
  reason: Reason = Reason.MERGE,
): PrisonerMergedAdditionalInformation = PrisonerMergedAdditionalInformation(
  nomsNumber = prisonNumber,
  reason = reason,
  removedNomsNumber = removedNomsNumber,
)

fun aValidEducationALNAssessmentUpdateAdditionalInformation(curiousReference: UUID): EducationALNAssessmentUpdateAdditionalInformation = EducationALNAssessmentUpdateAdditionalInformation(
  curiousExternalReference = curiousReference,
)

fun aValidEducationStatusUpdateAdditionalInformation(curiousReference: UUID): EducationStatusUpdateAdditionalInformation = EducationStatusUpdateAdditionalInformation(
  curiousExternalReference = curiousReference,
)

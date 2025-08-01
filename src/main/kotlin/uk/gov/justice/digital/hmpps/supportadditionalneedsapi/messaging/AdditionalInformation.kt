package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import java.util.*

/**
 * Classes modelling the different structures of Additional Information data for different HMPPS Domain Events.
 *
 * HMPPS Domain Events supports additional information pertaining to the event through an untyped property `additionalInformation`
 * in the event message. Each event type can model additional information as relevant to the event.
 *
 * The classes here are used when deserializing the `additionalInformation` property for each event type.
 */
sealed interface AdditionalInformation {
  /**
   * Additional Information for the Prisoner Received Into Prison (prison-offender-events.prisoner.received) HMPPS Domain Event
   */
  data class PrisonerReceivedAdditionalInformation(
    val nomsNumber: String,
    val reason: Reason,
    val details: String?,
    val currentLocation: Location?,
    val currentPrisonStatus: PrisonStatus?,
    val prisonId: String,
    val nomisMovementReasonCode: String,
  ) : AdditionalInformation {
    enum class Reason {
      ADMISSION,
      TEMPORARY_ABSENCE_RETURN,
      RETURN_FROM_COURT,
      TRANSFERRED,
    }

    enum class Location {
      IN_PRISON,
      OUTSIDE_PRISON,
      BEING_TRANSFERRED,
    }

    enum class PrisonStatus {
      UNDER_PRISON_CARE,
      NOT_UNDER_PRISON_CARE,
    }
  }

  /**
   * Additional Information for the Prisoner Released From Prison (prison-offender-events.prisoner.released) HMPPS Domain Event
   */
  data class PrisonerReleasedAdditionalInformation(
    val nomsNumber: String,
    val reason: Reason,
    val details: String?,
    val currentLocation: Location?,
    val currentPrisonStatus: PrisonStatus?,
    val prisonId: String,
    val nomisMovementReasonCode: String,
  ) : AdditionalInformation {

    val releaseTriggeredByPrisonerDeath: Boolean = nomisMovementReasonCode == "DEC"

    enum class Reason {
      TEMPORARY_ABSENCE_RELEASE,
      RELEASED_TO_HOSPITAL,
      RELEASED,
      SENT_TO_COURT,
      TRANSFERRED,
      UNKNOWN,
    }

    enum class Location {
      IN_PRISON,
      OUTSIDE_PRISON,
      BEING_TRANSFERRED,
    }

    enum class PrisonStatus {
      UNDER_PRISON_CARE,
      NOT_UNDER_PRISON_CARE,
    }
  }

  /**
   * Additional Information for Prisoner Merged (prison-offender-events.prisoner.merged) HMPPS Domain Event
   */
  data class PrisonerMergedAdditionalInformation(
    val nomsNumber: String,
    val reason: Reason,
    val removedNomsNumber: String,

  ) : AdditionalInformation {

    enum class Reason {
      MERGE,
    }
  }

  /**
   * Additional Information for EducationStatusUpdate (prison.education.updated) HMPPS Domain Event
   */
  data class EducationStatusUpdateAdditionalInformation(
    val curiousExternalReference: UUID,
  ) : AdditionalInformation

  /**
   * Additional Information for EducationALNAssessmentUpdate (prison.education-aln-assessment.updated) HMPPS Domain Event
   */
  data class EducationALNAssessmentUpdateAdditionalInformation(
    val curiousExternalReference: UUID,
  ) : AdditionalInformation
}

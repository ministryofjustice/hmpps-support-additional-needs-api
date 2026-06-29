package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.sar

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarChallengeResponse

/**
 * Maps the standard API [ChallengeResponse] into the [SarChallengeResponse] shape used by the Subject Access Request
 * report. The report needs `howIdentified` as a human-readable string rather than a list of enum codes, and renames
 * the `symptoms` field to `challengeDescription`.
 */
@Component
class SarChallengeMapper {

  fun fromResponse(challenge: ChallengeResponse): SarChallengeResponse = with(challenge) {
    SarChallengeResponse(
      active = active,
      challengeType = challengeType,
      archiveReason = archiveReason,
      challengeDescription = symptoms,
      howIdentified = howIdentified
        ?.takeIf { it.isNotEmpty() }
        ?.joinToString(", ") { it.value.lowercase().replace("_", " ") },
      howIdentifiedOther = howIdentifiedOther,
      createdBy = createdBy,
      createdByDisplayName = createdByDisplayName,
      createdAt = createdAt,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy,
      updatedByDisplayName = updatedByDisplayName,
      updatedAt = updatedAt,
      updatedAtPrison = updatedAtPrison,
    )
  }
}

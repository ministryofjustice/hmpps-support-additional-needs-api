package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.sar

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarStrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse

/**
 * Maps the standard API [StrengthResponse] into the [SarStrengthResponse] shape used by the Subject Access Request
 * report. The report needs `howIdentified` as a human-readable string rather than a list of enum codes, and renames
 * the `symptoms` field to `strengthDescription`.
 */
@Component
class SarStrengthMapper {

  fun fromResponse(strength: StrengthResponse): SarStrengthResponse = with(strength) {
    SarStrengthResponse(
      active = active,
      strengthType = strengthType,
      archiveReason = archiveReason,
      strengthDescription = symptoms,
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

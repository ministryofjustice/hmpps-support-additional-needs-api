package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import java.time.LocalDate

@Component
class StrengthMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: StrengthEntity,
    screenerDate: LocalDate? = null,
  ): StrengthResponse = with(entity) {
    StrengthResponse(
      fromALNScreener = fromALNScreener,
      reference = reference,
      createdBy = createdBy!!,
      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      strengthType = strengthType.toModel(),
      symptoms = symptoms,
      howIdentified = IdentificationSourceMapper.toModel(howIdentified),
      howIdentifiedOther = howIdentifiedOther,
      active = active,
      archiveReason = archiveReason,
      alnScreenerDate = if (fromALNScreener) screenerDate else null,
    )
  }

}

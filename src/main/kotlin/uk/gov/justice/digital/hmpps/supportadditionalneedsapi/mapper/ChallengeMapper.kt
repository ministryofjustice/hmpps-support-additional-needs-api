package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import java.time.LocalDate

@Component
class ChallengeMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ChallengeEntity,
    screenerDate: LocalDate? = null,
  ): ChallengeResponse = with(entity) {
    ChallengeResponse(
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
      challengeType = challengeType.toModel(),
      symptoms = symptoms,
      howIdentified = IdentificationSourceMapper.toModel(howIdentified),
      howIdentifiedOther = howIdentifiedOther,
      alnScreenerDate = if (fromALNScreener) screenerDate else null,
      active = active,
      archiveReason = archiveReason,
    )
  }

}

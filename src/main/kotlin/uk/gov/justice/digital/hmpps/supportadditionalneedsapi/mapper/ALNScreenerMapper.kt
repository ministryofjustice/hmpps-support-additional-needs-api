package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class ALNScreenerMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
  private val challengeMapper: ChallengeMapper,
  private val strengthMapper: StrengthMapper,
) {

  fun toModel(entity: ALNScreenerEntity): ALNScreenerResponse = with(entity) {
    val createdByName = userService.getUserDetails(createdBy!!).name
    val updatedByName = userService.getUserDetails(updatedBy!!).name

    ALNScreenerResponse(
      reference = reference,
      screenerDate = screeningDate,
      challenges = entity.challenges.map { challengeMapper.toModel(it, screenerDate = screeningDate) },
      strengths = entity.strengths.map { strengthMapper.toModel(it, screenerDate = screeningDate) },
      createdBy = createdBy!!,
      createdByDisplayName = createdByName,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = updatedByName,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
    )
  }
}

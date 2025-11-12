package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import java.time.LocalDate
import java.util.SortedSet
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource as IdentificationSourceEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

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
      howIdentified = toModel(howIdentified),
      howIdentifiedOther = howIdentifiedOther,
      alnScreenerDate = if (fromALNScreener) screenerDate else null,
      active = active,
      archiveReason = archiveReason,
    )
  }

  private fun toModel(identificationSources: Set<IdentificationSourceEntity>): List<IdentificationSourceModel>? = identificationSources
    .takeIf { it.isNotEmpty() }
    ?.map { IdentificationSourceModel.valueOf(it.name) }

  fun toEntity(identificationSources: List<IdentificationSourceModel>?): SortedSet<IdentificationSourceEntity> = identificationSources
    ?.map { IdentificationSourceEntity.valueOf(it.name) }
    ?.toSortedSet()
    ?: sortedSetOf()
}

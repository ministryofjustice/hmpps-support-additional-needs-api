package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource as IdentificationSourceEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

@Component
class StrengthMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: StrengthEntity,
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
      howIdentified = toModel(howIdentified),
      howIdentifiedOther = howIdentifiedOther,
      active = active,
    )
  }

  private fun toModel(identificationSources: Set<IdentificationSourceEntity>): List<IdentificationSourceModel>? = identificationSources
    .takeIf { it.isNotEmpty() }
    ?.map { IdentificationSourceModel.valueOf(it.name) }

  fun toEntity(identificationSources: List<IdentificationSourceModel>?): Set<IdentificationSourceEntity> = identificationSources
    ?.map { IdentificationSourceEntity.valueOf(it.name) }
    ?.toSet()
    ?: emptySet()
}

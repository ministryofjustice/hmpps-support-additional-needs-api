package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source as EntitySource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source as ModelSource

@Component
class ConditionMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ConditionEntity,
  ): ConditionResponse = with(entity) {
    ConditionResponse(
      source = toModel(source),
      reference = reference,
      createdBy = createdBy!!,
      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      conditionType = conditionType.toModel(),
      active = active,
    )
  }

  fun toModel(source: EntitySource): ModelSource = when (source) {
    EntitySource.ALN_SCREENER -> ModelSource.ALN_SCREENER
    EntitySource.SELF_DECLARED -> ModelSource.SELF_DECLARED
    EntitySource.CONFIRMED_DIAGNOSIS -> ModelSource.CONFIRMED_DIAGNOSIS
  }

  fun toEntity(source: ModelSource): EntitySource = when (source) {
    ModelSource.ALN_SCREENER -> EntitySource.ALN_SCREENER
    ModelSource.SELF_DECLARED -> EntitySource.SELF_DECLARED
    ModelSource.CONFIRMED_DIAGNOSIS -> EntitySource.CONFIRMED_DIAGNOSIS
  }
}

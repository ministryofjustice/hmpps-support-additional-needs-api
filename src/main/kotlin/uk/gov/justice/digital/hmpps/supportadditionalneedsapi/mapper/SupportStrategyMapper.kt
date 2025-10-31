package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class SupportStrategyMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: SupportStrategyEntity,
  ): SupportStrategyResponse = with(entity) {
    SupportStrategyResponse(
      reference = reference,
      createdBy = createdBy!!,
      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      supportStrategyType = supportStrategyType.toModel(),
      active = active,
      detail = detail,
      archiveReason = archiveReason,
    )
  }

  fun toEntity(
    prisonNumber: String,
    supportStrategyType: ReferenceDataEntity,
    requestItem: SupportStrategyRequest,
  ) = SupportStrategyEntity(
    prisonNumber = prisonNumber,
    supportStrategyType = supportStrategyType,
    createdAtPrison = requestItem.prisonId,
    updatedAtPrison = requestItem.prisonId,
    detail = requestItem.detail,
  )
}

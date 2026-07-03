package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EhcpStatusHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EhcpStatusResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class EhcpStatusHistoryMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(entity: EhcpStatusHistoryEntity): EhcpStatusResponse = with(entity) {
    EhcpStatusResponse(
      hasCurrentEhcp = hasCurrentEhcp,
      createdBy = createdBy,
      createdByDisplayName = userService.getUserDetails(createdBy).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy,
      updatedByDisplayName = userService.getUserDetails(updatedBy).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
    )
  }
}

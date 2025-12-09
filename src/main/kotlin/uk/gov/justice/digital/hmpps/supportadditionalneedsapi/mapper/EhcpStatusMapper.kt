package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EhcpStatusEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class EhcpStatusMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toEntity(prisonNumber: String, educationSupportPlanRequest: CreateEducationSupportPlanRequest): EhcpStatusEntity {
    val entity = with(educationSupportPlanRequest) {
      EhcpStatusEntity(
        prisonNumber = prisonNumber,
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
        hasCurrentEhcp = educationSupportPlanRequest.hasCurrentEhcp,
      )
    }
    return entity
  }

  // TODO add this when the model is completed in swagger spec
//  fun toModel(
//    entity: EhcpStatusEntity
//  ): EducationSupportPlanResponse = with(entity) {
//    EhcpStatusResponse(
//      hasCurrentEhcp = hasCurrentEhcp,
//      createdBy = createdBy!!,
//      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
//      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
//      createdAtPrison = createdAtPrison,
//      updatedBy = updatedBy!!,
//      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
//      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
//      updatedAtPrison = updatedAtPrison,
//    )
//  }
}

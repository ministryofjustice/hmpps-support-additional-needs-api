package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EhcpStatusHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class ElspPlanHistoryMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ElspPlanHistoryEntity,
    ehcpStatusHistoryEntity: EhcpStatusHistoryEntity?,
  ): EducationSupportPlanResponse = with(entity) {
    EducationSupportPlanResponse(
      hasCurrentEhcp = ehcpStatusHistoryEntity?.hasCurrentEhcp ?: false,
      planCreatedBy = planCreatedByName?.let { PlanContributor(it, planCreatedByJobRole!!) },
      teachingAdjustments = teachingAdjustments,
      specificTeachingSkills = specificTeachingSkills,
      examAccessArrangements = examAccessArrangements,
      lnspSupport = lnspSupport,
      lnspSupportHours = lnspSupportHours,
      individualSupport = individualSupport,
      detail = detail,
      otherContributors = otherContributors.map { PlanContributor(it.name, it.jobRole) },
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

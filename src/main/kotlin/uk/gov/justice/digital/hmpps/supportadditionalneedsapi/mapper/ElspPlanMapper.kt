package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.OtherContributorEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class ElspPlanMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ElspPlanEntity,
  ): EducationSupportPlanResponse = with(entity) {
    EducationSupportPlanResponse(
      hasCurrentEhcp = hasCurrentEhcp,
      planCreatedBy = planCreatedByName?.let { PlanContributor(planCreatedByName, planCreatedByJobRole!!) },
      learningEnvironmentAdjustments = learningEnvironmentAdjustments,
      teachingAdjustments = teachingAdjustments,
      specificTeachingSkills = specificTeachingSkills,
      examAccessArrangements = examAccessArrangements,
      lnspSupport = lnspSupport,
      otherContributors = otherContributors.map { PlanContributor(it.name, it.jobRole) },
      createdBy = createdBy!!,
      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
    )
  }

  fun toEntity(prisonNumber: String, educationSupportPlanResponse: CreateEducationSupportPlanRequest): ElspPlanEntity {
    val entity = with(educationSupportPlanResponse) {
      ElspPlanEntity(
        prisonNumber = prisonNumber,
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
        planCreatedByName = planCreatedBy?.name,
        planCreatedByJobRole = planCreatedBy?.jobRole,
        hasCurrentEhcp = hasCurrentEhcp,
        lnspSupport = lnspSupport,
        learningEnvironmentAdjustments = learningEnvironmentAdjustments,
        teachingAdjustments = teachingAdjustments,
        examAccessArrangements = examAccessArrangements,
        specificTeachingSkills = specificTeachingSkills,
        otherContributors = mutableListOf(),
      )
    }

    educationSupportPlanResponse.otherContributors?.forEach { contributor ->
      val contributorEntity = OtherContributorEntity(
        name = contributor.name,
        jobRole = contributor.jobRole,
        createdAtPrison = educationSupportPlanResponse.prisonId,
        updatedAtPrison = educationSupportPlanResponse.prisonId,
        elspPlan = entity,
      )
      entity.otherContributors.add(contributorEntity)
    }
    return entity
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanReviewResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class ElspReviewMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ElspReviewEntity,
  ): EducationSupportPlanReviewResponse = with(entity) {
    EducationSupportPlanReviewResponse(
      reviewCreatedBy = reviewCreatedByName?.let { ReviewContributor(reviewCreatedByName, reviewCreatedByJobRole!!) },
      reviewerFeedback = reviewerFeedback ?: "",
      prisonerFeedback = prisonerFeedback ?: "",
      prisonerDeclinedFeedback = prisonerDeclinedFeedback,
      otherContributors = otherContributors.map { ReviewContributor(it.name, it.jobRole) },
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
}

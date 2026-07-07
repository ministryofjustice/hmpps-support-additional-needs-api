package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarEducationSupportPlanReviewResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService

@Component
class SarEducationSupportPlanReviewResponseMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  /**
   * Maps a single Education Support Plan Review for the SAR report.
   *
   * A review in the report is a union of two records that share no id or reference:
   *  - [review] - the review record itself (feedback, contributors) and the created/updated audit shown in the report.
   *  - [planAfterReview] - the version of the plan (from the plan history) as it was after this review was completed;
   *    its plan fields are shown in the report so the reader can see how the plan changed as a result of the review.
   *
   * The association between these two records is made by the caller (by ordinal position), not here.
   */
  fun toModel(
    review: ElspReviewEntity,
    planAfterReview: ElspPlanHistoryEntity,
  ): SarEducationSupportPlanReviewResponse = SarEducationSupportPlanReviewResponse(
    // Plan fields - taken from the version of the plan as it was after this review.
    individualSupport = planAfterReview.individualSupport,
    teachingAdjustments = planAfterReview.teachingAdjustments,
    specificTeachingSkills = planAfterReview.specificTeachingSkills,
    examAccessArrangements = planAfterReview.examAccessArrangements,
    lnspSupport = planAfterReview.lnspSupport,
    lnspSupportHours = planAfterReview.lnspSupportHours,
    otherDetails = planAfterReview.detail,
    // Review fields - taken from the review record itself.
    prisonerDeclinedFeedback = review.prisonerDeclinedFeedback,
    prisonerFeedback = review.prisonerFeedback,
    reviewerFeedback = review.reviewerFeedback,
    reviewCreatedBy = review.reviewCreatedByName?.let { ReviewContributor(it, review.reviewCreatedByJobRole!!) },
    otherContributors = review.otherContributors.map { ReviewContributor(it.name, it.jobRole) },
    // Audit fields - taken from the review record.
    createdBy = review.createdBy!!,
    createdByDisplayName = userService.getUserDetails(review.createdBy!!).name,
    createdAt = instantMapper.toOffsetDateTime(review.createdAt)!!,
    createdAtPrison = review.createdAtPrison,
    updatedBy = review.updatedBy!!,
    updatedByDisplayName = userService.getUserDetails(review.updatedBy!!).name,
    updatedAt = instantMapper.toOffsetDateTime(review.updatedAt)!!,
    updatedAtPrison = review.updatedAtPrison,
  )
}

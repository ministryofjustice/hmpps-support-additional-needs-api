package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SarEducationSupportPlanReviewResponseMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarEducationSupportPlanReviewResponse

/**
 * Builds the "Education Support Reviews" section of the SAR report.
 *
 * There is some complexity here because the underlying data was never designed for this kind of reporting.
 *
 * For each review we want to show two things together:
 *  1. the review record itself (the feedback, who did it, other contributors), AND
 *  2. the version of the plan as it was AFTER that review was completed.
 *
 * The problem is that a review record and a version of the plan share NO id or reference. They are two independent
 * records in two independent tables (`elsp_review` and the plan history), and nothing links a given review to a given
 * version of the plan. The UI never needed to relate them, so the schema does not support it.
 *
 * We associate them by ORDINAL POSITION instead:
 *  - When the plan is first created there is 1 plan-history record (the original plan) and 0 reviews.
 *  - When the 1st review is done, the plan is updated, so there are now 2 plan-history records and 1 review.
 *  - When the 2nd review is done there are 3 plan-history records and 2 reviews. And so on.
 *
 * So, ordering both collections oldest-first:
 *  - plan-history index 0 is the ORIGINAL plan (shown separately in the "Plan" section, not here), and
 *  - the review at index `i` is associated with the plan-history version at index `i + 1`.
 *
 * (This relationship was validated against production data: the number of plan-history records for a person is always
 * exactly the number of reviews + 1.)
 *
 * We deliberately do NOT try to match on timestamps (e.g. "a review and a plan version created within N seconds of each
 * other are related") - the ordinal approach is more robust and easier to reason about.
 */
@Service
class SarEducationSupportPlanReviewService(
  private val elspReviewRepository: ElspReviewRepository,
  private val elspPlanHistoryRepository: ElspPlanHistoryRepository,
  private val sarEducationSupportPlanReviewResponseMapper: SarEducationSupportPlanReviewResponseMapper,
) {

  fun getReviewsWithTheirAssociatedPlanVersion(prisonNumber: String): List<SarEducationSupportPlanReviewResponse> {
    val reviewsOldestFirst = elspReviewRepository.findAllByPrisonNumber(prisonNumber)
      .sortedBy { it.createdAt }
    val planHistoryVersionsOldestFirst = elspPlanHistoryRepository.findAllByPrisonNumber(prisonNumber)
      .sortedBy { it.id.revisionNumber }

    return reviewsOldestFirst.mapIndexedNotNull { reviewIndex, review ->
      // The plan version associated with this review sits one place further along than the review, because plan-history
      // index 0 is the original plan (which precedes any review). IE. review index 0 -> plan-history index 1.
      val associatedPlanVersion = planHistoryVersionsOldestFirst.getOrNull(reviewIndex + 1)
        ?: return@mapIndexedNotNull null
      sarEducationSupportPlanReviewResponseMapper.toModel(review, associatedPlanVersion)
    }
  }
}

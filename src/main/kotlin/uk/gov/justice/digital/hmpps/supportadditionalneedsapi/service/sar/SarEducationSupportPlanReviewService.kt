package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SarEducationSupportPlanReviewResponseMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarEducationSupportPlanReviewResponse
import java.time.Instant

/**
 * Builds the "Education Support Reviews" section of the SAR report.
 *
 * There is some complexity here because the underlying data was never designed for this kind of reporting.
 *
 * For each review we want to show two things together:
 *  1. the review record itself (the feedback, who did it, other contributors), AND
 *  2. the version of the plan as it was after that review was completed.
 *
 * The problem is that a review record and a version of the plan share NO id or reference. They are two independent
 * records in two independent tables (`elsp_review` and the plan history), and nothing links a given review to a given
 * version of the plan. The UI never needed to relate them, so the schema does not support it.
 *
 * We therefore associate them by TIMESTAMP, using the sequence in which the records were written.
 *
 * ## Why not associate them by ordinal position?
 *
 * A previous implementation paired review index `i` with plan-history index `i + 1`, on the assumption that completing
 * a review always writes a new version of the plan - ie. that the number of plan-history versions is always the number
 * of reviews + 1.
 *
 * That assumption is wrong. A review can be submitted with `anyChanges = false` (the reviewer changed none of the
 * plan's answers), in which case `EducationSupportPlanService.updatePlan` does not touch the plan and NO new version is
 * written to the plan history - but a review record is still created. One such review is enough to shift the ordinal
 * pairing for every review that follows it: later reviews were rendered against a plan version that was one too new,
 * and the most recent review dropped off the end of the plan history and was silently omitted from the report
 * altogether.
 *
 * ## How the timestamp association works
 *
 * When a review is completed, [uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service
 * .EducationSupportPlanReviewService.processReview] creates the review record FIRST and then updates the plan, both in
 * the same transaction. So any plan version produced by a review is written after that review's `createdAt`, and before
 * the next review is started.
 *
 * For each review we therefore take the LATEST version of the plan that was written before the NEXT review began - ie.
 * the state the plan had settled on by the end of this review, and which it kept until the next review touched it. For
 * the most recent review there is no "next review", so we simply take the latest version of the plan.
 *
 * This handles a review that changed nothing without any special casing: no new version was written by that review, so
 * the latest version before the next review is the one left behind by an earlier review (or the original plan) - which
 * is exactly right, because the plan did not change.
 *
 * Note this deliberately avoids matching a review to a plan version by "these timestamps are within N seconds of each
 * other", which needs an arbitrary tolerance and still cannot represent a review that changed nothing.
 *
 * The plan versions are ordered by `updatedAt` rather than the Envers revision number, so that this business logic does
 * not depend on an Envers-internal field. Each version is written when the plan is created (the original) or updated
 * (as part of a review), so `updatedAt` reflects that chronological sequence.
 *
 * This assumes reviews are the only thing that updates the plan, which is true today: `processReview` is the sole
 * caller of `updatePlan`. If the plan ever becomes editable outside of a review, a version written between two reviews
 * would be attributed to the earlier of them, and this logic would need revisiting.
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
    val planVersionsOldestFirst = elspPlanHistoryRepository.findAllByPrisonNumber(prisonNumber)
      .sortedBy { it.updatedAt }

    return reviewsOldestFirst.mapIndexedNotNull { reviewIndex, review ->
      // The point at which this review's changes stopped being "the current plan" - ie. when the next review began.
      // Null for the most recent review, because nothing has superseded it yet.
      val nextReviewStartedAt = reviewsOldestFirst.getOrNull(reviewIndex + 1)?.createdAt

      val planVersionAfterThisReview = planVersionInForceAtTheEndOf(planVersionsOldestFirst, nextReviewStartedAt)
        ?: return@mapIndexedNotNull null

      sarEducationSupportPlanReviewResponseMapper.toModel(review, planVersionAfterThisReview)
    }
  }

  /**
   * Returns the version of the plan that was in force at the end of a review - the latest version written before
   * [nextReviewStartedAt], or the latest version of all if this was the most recent review (ie. [nextReviewStartedAt]
   * is null).
   *
   * Returns null only if the person has no plan history at all, in which case there is no plan data to render
   * alongside the review.
   */
  private fun planVersionInForceAtTheEndOf(
    planVersionsOldestFirst: List<ElspPlanHistoryEntity>,
    nextReviewStartedAt: Instant?,
  ): ElspPlanHistoryEntity? = if (nextReviewStartedAt == null) {
    planVersionsOldestFirst.lastOrNull()
  } else {
    planVersionsOldestFirst.lastOrNull { it.updatedAt < nextReviewStartedAt }
  }
}

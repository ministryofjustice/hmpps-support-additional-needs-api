package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.OtherReviewContributorEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.CannotCompleteReviewWithNoSchedule
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspReviewMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanReviewsResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class EducationSupportPlanReviewService(
  private val educationSupportPlanService: EducationSupportPlanService,
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleService: ReviewScheduleService,
  private val elspReviewRepository: ElspReviewRepository,
  private val elspReviewMapper: ElspReviewMapper,
) {

  @Transactional
  @TimelineEvent(
    eventType = TimelineEventType.ELSP_REVIEW_CREATED,
    additionalInfoPrefix = "Review completed on:",
    additionalInfoField = "reviewDate",
  )
  fun processReview(prisonNumber: String, request: SupportPlanReviewRequest) {
    log.info { "Creating review for $prisonNumber" }
    // validate that the plan exists.
    if (!educationSupportPlanService.hasPlan(prisonNumber)) {
      throw PlanNotFoundException(prisonNumber)
    }
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    // validate that a review schedule exists.
    if (reviewScheduleEntity == null || reviewScheduleEntity.status != ReviewScheduleStatus.SCHEDULED) {
      throw CannotCompleteReviewWithNoSchedule(prisonNumber)
    }
    createReview(prisonNumber, request, reviewScheduleEntity.reference)
    // edit the plan and create the review in the same transaction - envers will do the rest of the magic.
    educationSupportPlanService.updatePlan(prisonNumber, request.updateEducationSupportPlan)
    // complete the existing review schedule and create a new one with the new review data
    // as part of the review creation need to also publish messages
    reviewScheduleService.completeExistingAndCreateNextReviewSchedule(
      prisonNumber,
      request.nextReviewDate,
      request.prisonId,
      reviewScheduleEntity,
    )
  }

  @Transactional
  fun createReview(prisonNumber: String, request: SupportPlanReviewRequest, scheduleReference: UUID) {
    // create a new review record for the prisoner with the request details.
    with(request) {
      val review = ElspReviewEntity(
        prisonNumber = prisonNumber,
        reviewCreatedByName = reviewCreatedBy?.name,
        reviewCreatedByJobRole = reviewCreatedBy?.jobRole,
        otherContributors = mutableListOf(),
        prisonerDeclinedFeedback = prisonerDeclinedFeedback,
        prisonerFeedback = prisonerFeedback,
        reviewerFeedback = reviewerFeedback,
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
        reviewScheduleReference = scheduleReference,
      )

      otherContributors?.forEach { contributor ->
        val contributorEntity = OtherReviewContributorEntity(
          name = contributor.name,
          jobRole = contributor.jobRole,
          createdAtPrison = prisonId,
          updatedAtPrison = prisonId,
          elspReview = review,
        )
        review.otherContributors.add(contributorEntity)
      }
      elspReviewRepository.save(review)
    }
  }

  fun getReviews(prisonNumber: String): PlanReviewsResponse {
    if (!educationSupportPlanService.hasPlan(prisonNumber)) {
      throw PlanNotFoundException(prisonNumber)
    }
    val reviews = elspReviewRepository.findAllByPrisonNumber(prisonNumber)
    return PlanReviewsResponse(reviews.map { elspReviewMapper.toModel(it) })
  }
}

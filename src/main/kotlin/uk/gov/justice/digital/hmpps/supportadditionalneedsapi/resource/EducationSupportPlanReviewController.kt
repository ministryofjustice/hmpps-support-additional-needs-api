package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.transaction.annotation.Transactional
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanReviewsResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanReviewService

@Validated
@RestController
@RequestMapping("/profile/{prisonNumber}/education-support-plan/review")
class EducationSupportPlanReviewController(private val educationSupportPlanReviewService: EducationSupportPlanReviewService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Transactional
  fun createReview(
    @PathVariable prisonNumber: String,
    @RequestBody request: SupportPlanReviewRequest,
  ) {
    educationSupportPlanReviewService.processReview(prisonNumber, request)
  }

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  @ResponseStatus(HttpStatus.OK)
  fun getReviews(
    @PathVariable prisonNumber: String,
  ): PlanReviewsResponse = educationSupportPlanReviewService.getReviews(prisonNumber)
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest

private val log = KotlinLogging.logger {}

@Service
class EducationSupportPlanReviewService {
  fun createReview(prisonNumber: String, request: SupportPlanReviewRequest) {
    log.info { "Creating review for $prisonNumber" }
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleStatus.SCHEDULED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ReviewScheduleService

@RestController
@RequestMapping("/profile/{prisonNumber}/reviews/review-schedules")
class ReviewScheduleController(private val reviewScheduleService: ReviewScheduleService) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getReviewSchedules(
    @PathVariable prisonNumber: String,
  ): ReviewSchedulesResponse = reviewScheduleService.getSchedules(prisonNumber)

  @Hidden
  @PreAuthorize(HAS_EDIT_ELSP)
  @PutMapping("/exempt")
  fun exemptSchedule(
    @PathVariable prisonNumber: String,
  ) {
    val schedule =
      reviewScheduleService.getSchedules(prisonNumber).reviewSchedules.firstOrNull { it.status == SCHEDULED }
    if (schedule == null) throw IllegalStateException("SCHEDULED Review schedule not found for $prisonNumber")
    reviewScheduleService.exemptSchedule(
      prisonNumber = prisonNumber,
      status = ReviewScheduleStatus.EXEMPT_UNKNOWN,
      prisonId = schedule.updatedAtPrison,
    )
  }
}

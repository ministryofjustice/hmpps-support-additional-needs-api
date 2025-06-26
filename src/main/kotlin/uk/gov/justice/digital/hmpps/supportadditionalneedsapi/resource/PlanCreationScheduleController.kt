package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdatePlanCreationStatusRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService

@RestController
@RequestMapping("/profile/{prisonNumber}/plan-creation-schedule")
class PlanCreationScheduleController(private val planCreationScheduleService: PlanCreationScheduleService) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getPlanCreationSchedules(
    @PathVariable prisonNumber: String,
  ): PlanCreationSchedulesResponse = planCreationScheduleService.getSchedules(prisonNumber)

  @PreAuthorize(HAS_EDIT_ELSP)
  @PatchMapping("/status")
  fun updatePlanCreationScheduleStatus(
    @PathVariable prisonNumber: String,
    @RequestBody request: UpdatePlanCreationStatusRequest,
  ): PlanCreationSchedulesResponse = TODO()
}

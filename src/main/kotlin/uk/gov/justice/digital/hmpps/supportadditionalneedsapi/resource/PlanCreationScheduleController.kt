package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ReviewScheduleService

@RestController
@RequestMapping("/profile/{prisonNumber}/plan-creation-schedule")
class PlanCreationScheduleController(private val planCreationScheduleService: PlanCreationScheduleService) {
  // TODO add this in when roles are defined @PreAuthorize(ROLE_SUPPORT_ADDITIONAL_NEEDS__API__RO)
  @GetMapping
  fun getPlanCreationSchedules(
    @PathVariable prisonNumber: String,
  ): PlanCreationSchedulesResponse = planCreationScheduleService.getSchedules(prisonNumber)
}

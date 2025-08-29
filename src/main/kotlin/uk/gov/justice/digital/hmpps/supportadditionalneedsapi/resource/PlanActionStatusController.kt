package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanActionStatusService

@Validated
@RestController
@RequestMapping("/profile/{prisonNumber}/plan-action-status")
class PlanActionStatusController(private val planActionStatusService: PlanActionStatusService) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getPlanActionStatus(@PathVariable prisonNumber: String): PlanActionStatus = planActionStatusService.getPlanActionStatus(prisonNumber)
}

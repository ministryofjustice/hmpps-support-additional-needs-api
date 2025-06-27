package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationUpdateStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdatePlanCreationStatusRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator.ReasonSpecifiedForExemptionStatusRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus as PlanCreationScheduleStatusEntity

@Validated
@RestController
@RequestMapping("/profile/{prisonNumber}/plan-creation-schedule")
class PlanCreationScheduleController(private val planCreationScheduleService: PlanCreationScheduleService) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getPlanCreationSchedules(
    @PathVariable prisonNumber: String,
    @RequestParam(name = "includePastSchedules", defaultValue = "false") includePastSchedules: Boolean,
  ): PlanCreationSchedulesResponse = planCreationScheduleService.getSchedules(prisonNumber, includePastSchedules)

  @PreAuthorize(HAS_EDIT_ELSP)
  @PatchMapping("/status")
  @ReasonSpecifiedForExemptionStatusRequest
  fun updatePlanCreationScheduleStatus(
    @PathVariable prisonNumber: String,
    @RequestBody request: UpdatePlanCreationStatusRequest,
  ): PlanCreationSchedulesResponse {
    planCreationScheduleService.exemptSchedule(
      prisonNumber = prisonNumber,
      status = mapStatus(request.status),
      exemptionReason = request.exemptionReason?.value,
      exemptionDetail = request.exemptionDetail,
      updatedAtPrison = request.prisonId,
      clearDeadlineDate = true,
    )
    return planCreationScheduleService.getSchedules(prisonNumber, false)
  }

  fun mapStatus(status: PlanCreationUpdateStatus): PlanCreationScheduleStatusEntity = when (status) {
    PlanCreationUpdateStatus.EXEMPT_PRISONER_NOT_COMPLY -> PlanCreationScheduleStatusEntity.EXEMPT_PRISONER_NOT_COMPLY
  }
}

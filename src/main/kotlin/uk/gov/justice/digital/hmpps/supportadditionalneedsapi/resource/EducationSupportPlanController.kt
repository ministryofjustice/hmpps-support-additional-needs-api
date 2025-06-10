package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService

@RestController
@RequestMapping("/profile/{prisonNumber}/education-support-plan")
class EducationSupportPlanController(private val educationSupportPlanService: EducationSupportPlanService) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getEducationSupportPlan(@PathVariable prisonNumber: String): EducationSupportPlanResponse = educationSupportPlanService.getPlan(prisonNumber)

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createEducationSupportPlan(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody request: CreateEducationSupportPlanRequest,
  ): EducationSupportPlanResponse = educationSupportPlanService.create(prisonNumber, request)
}

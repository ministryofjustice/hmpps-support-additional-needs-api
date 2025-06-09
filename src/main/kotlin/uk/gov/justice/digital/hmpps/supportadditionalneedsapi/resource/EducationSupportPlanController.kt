package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse

@RestController
@RequestMapping("/profile/{prisonNumber}/education-support-plan")
class EducationSupportPlanController {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getEducationSupportPlan(@PathVariable prisonNumber: String) {
    TODO("Implement me")
  }

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  fun createEducationSupportPlan(
    @PathVariable prisonNumber: String,
    @Valid @RequestBody request: CreateEducationSupportPlanRequest,
  ): EducationSupportPlanResponse {
    TODO("Implement me")
  }
}

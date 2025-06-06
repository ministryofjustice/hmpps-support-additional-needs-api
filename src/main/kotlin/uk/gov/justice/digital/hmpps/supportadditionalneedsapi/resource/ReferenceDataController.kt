package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import io.swagger.v3.oas.annotations.Parameter
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReferenceDataListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ReferenceDataService

@RestController
@RequestMapping(path = ["/reference-data/{domain}"])
class ReferenceDataController(
  private val referenceDataService: ReferenceDataService,
) {
  @GetMapping
  @PreAuthorize(HAS_VIEW_ELSP)
  fun getReferenceData(
    @PathVariable @Parameter(description = "Reference data domain.", required = true) domain: Domain,
    @Parameter(description = "Include inactive reference data. Defaults to false") includeInactive: Boolean = false,
  ): ReferenceDataListResponse = ReferenceDataListResponse(referenceDataService.getReferenceDataForDomain(domain, includeInactive))
}

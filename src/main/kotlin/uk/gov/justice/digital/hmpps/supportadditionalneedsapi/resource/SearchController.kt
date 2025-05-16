package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortDirection
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortField

@RestController
@RequestMapping("/search/prisons/{prisonId}/people")
class SearchController {
  @GetMapping
  @PreAuthorize(HAS_SEARCH_PRISONS)
  fun searchByPrison(
    @PathVariable prisonId: String,
    @RequestParam(required = false) prisonerNameOrNumber: String?,
    @RequestParam(required = false, defaultValue = "PRISONER_NAME") sortBy: SearchSortField,
    @RequestParam(required = false, defaultValue = "ASC") sortDirection: SearchSortDirection,
    @RequestParam(required = false, defaultValue = "1") @Min(1) page: Int,
    @RequestParam(required = false, defaultValue = "50") @Min(1) pageSize: Int,
  ): SearchByPrisonResponse {
    TODO("implement me")
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.constraints.Min
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PaginationMetaData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortDirection
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortField
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SearchCriteria
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SearchService
import kotlin.math.max
import kotlin.math.min

@RestController
@RequestMapping("/search/prisons/{prisonId}/people")
@Validated
class SearchController(private val searchService: SearchService) {
  @GetMapping
  @PreAuthorize(HAS_SEARCH_PRISONS)
  fun searchByPrison(
    @PathVariable prisonId: String,
    @RequestParam(required = false) prisonerNameOrNumber: String? = null,
    @RequestParam(required = false) planStatus: PlanStatus? = null,
    @RequestParam(required = false) sortBy: SearchSortField = SearchSortField.PRISONER_NAME,
    @RequestParam(required = false) sortDirection: SearchSortDirection = SearchSortDirection.ASC,
    @RequestParam(required = false) @Min(1) page: Int = 1,
    @RequestParam(required = false) @Min(1) pageSize: Int = 50,
  ): SearchByPrisonResponse {
    val filteredAndSortedPrisoners = searchService.searchPrisoners(
      SearchCriteria(
        prisonId = prisonId,
        prisonerNameOrNumber = prisonerNameOrNumber,
        planStatus = planStatus,
        sortBy = sortBy,
        sortDirection = sortDirection,
      ),
    )

    val totalElements = filteredAndSortedPrisoners.size
    val totalPages = (totalElements + pageSize - 1) / pageSize
    val elementsToDropBeforeRequestedPage = if (totalPages > 1) (min(page, totalPages) - 1) * pageSize else 0
    val requestedPageOfPrisoners = filteredAndSortedPrisoners.drop(elementsToDropBeforeRequestedPage).take(pageSize)

    return SearchByPrisonResponse(
      people = requestedPageOfPrisoners,
      pagination = PaginationMetaData(
        totalElements = totalElements,
        totalPages = max(totalPages, 1),
        page = max(min(totalPages, page), 1),
        pageSize = pageSize,
        first = max(min(totalPages, page), 1) == 1,
        last = totalPages == 1 || totalPages <= page || requestedPageOfPrisoners.isEmpty(),
      ),
    )
  }
}

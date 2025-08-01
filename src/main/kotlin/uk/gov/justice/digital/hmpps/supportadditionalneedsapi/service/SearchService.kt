package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SentenceTypeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Person
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortDirection
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortField

@Service
class SearchService(private val prisonerSearchApiService: PrisonerSearchApiService) {
  fun searchPrisoners(searchCriteria: SearchCriteria): List<Person> {
    val filteredAndSortedPrisoners = prisonerSearchApiService.getAllPrisonersInPrison(searchCriteria.prisonId)
      .filterByCriteria(searchCriteria)
      .sortBy(searchCriteria)

    // TODO - get Additional Needs data for each prisoner from this service DB

    return filteredAndSortedPrisoners.map {
      Person(
        forename = it.firstName,
        surname = it.lastName,
        prisonNumber = it.prisonerNumber,
        dateOfBirth = it.dateOfBirth,
        releaseDate = it.releaseDate,
        cellLocation = it.cellLocation,
        sentenceType = SentenceTypeMapper.fromPrisonerSearchApiToModel(it.legalStatus),
        inEducation = false,
        hasAdditionalNeed = false,
        planStatus = PlanStatus.NO_PLAN
      )
    }
  }

  private fun List<Prisoner>.filterByCriteria(searchCriteria: SearchCriteria): List<Prisoner> = this.filter { prisoner ->
    // Filter by prisoner name or number
    (
      searchCriteria.prisonerNameOrNumber.isNullOrBlank() ||
        prisoner.firstName.contains(searchCriteria.prisonerNameOrNumber, ignoreCase = true) ||
        prisoner.lastName.contains(searchCriteria.prisonerNameOrNumber, ignoreCase = true) ||
        prisoner.prisonerNumber.equals(searchCriteria.prisonerNameOrNumber, ignoreCase = true)
      )
  }

  private fun List<Prisoner>.sortBy(searchCriteria: SearchCriteria): List<Prisoner> {
    val comparator: Comparator<Prisoner> = when (searchCriteria.sortBy) {
      SearchSortField.PRISONER_NAME -> compareBy(nullsLast()) { it.lastName }
      SearchSortField.PRISON_NUMBER -> compareBy(nullsLast()) { it.prisonerNumber }
      SearchSortField.RELEASE_DATE -> compareBy(nullsLast()) { it.releaseDate }
      SearchSortField.CELL_LOCATION -> compareBy(nullsLast()) { it.cellLocation }
    }

    return when (searchCriteria.sortDirection) {
      SearchSortDirection.ASC -> this.sortedWith(comparator)
      SearchSortDirection.DESC -> this.sortedWith(comparator.reversed())
    }
  }
}

data class SearchCriteria(
  val prisonId: String,
  val prisonerNameOrNumber: String? = null,
  val sortBy: SearchSortField,
  val sortDirection: SearchSortDirection,
)

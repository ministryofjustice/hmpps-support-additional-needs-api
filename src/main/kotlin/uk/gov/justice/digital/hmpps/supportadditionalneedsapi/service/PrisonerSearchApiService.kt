package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient

private val log = KotlinLogging.logger {}

@Service
class PrisonerSearchApiService(private val prisonerSearchApiClient: PrisonerSearchApiClient) {

  fun getAllPrisonersInPrison(
    prisonId: String,
    // prisoner-search-api has a buggy paged API implementation, so safest to request a large page size to get all records in one hit (arguably not the most efficient though)
    pageSize: Int = 9999,
  ): List<Prisoner> {
    var page = 0

    val prisoners = mutableListOf<Prisoner>()

    do {
      val apiResponse = prisonerSearchApiClient.getPrisonersByPrisonId(
        prisonId = prisonId,
        page = page++,
        pageSize = pageSize,
      )
      prisoners.addAll(apiResponse.content)
    } while (apiResponse.last != true)

    return prisoners.toList()
      .also {
        log.info { "Returned ${it.size} prisoners for prison $prisonId from $page calls to Prisoner Search API" }
      }
  }

  fun getPrisoner(prisonNumber: String): Prisoner = prisonerSearchApiClient.getPrisoner(prisonNumber)
    .also {
      log.info { "Retrieved prisoner [$prisonNumber] from Prisoner Search API" }
    }
}

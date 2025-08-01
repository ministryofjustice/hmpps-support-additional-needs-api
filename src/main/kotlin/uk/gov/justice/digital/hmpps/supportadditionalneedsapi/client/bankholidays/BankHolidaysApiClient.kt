package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.CacheConfiguration.Companion.BANK_HOLIDAYS

/**
 * API client class to consume Government Bank Holiday API endpoints.
 *
 * See https://www.api.gov.uk/gds/bank-holidays/#bank-holidays
 */
@Component
class BankHolidaysApiClient(
  @Qualifier("bankHolidaysApiWebClient")
  private val bankHolidaysApiWebClient: WebClient,
) {

  @Cacheable(BANK_HOLIDAYS)
  fun getBankHolidays(): BankHolidays = try {
    bankHolidaysApiWebClient.get()
      .uri("/bank-holidays.json")
      .retrieve()
      .bodyToMono(BankHolidays::class.java)
      .block()!!
  } catch (e: Exception) {
    throw Exception("Error retrieving bank holiday info ${e.cause}", e)
  }
}

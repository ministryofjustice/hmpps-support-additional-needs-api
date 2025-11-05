package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource.reporting

import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import java.time.LocalDate

class PrisonActivitySummaryIntegrationTest : IntegrationTestBase() {

  @Test
  fun `should return 200 OK with CSV data for authorized user with RO role`() {
    val fromDate = LocalDate.now().minusDays(14)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"prison-activity-summary.csv\"")
  }

  @Test
  fun `should return 200 OK with CSV data for authorized user with RW role`() {
    val fromDate = LocalDate.now().minusDays(14)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"prison-activity-summary.csv\"")
  }

  @Test
  fun `should return 200 OK with default date range when dates not provided`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
  }

  @Test
  fun `should return 403 Forbidden for user without correct role`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should return 400 Bad Request when date range exceeds maximum`() {
    val fromDate = LocalDate.now().minusDays(90)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `should return 401 Unauthorized when no bearer token`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .exchange()
      .expectStatus().isUnauthorized
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat

class SearchByPrisonTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/search/prisons/{prisonId}/people"
    private const val PRISON_ID = "BXI"
  }

  @Test
  fun `should return unauthorized given no bearer token`() {
    webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden given bearer token without required role`() {
    // Given

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus()
      .isForbidden
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.FORBIDDEN.value())
      .hasUserMessage("Access Denied")
      .hasDeveloperMessage("Access denied on uri=/search/prisons/BXI/people")
  }
}

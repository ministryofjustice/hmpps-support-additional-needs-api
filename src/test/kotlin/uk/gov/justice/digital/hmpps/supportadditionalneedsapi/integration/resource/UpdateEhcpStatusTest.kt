package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EhcpStatusResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEhcpRequest

class UpdateEhcpStatusTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/ehcp-status"
  }

  @Test
  fun `Update EHCP status for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)

    val request = UpdateEhcpRequest(
      hasCurrentEhcp = false,
      prisonId = "BXI",
    )

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(EhcpStatusResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.hasCurrentEhcp).isFalse()

    val persisted = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    assertThat(persisted).isNotNull()
    assertThat(persisted.hasCurrentEhcp).isFalse()
  }

  @Test
  fun `Return error when ELSP does not exist for prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val request = UpdateEhcpRequest(
      hasCurrentEhcp = false,
      prisonId = "BXI",
    )
    // No EHCP status is created or saved for this prison number

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.userMessage).isEqualTo("ELSP plan not found for prisoner [$prisonNumber]")
  }
}

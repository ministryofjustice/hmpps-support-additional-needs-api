package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReferenceDataListResponse

class GetReferenceDataCategoryTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/reference-data/{domain}/categories"
  }

  @Test
  fun `should return a list of CHALLENGE category reference data`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, Domain.CHALLENGE)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReferenceDataListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.referenceDataList.size).isEqualTo(9)

    // Ensure no duplicate codes
    val uniqueCodes = actual.referenceDataList.map { it.code }.toSet()
    assertThat(uniqueCodes.size).isEqualTo(actual.referenceDataList.size)

    // Ensure all entries have non-null descriptions
    assertThat(actual.referenceDataList).allMatch { it.description != null }
  }
}

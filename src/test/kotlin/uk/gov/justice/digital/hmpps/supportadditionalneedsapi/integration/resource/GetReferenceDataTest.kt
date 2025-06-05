package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReferenceDataListResponse

class GetReferenceDataTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/reference-data/{domain}"
  }

  @Test
  fun `should return a list of CONDITON reference data`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, Domain.CONDITION)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReferenceDataListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.referenceDataList.size).isEqualTo(18)

    // Example check for specific entry (e.g. code = "ADHD")
    val adhd = actual.referenceDataList.find { it.code == "ADHD" }
    assertThat(adhd).isNotNull()
    assertThat(adhd!!.description).isEqualTo("Attention Deficit Hyperactivity Disorder")
    assertThat(adhd.listSequence).isEqualTo(3)
    assertThat(adhd.active).isNotEqualTo(false)

    val firstItem = actual.referenceDataList.minByOrNull { it.listSequence ?: Int.MAX_VALUE }
    assertThat(firstItem).isNotNull()
    assertThat(firstItem!!.code).isEqualTo("ABI")

    val lastItem = actual.referenceDataList.maxByOrNull { it.listSequence ?: Int.MIN_VALUE }
    assertThat(lastItem).isNotNull()
    assertThat(lastItem!!.code).isEqualTo("OTHER")

    // Ensure no duplicate codes
    val uniqueCodes = actual.referenceDataList.map { it.code }.toSet()
    assertThat(uniqueCodes.size).isEqualTo(18)

    // Ensure all entries have non-null descriptions
    assertThat(actual.referenceDataList).allMatch { it.description != null }
  }
}

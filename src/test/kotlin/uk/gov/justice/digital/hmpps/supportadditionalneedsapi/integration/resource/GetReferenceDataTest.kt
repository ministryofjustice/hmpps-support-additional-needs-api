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
    assertThat(actual!!.referenceDataList.size).isEqualTo(22)

    // Example check for specific entry (e.g. code = "ADHD")
    val adhd = actual.referenceDataList.find { it.code == "ADHD" }
    assertThat(adhd).isNotNull()
    assertThat(adhd!!.description).isEqualTo("Attention Deficit Hyperactivity Disorder (ADHD / ADD)")
    assertThat(adhd.active).isNotEqualTo(false)
    assertThat(adhd.categoryCode).isEqualTo("LEARNING_DIFFICULTY")
    assertThat(adhd.categoryDescription).isEqualTo("Learning Difficulty")

    // Ensure no duplicate codes
    val uniqueCodes = actual.referenceDataList.map { it.code }.toSet()
    assertThat(uniqueCodes.size).isEqualTo(actual.referenceDataList.size)

    // Ensure all entries have non-null descriptions
    assertThat(actual.referenceDataList).allMatch { it.description != null }
  }

  @Test
  fun `should return a list of STRENGTH reference data`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, Domain.STRENGTH)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReferenceDataListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.referenceDataList.size).isEqualTo(63)

    // Example check for specific entry (e.g. code = "ADHD")
    val adhd = actual.referenceDataList.find { it.code == "LISTENING" }
    assertThat(adhd).isNotNull()
    assertThat(adhd!!.description).isEqualTo("Listening")
    assertThat(adhd.active).isNotEqualTo(false)
    assertThat(adhd.categoryCode).isEqualTo("SENSORY")
    assertThat(adhd.categoryDescription).isEqualTo("Sensory")

    // Ensure no duplicate codes
    val uniqueCodes = actual.referenceDataList.map { it.code }.toSet()
    assertThat(uniqueCodes.size).isEqualTo(actual.referenceDataList.size)

    // Ensure all entries have non-null descriptions
    assertThat(actual.referenceDataList).allMatch { it.description != null }
  }
}

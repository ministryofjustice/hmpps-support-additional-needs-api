package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse

class GetConditionsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/conditions"
  }

  @Test
  fun `Get list of conditions for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")
    val dyslexia = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "DYSLEXIA"))
      ?: throw IllegalStateException("Reference data not found")
    val mentalHealth = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "MENTAL_HEALTH"))
      ?: throw IllegalStateException("Reference data not found")

    conditionRepository.saveAll(
      listOf(
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = adhd,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = dyslexia,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.CONFIRMED_DIAGNOSIS,
          conditionType = mentalHealth,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ConditionListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.conditions).hasSize(3)

    val conditionCodes = actual.conditions.map { it.conditionType.code }
    assertThat(conditionCodes).containsExactlyInAnyOrder("ADHD", "DYSLEXIA", "MENTAL_HEALTH")
  }

  @Test
  fun `Return empty list when no conditions exist for prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // No conditions are created or saved for this prison number

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ConditionListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.conditions).isEmpty()
  }
}

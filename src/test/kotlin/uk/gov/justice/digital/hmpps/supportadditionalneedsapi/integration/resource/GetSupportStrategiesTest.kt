package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyListResponse

class GetSupportStrategiesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies"
  }

  @Test
  fun `Get list of support strategies for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found for PROCESSING_SPEED")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "SENSORY"))
      ?: throw IllegalStateException("Reference data not found for SENSORY")
    val general = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "GENERAL"))
      ?: throw IllegalStateException("Reference data not found for GENERAL")

    supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          detail = "Needs quiet space to focus",
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = general,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
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
      .returnResult(SupportStrategyListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.supportStrategies).hasSize(3)

    val strategyCodes = actual.supportStrategies.map { it.supportStrategyType.code }
    assertThat(strategyCodes).containsExactlyInAnyOrder("PROCESSING_SPEED", "GENERAL", "SENSORY")
  }

  @Test
  fun `Get list of support strategies for a given prisoner where one is archived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found for PROCESSING_SPEED")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "SENSORY"))
      ?: throw IllegalStateException("Reference data not found for SENSORY")
    val general = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "GENERAL"))
      ?: throw IllegalStateException("Reference data not found for GENERAL")

    supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          detail = "Needs quiet space to focus",
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = general,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = false,
          archiveReason = "Archive reason",
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
      .returnResult(SupportStrategyListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.supportStrategies).hasSize(3)

    val strategyCodes = actual.supportStrategies.map { it.supportStrategyType.code }
    assertThat(strategyCodes).containsExactlyInAnyOrder("PROCESSING_SPEED", "GENERAL", "SENSORY")
    val archivedStrategy = actual.supportStrategies.filter { it.supportStrategyType.code == "GENERAL" }
    assertThat(archivedStrategy.first().active).isFalse()
    assertThat(archivedStrategy.first().archiveReason).isEqualTo("Archive reason")
  }

  @Test
  fun `Return empty list when no support strategies exist for prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // No support strategies created for this prisoner

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SupportStrategyListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.supportStrategies).isEmpty()
  }
}

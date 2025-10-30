package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateSupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class UpdateSupportStrategiesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies/{reference}"
  }

  @Test
  fun `update a support strategy for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    val strategies = supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          detail = "Needs quiet space to focus",
          active = true,
        ),
      ),
    )

    val strategy = strategies.first()
    val updateSupportStrategyRequest = UpdateSupportStrategyRequest(detail = "updated detail", prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateSupportStrategyRequest)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SupportStrategyResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val updatedStrategy =
      supportStrategyRepository.findAllByPrisonNumber(prisonNumber).find { it.supportStrategyType.code == "PROCESSING_SPEED" }
        ?: throw IllegalStateException("support strategy not found")

    assertThat(updatedStrategy.detail).isEqualTo("updated detail")
    assertThat(updatedStrategy.updatedAtPrison).isEqualTo("FKL")
  }

  @Test
  fun `update a support strategy for a given prisoner no detail`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    val strategies = supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          detail = "Needs quiet space to focus",
          active = true,
        ),
      ),
    )

    val strategy = strategies.first()
    val updateSupportStrategyRequest = UpdateSupportStrategyRequest(detail = "  ", prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateSupportStrategyRequest)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.BAD_REQUEST.value())
  }

  @Test
  fun `attempt to update a support strategy that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val updateRequest = UpdateSupportStrategyRequest(detail = "updated detail", prisonId = "FKL")
    val ref = UUID.randomUUID().toString()

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Support Strategy with reference [$ref] not found for prisoner [$prisonNumber]")
  }
}

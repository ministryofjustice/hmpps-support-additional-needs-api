package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveSupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class ArchiveSupportStrategiesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies/{reference}/archive"
  }

  @Test
  fun `archive a support strategy for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    val strategy = supportStrategyRepository.saveAndFlush(
      SupportStrategyEntity(
        prisonNumber = prisonNumber,
        supportStrategyType = processingSpeed,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        detail = "Needs quiet space to focus",
        active = true,
      ),
    )

    val request = ArchiveSupportStrategyRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val supportStrategies = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)

    assertThat(supportStrategies).hasSize(1)
    assertThat(supportStrategies.first().active).isEqualTo(false)
    assertThat(supportStrategies.first().archiveReason).isEqualTo("archive reason")
  }

  @Test
  fun `archive a support strategy for a given prisoner that is already archived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    val strategy = supportStrategyRepository.saveAndFlush(
      SupportStrategyEntity(
        prisonNumber = prisonNumber,
        supportStrategyType = processingSpeed,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        detail = "Needs quiet space to focus",
        active = false,
        archiveReason = "archive reason",
      ),
    )

    val request = ArchiveSupportStrategyRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Support Strategy with reference [${strategy.reference}] has been archived for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to archive a support strategy that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID().toString()
    val request = ArchiveSupportStrategyRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
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

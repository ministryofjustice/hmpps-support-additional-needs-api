package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.UUID

class DeleteSupportStrategyTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies/{reference}"
    private const val DEFAULT_QUERY = "?prisonId=BXI&reason=ENTERED_IN_ERROR"
  }

  @Test
  fun `delete a support strategy for a given prisoner`() {
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
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val strategies = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(strategies).isEmpty()
  }

  @Test
  fun `delete one of several support strategies for a prisoner leaves the others`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "SENSORY"))
      ?: throw IllegalStateException("Reference data not found")

    val processingStrategy = supportStrategyRepository.saveAndFlush(
      SupportStrategyEntity(
        prisonNumber = prisonNumber,
        supportStrategyType = processingSpeed,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )
    val sensoryStrategy = supportStrategyRepository.saveAndFlush(
      SupportStrategyEntity(
        prisonNumber = prisonNumber,
        supportStrategyType = sensory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, processingStrategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val remaining = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().reference).isEqualTo(sensoryStrategy.reference)
    assertThat(remaining.first().supportStrategyType.key.code).isEqualTo("SENSORY")
  }

  @Test
  fun `delete an archived (inactive) support strategy`() {
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
        active = false,
        archiveReason = "previously archived",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val strategies = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(strategies).isEmpty()
  }

  @Test
  fun `delete a support strategy that does not exist for the prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
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

  @Test
  fun `delete a support strategy belonging to a different prisoner returns 404`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonerA = randomValidPrisonNumber()
    val prisonerB = randomValidPrisonNumber()

    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    val strategy = supportStrategyRepository.saveAndFlush(
      SupportStrategyEntity(
        prisonNumber = prisonerA,
        supportStrategyType = processingSpeed,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When — try to delete prisonerA's strategy under prisonerB's path
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonerB, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Support Strategy with reference [${strategy.reference}] not found for prisoner [$prisonerB]")

    // and the original is untouched
    assertThat(supportStrategyRepository.findAllByPrisonNumber(prisonerA)).hasSize(1)
  }

  @Test
  fun `delete a support strategy with no role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf(), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete a support strategy with read-only role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When — read-only role cannot delete
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete a support strategy without prisonId query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?reason=ENTERED_IN_ERROR", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete a support strategy without reason query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete a support strategy with an unknown reason value`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI&reason=NOT_A_REAL_REASON", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete records a SUPPORT_STRATEGY_DELETED timeline event with reference and reason in additionalInfo`() {
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
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    val deletionEntry = timelineEntries.firstOrNull { it.event == TimelineEventType.SUPPORT_STRATEGY_DELETED }
    assertThat(deletionEntry).isNotNull()
    assertThat(deletionEntry!!.additionalInfo)
      .isEqualTo("supportStrategyReference=${strategy.reference}|reason=ENTERED_IN_ERROR")
    assertThat(deletionEntry.createdAtPrison).isEqualTo("BXI")
  }
}

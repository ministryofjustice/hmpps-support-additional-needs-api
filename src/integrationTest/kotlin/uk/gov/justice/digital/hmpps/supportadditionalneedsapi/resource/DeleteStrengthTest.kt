package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate
import java.util.UUID

class DeleteStrengthTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/strengths/{reference}"
    private const val DEFAULT_QUERY = "?prisonId=BXI&reason=ENTERED_IN_ERROR"
  }

  @Test
  fun `delete a strength for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val strength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val strengths = strengthRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(strengths).isEmpty()
  }

  @Test
  fun `delete one of several strengths for a prisoner leaves the others`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")

    val memoryStrength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )
    val sensoryStrength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = sensory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, memoryStrength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val remaining = strengthRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().reference).isEqualTo(sensoryStrength.reference)
    assertThat(remaining.first().strengthType.key.code).isEqualTo("SENSORY_PROCESSING")
  }

  @Test
  fun `delete an archived (inactive) strength`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val strength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        active = false,
        archiveReason = "previously archived",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val strengths = strengthRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(strengths).isEmpty()
  }

  @Test
  fun `delete a strength that does not exist for the prisoner`() {
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
      .hasUserMessage("Strength with reference [$ref] not found for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to delete a strength that is from an ALN screener returns 409`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val alnScreener = alnScreenerRepository.saveAndFlush(
      ALNScreenerEntity(
        screeningDate = LocalDate.now(),
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        prisonNumber = prisonNumber,
      ),
    )

    val strength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        alnScreenerId = alnScreener.id,
      ),
    )

    // When
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Strength with reference [${strength.reference}] cannot be modified as it is an ALN screener strength for prisoner [$prisonNumber]")

    // and the strength is still present
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber)).hasSize(1)
  }

  @Test
  fun `delete a strength belonging to a different prisoner returns 404`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonerA = randomValidPrisonNumber()
    val prisonerB = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val strength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonerA,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When — try to delete prisonerA's strength under prisonerB's path
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonerB, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Strength with reference [${strength.reference}] not found for prisoner [$prisonerB]")

    // and the original is untouched
    assertThat(strengthRepository.findAllByPrisonNumber(prisonerA)).hasSize(1)
  }

  @Test
  fun `delete a strength with no role`() {
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
  fun `delete a strength with read-only role`() {
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
  fun `delete a strength without prisonId query param`() {
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
  fun `delete a strength without reason query param`() {
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
  fun `delete a strength with an unknown reason value`() {
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
  fun `delete records a STRENGTH_DELETED timeline event with reference and reason in additionalInfo`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val strength = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    val deletionEntry = timelineEntries.firstOrNull { it.event == TimelineEventType.STRENGTH_DELETED }
    assertThat(deletionEntry).isNotNull()
    assertThat(deletionEntry!!.additionalInfo)
      .isEqualTo("strengthReference=${strength.reference}|reason=ENTERED_IN_ERROR")
    assertThat(deletionEntry.createdAtPrison).isEqualTo("BXI")
  }
}

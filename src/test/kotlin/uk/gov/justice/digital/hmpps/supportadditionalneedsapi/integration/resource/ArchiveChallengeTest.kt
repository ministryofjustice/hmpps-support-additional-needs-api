package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate
import java.util.*

class ArchiveChallengeTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges/{reference}/archive"
  }

  @Test
  fun `archive a challenge for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val entity = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    val request = ArchiveChallengeRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, entity.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val challengeEntities = challengeRepository.findAllByPrisonNumber(prisonNumber)

    assertThat(challengeEntities).hasSize(1)
    assertThat(challengeEntities.first().active).isEqualTo(false)
    assertThat(challengeEntities.first().archiveReason).isEqualTo("archive reason")
  }

  @Test
  fun `attempt to archive a challenge that is already archived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val entity = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        archiveReason = "archive reason",
        active = false,
      ),
    )

    val request = ArchiveChallengeRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, entity.reference)
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
      .hasUserMessage("Challenge with reference [${entity.reference}] has been archived for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to archive a challenge that is from an ALN screener`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val alnScreener = alnScreenerRepository.saveAndFlush(
      ALNScreenerEntity(
        screeningDate = LocalDate.now(),
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        prisonNumber = prisonNumber,
      ),
    )

    val entity = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        alnScreenerId = alnScreener.id,
      ),
    )

    val request = ArchiveChallengeRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, entity.reference)
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
      .hasUserMessage("Challenge with reference [${entity.reference}] cannot be archived as it is an ALN screener challenge for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to archive a challenge that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID().toString()
    val request = ArchiveChallengeRequest(prisonId = "BXI", archiveReason = "archive reason")

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
      .hasUserMessage("Challenge with reference [$ref] not found for prisoner [$prisonNumber]")
  }
}

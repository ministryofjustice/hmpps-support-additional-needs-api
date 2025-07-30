package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import java.time.LocalDate

class GetChallengesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges"
  }

  @Test
  fun `Get list of non screener challenges for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    challengeRepository.saveAll(
      listOf(
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = memory,
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
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.challenges).hasSize(2)

    val challengeCodes = actual.challenges.map { it.challengeType.code }
    assertThat(challengeCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.challenges.forEach {
      assertThat(it.fromALNScreener).isFalse()
    }
  }

  @Test
  fun `Get list of screener challenges for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    val alnScreener = alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2020-01-01")))
    challengeRepository.saveAll(
      listOf(
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
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
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.challenges).hasSize(2)

    val challengeCodes = actual.challenges.map { it.challengeType.code }
    assertThat(challengeCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.challenges.forEach {
      assertThat(it.fromALNScreener).isTrue()
    }
  }

  @Test
  fun `Return no challenges for a given prisoner when the latest screener has none`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    // screener 1 has challenges
    val alnScreener = alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2020-01-01")))
    challengeRepository.saveAll(
      listOf(
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
      ),
    )
    // screener2 - has no challenges needs to be a small delay or they will both have the same timestamp
    Thread.sleep(100)
    alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2022-01-01")))

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.challenges).hasSize(0)
  }

  @Test
  fun `Return empty list when no challenges exist for prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // No challenges are created or saved for this prison number

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.challenges).isEmpty()
  }
}

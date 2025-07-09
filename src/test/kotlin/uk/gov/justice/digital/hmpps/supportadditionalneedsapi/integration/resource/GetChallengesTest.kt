package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse

class GetChallengesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges"
  }

  @Test
  fun `Get list of challenges for a given prisoner`() {
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

    val challengeCodes = actual.challenges.map { it.challengeTypeCode }
    assertThat(challengeCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
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

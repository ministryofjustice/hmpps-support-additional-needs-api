package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import java.util.*

class UpdateChallengesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges/{reference}"
  }

  @Test
  fun `update a challenge for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY"))
      ?: throw IllegalStateException("Reference data not found")
    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val challenges = challengeRepository.saveAll(
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

    val challenge = challenges.find { it.challengeType.code == "MEMORY" } ?: throw IllegalStateException("challenge not found")
    val updateChallengeRequest = UpdateChallengeRequest(active = false, prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateChallengeRequest)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ChallengeResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val updatedChallenge =
      challengeRepository.findAllByPrisonNumber(prisonNumber).find { it.challengeType.code == "MEMORY" }
        ?: throw IllegalStateException("challenge not found")

    assertThat(updatedChallenge.active).isEqualTo(false)
    assertThat(updatedChallenge.updatedAtPrison).isEqualTo("FKL")
  }

  @Test
  fun `attempt to update a challenge where a challenge doesnt exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val updateChallengeRequest = UpdateChallengeRequest(active = false, prisonId = "FKL")

    val ref = UUID.randomUUID().toString()
    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateChallengeRequest)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.userMessage).isEqualTo("Challenge with reference [$ref] not found for prisoner [$prisonNumber]")
  }
}

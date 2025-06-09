package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest

class CreateChallengeTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges"
  }

  @Test
  fun `Create a list of challenges for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val savedChallenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(savedChallenges.size).isEqualTo(2)
    val memoryChallenge = savedChallenges.find { it.challengeType.key.code == "MEMORY" }
    assertThat(memoryChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(memoryChallenge.challengeType.key.code).isEqualTo("MEMORY")
    assertThat(memoryChallenge.fromALNScreener).isFalse()
    assertThat(memoryChallenge.createdAtPrison).isEqualTo("BXI")

    val processingSpeedChallenge = savedChallenges.find { it.challengeType.key.code == "PROCESSING_SPEED" }
    assertThat(processingSpeedChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeedChallenge.challengeType.key.code).isEqualTo("PROCESSING_SPEED")
    assertThat(processingSpeedChallenge.fromALNScreener).isFalse()
    assertThat(processingSpeedChallenge.createdAtPrison).isEqualTo("BXI")
  }

  @Test
  fun `Fail when request contains duplicate challenge codes`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val duplicateChallengesList = CreateChallengesRequest(
      listOf(
        ChallengeRequest(
          "BXI",
          "MEMORY",
        ),
        ChallengeRequest(
          "BXI",
          "MEMORY", // Duplicate code
        ),
      ),
    )

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(duplicateChallengesList)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody(String::class.java)
      .returnResult()

    // Then
    val errorMessage = response.responseBody
    assertThat(errorMessage).contains("Attempted to add duplicate challenge(s)")
  }

  private fun createChallengesList(prisonNumber: String): CreateChallengesRequest = CreateChallengesRequest(
    listOf(
      ChallengeRequest(
        "BXI",
        "MEMORY",
      ),
      ChallengeRequest(
        "BXI",
        "PROCESSING_SPEED",
      ),
    ),
  )
}

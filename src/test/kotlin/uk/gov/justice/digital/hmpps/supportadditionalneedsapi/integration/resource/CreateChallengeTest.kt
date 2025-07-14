package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

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
    assertThat(memoryChallenge.howIdentified).isEqualTo(setOf(IdentificationSource.WIDER_PRISON))
    assertThat(memoryChallenge.symptoms).isEqualTo("Struggles to remember a sequence of numbers")

    val processingSpeedChallenge = savedChallenges.find { it.challengeType.key.code == "SPEED_OF_CALCULATION" }
    assertThat(processingSpeedChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeedChallenge.challengeType.key.code).isEqualTo("SPEED_OF_CALCULATION")
    assertThat(processingSpeedChallenge.challengeType.areaDescription).isEqualTo("Cognition & Learning")
    assertThat(processingSpeedChallenge.challengeType.categoryDescription).isEqualTo("Numeracy Skills")
    assertThat(processingSpeedChallenge.fromALNScreener).isFalse()
    assertThat(processingSpeedChallenge.createdAtPrison).isEqualTo("BXI")
    assertThat(processingSpeedChallenge.reference).isNotNull()
  }

  private fun createChallengesList(prisonNumber: String): CreateChallengesRequest = CreateChallengesRequest(
    listOf(
      ChallengeRequest(
        prisonId = "BXI",
        challengeTypeCode = "MEMORY",
        howIdentified = listOf(IdentificationSourceModel.WIDER_PRISON),
        symptoms = "Struggles to remember a sequence of numbers",
      ),
      ChallengeRequest(
        prisonId = "BXI",
        challengeTypeCode = "SPEED_OF_CALCULATION",
      ),
    ),
  )
}

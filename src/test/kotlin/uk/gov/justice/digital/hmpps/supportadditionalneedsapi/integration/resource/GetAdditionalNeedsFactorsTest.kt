package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.AdditionalNeedsFactorsResponse

class GetAdditionalNeedsFactorsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/additional-needs-factors"
  }

  @Test
  fun `Get list of additional needs factors for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    nonAlnChallengesExist(prisonNumber)
    conditionsExist(prisonNumber)
    nonAlnStrengthsExist(prisonNumber)
    supportStrategiesExist(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(AdditionalNeedsFactorsResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.challenges).hasSize(2)

    val challengeCodes = actual.challenges.map { it.challengeType.code }
    assertThat(challengeCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.challenges.forEach {
      assertThat(it.fromALNScreener).isFalse()

      assertThat(actual.conditions).hasSize(3)

      val conditionCodes = actual.conditions.map { it.conditionType.code }
      assertThat(conditionCodes).containsExactlyInAnyOrder("ADHD", "DYSLEXIA", "MENTAL_HEALTH")
    }

    assertThat(actual.strengths).hasSize(2)

    val strengthCodes = actual.strengths.map { it.strengthType.code }
    assertThat(strengthCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.strengths.forEach {
      assertThat(it.fromALNScreener).isFalse()
    }

    assertThat(actual.supportStrategies).hasSize(3)

    val strategyCodes = actual.supportStrategies.map { it.supportStrategyType.code }
    assertThat(strategyCodes).containsExactlyInAnyOrder("PROCESSING_SPEED", "GENERAL", "SENSORY")
  }
}

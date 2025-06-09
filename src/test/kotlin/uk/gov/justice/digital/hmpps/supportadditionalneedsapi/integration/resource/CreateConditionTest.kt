package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source as EntitySource

class CreateConditionTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/conditions"
  }

  @Test
  fun `Create a list of conditions for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val conditionsList: CreateConditionsRequest = createConditionsList(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(conditionsList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ConditionListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val savedConditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(savedConditions.size).isEqualTo(3)
    val adhdCondition = savedConditions.find { it.conditionType.key.code == "ADHD" }
    assertThat(adhdCondition!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(adhdCondition.conditionType.key.code).isEqualTo("ADHD")
    assertThat(adhdCondition.source).isEqualTo(EntitySource.SELF_DECLARED)
    assertThat(adhdCondition.createdAtPrison).isEqualTo("BXI")

    val dyslexiaCondition = savedConditions.find { it.conditionType.key.code == "DYSLEXIA" }
    assertThat(dyslexiaCondition!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(dyslexiaCondition.conditionType.key.code).isEqualTo("DYSLEXIA")
    assertThat(dyslexiaCondition.source).isEqualTo(EntitySource.ALN_SCREENER)
    assertThat(dyslexiaCondition.createdAtPrison).isEqualTo("BXI")

    val mentalHealthCondition = savedConditions.find { it.conditionType.key.code == "MENTAL_HEALTH" }
    assertThat(mentalHealthCondition!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(mentalHealthCondition.conditionType.key.code).isEqualTo("MENTAL_HEALTH")
    assertThat(mentalHealthCondition.source).isEqualTo(EntitySource.CONFIRMED_DIAGNOSIS)
    assertThat(mentalHealthCondition.createdAtPrison).isEqualTo("BXI")
  }

  @Test
  fun `Fail when request contains duplicate condition codes`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val duplicateConditionsList = CreateConditionsRequest(
      listOf(
        ConditionRequest(
          Source.SELF_DECLARED,
          "BXI",
          "ADHD",
        ),
        ConditionRequest(
          Source.ALN_SCREENER,
          "BXI",
          "ADHD", // Duplicate code
        ),
      )
    )

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(duplicateConditionsList)
      .exchange()
      .expectStatus()
      .isBadRequest
      .expectBody(String::class.java)
      .returnResult()

    // Then
    val errorMessage = response.responseBody
    assertThat(errorMessage).contains("Attempted to add duplicate condition(s)")
  }



  private fun createConditionsList(prisonNumber: String): CreateConditionsRequest = CreateConditionsRequest(
    listOf(
      ConditionRequest(
        Source.SELF_DECLARED,
        "BXI",
        "ADHD",
      ),
      ConditionRequest(
        Source.ALN_SCREENER,
        "BXI",
        "DYSLEXIA",
      ),
      ConditionRequest(
        Source.CONFIRMED_DIAGNOSIS,
        "BXI",
        "MENTAL_HEALTH",
      ),
    ),
  )
}

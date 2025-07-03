package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.EducationNeedRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat

class CreateTestSetupTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/set-up-data"
  }

  @Test
  fun `Set up data for a prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val educationNeedRequest = createEducationNeedRequest()

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(educationNeedRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(PlanCreationScheduleEntity::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
  }

  private fun createEducationNeedRequest(): EducationNeedRequest = EducationNeedRequest(
    prisonId = "BXI",
    alnNeed = true,
    lddNeed = true,
    conditionSelfDeclared = true,
    conditionConfirmed = true,
    challengeNotALN = true,
    inEducation = true,
  )
}

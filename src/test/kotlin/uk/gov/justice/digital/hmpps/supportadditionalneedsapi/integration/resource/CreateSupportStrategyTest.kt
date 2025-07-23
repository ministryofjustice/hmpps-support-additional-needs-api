package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateSupportStrategiesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyRequest

class CreateSupportStrategyTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies"
  }

  @Test
  fun `Create a list of support strategies for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val supportStrategies = createSupportStrategyList()

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(supportStrategies)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(SupportStrategyListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val savedStrategies = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(savedStrategies.size).isEqualTo(2)

    val processingSpeed = savedStrategies.find { it.supportStrategyType.key.code == "PROCESSING_SPEED" }
    assertThat(processingSpeed!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeed.supportStrategyType.key.code).isEqualTo("PROCESSING_SPEED")
    assertThat(processingSpeed.createdAtPrison).isEqualTo("BXI")
    assertThat(processingSpeed.detail).isEqualTo("Needs quiet space to focus")

    val visualAids = savedStrategies.find { it.supportStrategyType.key.code == "PHYSICAL_SKILLS_DEFAULT" }
    assertThat(visualAids!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(visualAids.supportStrategyType.key.code).isEqualTo("PHYSICAL_SKILLS_DEFAULT")
    assertThat(visualAids.createdAtPrison).isEqualTo("BXI")
  }

  private fun createSupportStrategyList(): CreateSupportStrategiesRequest = CreateSupportStrategiesRequest(
    supportStrategies = listOf(
      SupportStrategyRequest(
        supportStrategyTypeCode = "PROCESSING_SPEED",
        prisonId = "BXI",
        detail = "Needs quiet space to focus",
      ),
      SupportStrategyRequest(
        supportStrategyTypeCode = "PHYSICAL_SKILLS_DEFAULT",
        prisonId = "BXI",
      ),
    ),
  )
}

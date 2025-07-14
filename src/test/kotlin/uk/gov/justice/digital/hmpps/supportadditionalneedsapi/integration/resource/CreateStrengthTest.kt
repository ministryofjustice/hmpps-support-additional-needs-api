package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

class CreateStrengthTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/strengths"
  }

  @Test
  fun `Create a list of strengths for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val strengthsList = createStrengthsList(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(strengthsList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val savedStrengths = strengthRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(savedStrengths.size).isEqualTo(2)
    val memoryStrength = savedStrengths.find { it.strengthType.key.code == "MEMORY" }
    assertThat(memoryStrength!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(memoryStrength.strengthType.key.code).isEqualTo("MEMORY")
    assertThat(memoryStrength.fromALNScreener).isFalse()
    assertThat(memoryStrength.createdAtPrison).isEqualTo("BXI")
    assertThat(memoryStrength.howIdentified).isEqualTo(setOf(IdentificationSource.WIDER_PRISON))
    assertThat(memoryStrength.symptoms).isEqualTo("Is really good at remembering a sequence of numbers")

    val processingSpeedStrength = savedStrengths.find { it.strengthType.key.code == "SPEED_OF_CALCULATION" }
    assertThat(processingSpeedStrength!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeedStrength.strengthType.key.code).isEqualTo("SPEED_OF_CALCULATION")
    assertThat(processingSpeedStrength.strengthType.areaDescription).isEqualTo("Cognition & Learning")
    assertThat(processingSpeedStrength.strengthType.categoryDescription).isEqualTo("Numeracy Skills")
    assertThat(processingSpeedStrength.fromALNScreener).isFalse()
    assertThat(processingSpeedStrength.createdAtPrison).isEqualTo("BXI")
    assertThat(processingSpeedStrength.reference).isNotNull()
  }

  @Test
  fun `Fail when request contains duplicate strength codes`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val duplicateStrengthsList = CreateStrengthsRequest(
      listOf(
        StrengthRequest(
          prisonId = "BXI",
          strengthTypeCode = "MEMORY",
        ),
        StrengthRequest(
          prisonId = "BXI",
          strengthTypeCode = "MEMORY", // Duplicate code
        ),
      ),
    )

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(duplicateStrengthsList)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody(ErrorResponse::class.java)
      .returnResult()

    // Then
    val actual = response.responseBody
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Attempted to add duplicate strength(s) MEMORY for prisoner [$prisonNumber]")
  }

  private fun createStrengthsList(prisonNumber: String): CreateStrengthsRequest = CreateStrengthsRequest(
    listOf(
      StrengthRequest(
        prisonId = "BXI",
        strengthTypeCode = "MEMORY",
        howIdentified = listOf(IdentificationSourceModel.WIDER_PRISON),
        symptoms = "Is really good at remembering a sequence of numbers",
      ),
      StrengthRequest(
        prisonId = "BXI",
        strengthTypeCode = "SPEED_OF_CALCULATION",
      ),
    ),
  )
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource.OTHER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class UpdateStrengthsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/strengths/{reference}"
  }

  @Test
  fun `update a strength for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val strengths = strengthRepository.saveAll(
      listOf(
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )

    val strength = strengths.find { it.strengthType.code == "MEMORY" } ?: throw IllegalStateException("strength not found")
    val updateStrengthRequest = UpdateStrengthRequest(symptoms = "updated symptoms", howIdentified = listOf(IdentificationSource.OTHER), howIdentifiedOther = "how identified other", prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strength.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateStrengthRequest)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val updatedStrength =
      strengthRepository.findAllByPrisonNumber(prisonNumber).find { it.strengthType.code == "MEMORY" }
        ?: throw IllegalStateException("strength not found")

    assertThat(updatedStrength.symptoms).isEqualTo("updated symptoms")
    assertThat(updatedStrength.howIdentified).isEqualTo(linkedSetOf(OTHER))
    assertThat(updatedStrength.howIdentifiedOther).isEqualTo("how identified other")
    assertThat(updatedStrength.updatedAtPrison).isEqualTo("FKL")
  }

  @Test
  fun `attempt to update a strength where a strength doesnt exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val updateStrengthRequest = UpdateStrengthRequest(symptoms = "updated symptoms", howIdentified = listOf(IdentificationSource.OTHER), howIdentifiedOther = "how identified other", prisonId = "FKL")

    val ref = UUID.randomUUID().toString()
    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateStrengthRequest)
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Strength with reference [$ref] not found for prisoner [$prisonNumber]")
  }
}

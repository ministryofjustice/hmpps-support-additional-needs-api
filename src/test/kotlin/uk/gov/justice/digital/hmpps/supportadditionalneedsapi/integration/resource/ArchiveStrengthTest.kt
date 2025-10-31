package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class ArchiveStrengthTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/strengths/{reference}/archive"
  }

  @Test
  fun `archive a strength for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val entity = strengthRepository.saveAndFlush(
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, entity.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val strengthEntities = strengthRepository.findAllByPrisonNumber(prisonNumber)

    assertThat(strengthEntities).hasSize(1)
    assertThat(strengthEntities.first().active).isEqualTo(false)
  }

  @Test
  fun `attempt to archive a strength that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID().toString()

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
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

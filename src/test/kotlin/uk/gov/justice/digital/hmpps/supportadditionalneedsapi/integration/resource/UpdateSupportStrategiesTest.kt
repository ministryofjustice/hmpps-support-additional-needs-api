package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateSupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class UpdateSupportStrategiesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/support-strategies/{reference}"
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "updated detail",
      "  updated detail  ",
      """Updated detail with a trailing line break
""",
      """
Updated detail with a leading line break
""",
      """

     Updated detail with a leading and trailing line breaks

""",
      """Updated detail
Split over several lines

Including blank lines to simulate paragraph breaks.
""",
    ],
  )
  fun `update a support strategy for a given prisoner`(detail: String) {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val strategies = setupPrisonerSupportStrategies(prisonNumber)
    val strategy = strategies.first()
    val updateSupportStrategyRequest = UpdateSupportStrategyRequest(detail, prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateSupportStrategyRequest)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SupportStrategyResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val updatedStrategy =
      supportStrategyRepository.findAllByPrisonNumber(prisonNumber).find { it.supportStrategyType.code == "PROCESSING_SPEED" }
        ?: throw IllegalStateException("support strategy not found")

    assertThat(updatedStrategy.detail).isEqualTo(detail)
    assertThat(updatedStrategy.updatedAtPrison).isEqualTo("FKL")
  }

  @ParameterizedTest
  @ValueSource(
    strings = [
      "",
      "  ",
      """
""",
      """
  
""",
      """

     

""",
    ],
  )
  fun `should not update a support strategy given invalid detail`(detail: String) {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val strategies = setupPrisonerSupportStrategies(prisonNumber)
    val strategy = strategies.first()
    val updateSupportStrategyRequest = UpdateSupportStrategyRequest(detail, prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, strategy.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateSupportStrategyRequest)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.BAD_REQUEST.value())
      .hasUserMessageContaining("Validation failed for object='updateSupportStrategyRequest'")
      .hasDeveloperMessageContaining("Error on field 'detail': rejected value [$detail], must match \".*\\S.*\"")
  }

  @Test
  fun `attempt to update a support strategy that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val updateRequest = UpdateSupportStrategyRequest(detail = "updated detail", prisonId = "FKL")
    val ref = UUID.randomUUID().toString()

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateRequest)
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Support Strategy with reference [$ref] not found for prisoner [$prisonNumber]")
  }

  private fun setupPrisonerSupportStrategies(prisonNumber: String): List<SupportStrategyEntity> {
    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found")

    return supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          detail = "Needs quiet space to focus",
          active = true,
        ),
      ),
    )
  }
}

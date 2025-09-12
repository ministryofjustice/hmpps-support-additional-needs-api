package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.SetUpLDDRequest

class LDDSetupTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/ldd-setup"
  }

  @BeforeEach
  fun setup() {
    lddAssessmentRepository.deleteAll()
  }

  @Test
  fun `set up ldd needs for a list of prisoners`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val setUpLDDRequest = SetUpLDDRequest(
      listOf(
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
      ),
    )

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(setUpLDDRequest)
      .exchange()
      .expectStatus()
      .isCreated

    val alnAssessments = lddAssessmentRepository.findAll()
    assertThat(alnAssessments.size).isEqualTo(setUpLDDRequest.prisonNumbers.size)
  }

  @Test
  fun `set up ldd needs for a list of prisoners running twice`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val setUpLDDRequest = SetUpLDDRequest(
      listOf(
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
        randomValidPrisonNumber(),
      ),
    )

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(setUpLDDRequest)
      .exchange()
      .expectStatus()
      .isCreated

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(setUpLDDRequest)
      .exchange()
      .expectStatus()
      .isCreated

    val alnAssessments = lddAssessmentRepository.findAll()
    // should be the same number of assessments
    assertThat(alnAssessments.size).isEqualTo(setUpLDDRequest.prisonNumbers.size)
  }
}

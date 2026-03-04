package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber

class ExemptReviewScheduleTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/reviews/review-schedules/exempt"
  }

  @Test
  fun `should exempt a of review schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)

    // When
    webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk

    // Then
    val reviewSchedules = reviewScheduleRepository.findAllByPrisonNumber(prisonNumber)

    assertThat(reviewSchedules).hasSize(1)
    assertThat(reviewSchedules.first().status).isEqualTo(ReviewScheduleStatus.EXEMPT_UNKNOWN)
  }
}

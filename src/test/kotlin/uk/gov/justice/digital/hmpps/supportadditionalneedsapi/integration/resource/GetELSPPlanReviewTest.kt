package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanReviewsResponse

class GetELSPPlanReviewTest : IntegrationTestBase() {
  companion object {
    private const val URI_REVIEW = "/profile/{prisonNumber}/education-support-plan/review"
  }

  @Test
  fun `Get an ELSP plan review for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)
    val reviewSchedule = aValidReviewScheduleExists(prisonNumber)
    val review = anElSPReviewExists(prisonNumber, reviewSchedule.reference)

    // When
    val response = webTestClient.get()
      .uri(URI_REVIEW, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanReviewsResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.reviews).hasSize(1)
    assertThat(actual.reviews[0].reviewerFeedback).isEqualTo(review.reviewerFeedback)
    assertThat(actual.reviews[0].prisonerFeedback).isEqualTo(review.prisonerFeedback)
    assertThat(actual.reviews[0].prisonerDeclinedFeedback).isEqualTo(review.prisonerDeclinedFeedback)
  }
}

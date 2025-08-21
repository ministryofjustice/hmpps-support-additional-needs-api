package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEducationSupportPlanRequest
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus as ReviewScheduleStatusEntity

class CreateELSPPlanReviewTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/education-support-plan/review"
  }

  @Test
  fun `Create an ELSP plan review for a given prisoner where the plan has no changes`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planReviewRequest = createELSPPlanReviewRequest()
    anElSPExists(prisonNumber)
    aValidReviewScheduleExists(prisonNumber)

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(planReviewRequest)
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    val reviews = elspReviewRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(reviews).hasSize(1)

    val reviewSchedules = reviewScheduleRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(reviewSchedules).hasSize(2)
    assertThat(reviewSchedules[0].status).isEqualTo(ReviewScheduleStatusEntity.COMPLETED)

    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatusEntity.SCHEDULED)
    assertThat(reviewSchedule.deadlineDate).isEqualTo(planReviewRequest.nextReviewDate)

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.ELSP_REVIEW_CREATED)
  }

  fun createELSPPlanReviewRequest(): SupportPlanReviewRequest = SupportPlanReviewRequest(
    nextReviewDate = LocalDate.now().plusMonths(1),
    prisonId = "BXI",
    prisonerDeclinedFeedback = false,
    reviewerFeedback = "reviewerFeedback",
    prisonerFeedback = "prisonerFeedback",
    updateEducationSupportPlan = UpdateEducationSupportPlanRequest(anyChanges = false),
  )
}

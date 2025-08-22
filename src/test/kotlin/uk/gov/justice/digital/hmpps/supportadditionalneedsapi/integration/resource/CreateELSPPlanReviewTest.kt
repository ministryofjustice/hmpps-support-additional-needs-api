package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEducationSupportPlanRequest
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus as ReviewScheduleStatusEntity

class CreateELSPPlanReviewTest : IntegrationTestBase() {
  companion object {
    private const val URI_REVIEW = "/profile/{prisonNumber}/education-support-plan/review"
    private const val URI_REVIEW_SCHEDULES = "/profile/{prisonNumber}/reviews/review-schedules"
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
      .uri(URI_REVIEW, prisonNumber)
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

    // and check the schedule completed date is set:
    // When
    val response = webTestClient.get()
      .uri(URI_REVIEW_SCHEDULES, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReviewSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.reviewSchedules[0].reviewCompletedDate).isNull()
    assertThat(actual.reviewSchedules[0].reviewCompletedBy).isNull()
    assertThat(actual.reviewSchedules[0].reviewCompletedByJobRole).isNull()
    assertThat(actual.reviewSchedules[0].reviewKeyedInBy).isNull()
    assertThat(actual.reviewSchedules[0].status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
    assertThat(actual.reviewSchedules[1].reviewCompletedDate).isEqualTo(LocalDate.now())
    assertThat(actual.reviewSchedules[1].reviewCompletedBy).isEqualTo("Bob Smith")
    assertThat(actual.reviewSchedules[1].reviewCompletedByJobRole).isEqualTo("Teacher")
    assertThat(actual.reviewSchedules[1].reviewKeyedInBy).isEqualTo("Test User")
    assertThat(actual.reviewSchedules[1].status).isEqualTo(ReviewScheduleStatus.COMPLETED)
  }

  fun createELSPPlanReviewRequest(): SupportPlanReviewRequest = SupportPlanReviewRequest(
    nextReviewDate = LocalDate.now().plusMonths(1),
    prisonId = "BXI",
    prisonerDeclinedFeedback = false,
    reviewerFeedback = "reviewerFeedback",
    prisonerFeedback = "prisonerFeedback",
    reviewCreatedBy = ReviewContributor("Bob Smith", "Teacher"),
    updateEducationSupportPlan = UpdateEducationSupportPlanRequest(anyChanges = false),
  )
}

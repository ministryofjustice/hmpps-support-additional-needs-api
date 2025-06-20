package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntityKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetReviewSchedulesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/reviews/review-schedules"
  }

  @Test
  fun `should return a list of review schedules`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val reviewScheduleRecords = createReviewScheduleRecords(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReviewSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.reviewSchedules[0].createdByDisplayName).isEqualTo("Test User")
    assertThat(actual.reviewSchedules[0].updatedByDisplayName).isEqualTo("Test User")
    assertThat(actual.reviewSchedules).hasSize(reviewScheduleRecords.size)
  }

  private fun createReviewScheduleRecords(
    prisonNumber: String,
    reference: UUID = UUID.randomUUID(),
  ): List<ReviewScheduleHistoryEntity> = (1..3).map {
    val reviewScheduleHistoryEntity = ReviewScheduleHistoryEntity(
      reference = reference,
      prisonNumber = prisonNumber,
      deadlineDate = LocalDate.now().minusMonths(1),
      status = if (it == 3) ReviewScheduleStatus.SCHEDULED else ReviewScheduleStatus.COMPLETED,
      exemptionReason = null,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      createdBy = "testuser",
      createdAt = Instant.now(),
      updatedBy = "testuser",
      updatedAt = Instant.now(),
      id = ReviewScheduleHistoryEntityKey(revisionNumber = 0, id = UUID.randomUUID()),
      version = 1,
    )
    reviewScheduleHistoryRepository.saveAndFlush(reviewScheduleHistoryEntity)
  }
}

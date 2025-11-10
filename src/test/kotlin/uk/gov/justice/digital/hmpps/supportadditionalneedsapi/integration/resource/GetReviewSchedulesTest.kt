package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
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
    assertThat(actual!!.reviewSchedules).hasSizeGreaterThanOrEqualTo(reviewScheduleRecords.size)
    val statuses = actual.reviewSchedules.map { it.status.toString() }.toSet()
    assertThat(statuses).contains("SCHEDULED", "COMPLETED")
  }

  private fun createReviewScheduleRecords(
    prisonNumber: String,
  ): List<ReviewScheduleEntity> = (1..3).map { index ->
    val reviewScheduleEntity = ReviewScheduleEntity(
      reference = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      deadlineDate = LocalDate.now().minusMonths(1),
      status = if (index == 3) ReviewScheduleStatus.SCHEDULED else ReviewScheduleStatus.COMPLETED,
      exemptionReason = null,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
    )
    reviewScheduleRepository.saveAndFlush(reviewScheduleEntity)
  }
}

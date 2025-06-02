package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse
import java.time.LocalDate
import java.util.*

class GetReviewSchedulesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/reviews/review-schedules"
  }

  @Test
  fun `should return a list of review schedules`() {
    // Given
    stubGetTokenFromHmppsAuth()
    val prisonNumber = randomValidPrisonNumber()
    createReviewScheduleRecords(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__API__RO"), username = "system"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ReviewSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.reviewSchedules).hasSize(3)
  }

  private fun createReviewScheduleRecords(prisonNumber: String): ReviewScheduleEntity = (1..3).map {
    val reviewScheduleEntity = ReviewScheduleEntity(
      reference = UUID.randomUUID(),
      prisonNumber = prisonNumber,
      deadlineDate = LocalDate.now().minusMonths(1),
      status = if (it == 3) ReviewScheduleStatus.SCHEDULED else ReviewScheduleStatus.COMPLETED,
      exemptionReason = null,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
    )
    reviewScheduleRepository.saveAndFlush(reviewScheduleEntity)
  }.last()
}

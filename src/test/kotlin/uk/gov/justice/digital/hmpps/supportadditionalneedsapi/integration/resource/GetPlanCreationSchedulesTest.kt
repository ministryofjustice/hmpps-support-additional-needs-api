package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class GetPlanCreationSchedulesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/plan-creation-schedule"
  }

  @Test
  fun `should return a list of plan creation schedules`() {
    // Given
    stubGetTokenFromHmppsAuth()
    val prisonNumber = randomValidPrisonNumber()
    val reviewScheduleRecords = createPlanCreationScheduleRecords(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__API__RO"), username = "system"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanCreationSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.planCreationSchedules).hasSize(reviewScheduleRecords.size)
  }

  private fun createPlanCreationScheduleRecords(
    prisonNumber: String,
    reference: UUID = UUID.randomUUID(),
  ): List<PlanCreationScheduleHistoryEntity> = (1..3).map {
    val planCreationScheduleHistoryEntity = PlanCreationScheduleHistoryEntity(
      reference = reference,
      prisonNumber = prisonNumber,
      deadlineDate = LocalDate.now().minusMonths(1),
      status = if (it == 3) PlanCreationScheduleStatus.SCHEDULED else PlanCreationScheduleStatus.COMPLETED,
      exemptionReason = null,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      version = it,
      createdBy = "system",
      createdAt = Instant.now(),
      updatedBy = "system",
      updatedAt = Instant.now(),
    )
    planCreationScheduleHistoryRepository.saveAndFlush(planCreationScheduleHistoryEntity)
  }
}

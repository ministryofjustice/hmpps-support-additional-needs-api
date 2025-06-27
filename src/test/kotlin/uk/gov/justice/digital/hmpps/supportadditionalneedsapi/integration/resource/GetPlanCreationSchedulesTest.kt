package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationStatus
import java.time.LocalDate

class GetPlanCreationSchedulesTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/plan-creation-schedule"
  }

  @Test
  fun `should return a plan creation schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("system")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanCreationSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()

    assertThat(actual).isNotNull()
    assertThat(actual!!.planCreationSchedules[0].status).isEqualTo(PlanCreationStatus.SCHEDULED)
    assertThat(actual.planCreationSchedules[0].deadlineDate).isEqualTo(LocalDate.now().minusMonths(1))
  }

  @Test
  fun `should return plan creation schedules where there are multiple versions`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    // update the schedule
    schedule!!.status = PlanCreationScheduleStatus.COMPLETED
    planCreationScheduleRepository.save(schedule)

    // When
    val response = webTestClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(URI_TEMPLATE)
          .queryParam("includePastSchedules", true)
          .build(prisonNumber)
      }
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanCreationSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()

    assertThat(actual).isNotNull()
    assertThat(actual!!.planCreationSchedules[0].status).isEqualTo(PlanCreationStatus.SCHEDULED)
    assertThat(actual.planCreationSchedules[0].deadlineDate).isEqualTo(LocalDate.now().minusMonths(1))
    assertThat(actual.planCreationSchedules[1].status).isEqualTo(PlanCreationStatus.COMPLETED)
    assertThat(actual.planCreationSchedules[1].deadlineDate).isEqualTo(LocalDate.now().minusMonths(1))
  }

  @Test
  fun `should return single creation schedule history where there are multiple versions`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    // update the schedule
    schedule!!.status = PlanCreationScheduleStatus.COMPLETED
    planCreationScheduleRepository.save(schedule)

    // When
    val response = webTestClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(URI_TEMPLATE)
          .queryParam("includePastSchedules", false)
          .build(prisonNumber)
      }
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanCreationSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()

    assertThat(actual).isNotNull()
    assertThat(actual!!.planCreationSchedules[0].status).isEqualTo(PlanCreationStatus.COMPLETED)
    assertThat(actual.planCreationSchedules[0].deadlineDate).isEqualTo(LocalDate.now().minusMonths(1))
  }
}

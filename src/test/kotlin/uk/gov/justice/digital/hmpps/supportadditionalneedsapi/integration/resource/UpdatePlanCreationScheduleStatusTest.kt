package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationUpdateStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdatePlanCreationStatusRequest

class UpdatePlanCreationScheduleStatusTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/plan-creation-schedule/status"
  }

  @Test
  fun `update a plan creation schedule status to EXEMPT_PRISONER_NOT_COMPLY`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("system")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    val request = UpdatePlanCreationStatusRequest(
      prisonId = "LWI",
      exemptionReason = "don't want to",
      exemptionDetail = "because I say so",
      status = PlanCreationUpdateStatus.EXEMPT_PRISONER_NOT_COMPLY,
    )

    // When
    val response = webTestClient.patch()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(PlanCreationSchedulesResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()

    assertThat(actual).isNotNull()
    assertThat(actual!!.planCreationSchedules[1].status).isEqualTo(PlanCreationStatus.EXEMPT_PRISONER_NOT_COMPLY)
    assertThat(actual.planCreationSchedules[1].deadlineDate).isNull()
    assertThat(actual.planCreationSchedules[1].exemptionReason).isEqualTo("don't want to")
    assertThat(actual.planCreationSchedules[1].exemptionDetail).isEqualTo("because I say so")
    assertThat(actual.planCreationSchedules[1].updatedAtPrison).isEqualTo("LWI")
  }
}

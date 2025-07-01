package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus.COMPLETED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleExemptionReason
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
      exemptionReason = PlanCreationScheduleExemptionReason.EXEMPT_NOT_REQUIRED,
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
    assertThat(actual!!.planCreationSchedules[0].status).isEqualTo(PlanCreationStatus.EXEMPT_PRISONER_NOT_COMPLY)
    assertThat(actual.planCreationSchedules[0].deadlineDate).isNull()
    assertThat(actual.planCreationSchedules[0].exemptionReason).isEqualTo(PlanCreationScheduleExemptionReason.EXEMPT_NOT_REQUIRED)
    assertThat(actual.planCreationSchedules[0].exemptionDetail).isEqualTo("because I say so")
    assertThat(actual.planCreationSchedules[0].updatedAtPrison).isEqualTo("LWI")
  }

  @Test
  fun `update a plan creation schedule status to EXEMPT_PRISONER_NOT_COMPLY fail due to no reason`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("system")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    val request = UpdatePlanCreationStatusRequest(
      prisonId = "LWI",
      status = PlanCreationUpdateStatus.EXEMPT_PRISONER_NOT_COMPLY,
    )

    // When
    val response = webTestClient.patch()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual!!.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    assertThat(actual.userMessage).isEqualTo("Reason must be specified for Exemptions")
  }

  @Test
  fun `update a plan creation schedule status to EXEMPT_PRISONER_NOT_COMPLY fail due to status not being SCHEDULED`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("system")
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber, status = COMPLETED)
    val request = UpdatePlanCreationStatusRequest(
      prisonId = "LWI",
      exemptionReason = PlanCreationScheduleExemptionReason.EXEMPT_NOT_REQUIRED,
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
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual!!.status).isEqualTo(HttpStatus.CONFLICT.value())
    assertThat(actual.userMessage).isEqualTo("Plan creation schedule status must be [SCHEDULED] but was [COMPLETED] for prisoner [$prisonNumber]")
  }

  @Test
  fun `update a plan creation schedule status to EXEMPT_PRISONER_NOT_COMPLY fail due to no schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("system")
    val prisonNumber = randomValidPrisonNumber()
    val request = UpdatePlanCreationStatusRequest(
      prisonId = "LWI",
      exemptionReason = PlanCreationScheduleExemptionReason.EXEMPT_NOT_REQUIRED,
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
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual!!.status).isEqualTo(HttpStatus.NOT_FOUND.value())
    assertThat(actual.userMessage).isEqualTo("Plan creation schedule not found for prisoner [$prisonNumber]")
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import java.time.LocalDate

class GetELSPPlanTest : IntegrationTestBase() {
  companion object {
    const val URI_TEMPLATE = "/profile/{prisonNumber}/education-support-plan"
  }

  @Test
  fun `Get an ELSP plan for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planRequest = createPlanRequest()

    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(planRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(EducationSupportPlanResponse::class.java)

    // Then
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(EducationSupportPlanResponse::class.java)

    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    assertThat(actual!!.planCreatedBy?.name).isEqualTo(planRequest.planCreatedBy?.name)
    assertThat(actual.planCreatedBy?.jobRole).isEqualTo(planRequest.planCreatedBy?.jobRole)
    assertThat(actual.examAccessArrangements).isEqualTo(planRequest.examAccessArrangements)
    assertThat(actual.hasCurrentEhcp).isEqualTo(planRequest.hasCurrentEhcp)
    assertThat(actual.lnspSupport).isEqualTo(planRequest.lnspSupport)
    assertThat(actual.lnspSupportHours).isEqualTo(planRequest.lnspSupportHours)
    assertThat(actual.individualSupport).isEqualTo(planRequest.individualSupport)
    assertThat(actual.detail).isEqualTo(planRequest.detail)
    assertThat(actual.specificTeachingSkills).isEqualTo(planRequest.specificTeachingSkills)
    assertThat(actual.otherContributors?.get(0)?.name).isEqualTo(planRequest.otherContributors?.get(0)?.name)
    assertThat(actual.otherContributors?.get(0)?.jobRole).isEqualTo(planRequest.otherContributors?.get(0)?.jobRole)
  }

  private fun createPlanRequest(): CreateEducationSupportPlanRequest = CreateEducationSupportPlanRequest(
    prisonId = "BXI",
    hasCurrentEhcp = false,
    lnspSupport = "lnspSupport",
    lnspSupportHours = 99,
    teachingAdjustments = "teachingAdjustments",
    specificTeachingSkills = "specificTeachingSkills",
    examAccessArrangements = "examAccessArrangements",
    individualSupport = "individualSupport",
    detail = "detail",
    reviewDate = LocalDate.now(),
    planCreatedBy = PlanContributor("Fred Johns", "manager"),
    otherContributors = listOf(PlanContributor("John Smith", "education coordinator")),
  )

  @Test
  fun `return not found when prisoner already does not have a plan`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .expectBody(String::class.java)
      .returnResult()

    // Then
    val errorMessage = response.responseBody
    assertThat(errorMessage).contains("ELSP plan not found for prisoner [$prisonNumber]")
  }
}

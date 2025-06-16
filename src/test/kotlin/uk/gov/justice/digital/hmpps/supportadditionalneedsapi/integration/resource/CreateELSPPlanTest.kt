package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate

class CreateELSPPlanTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/education-support-plan"
  }

  @Test
  fun `Create an ELSP plan for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planRequest = createPlanRequest()

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(planRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(EducationSupportPlanResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val planFromService = elspPlanService.getPlan(prisonNumber)

    assertThat(planFromService).isNotNull()
    assertThat(planFromService.planCreatedBy?.name).isEqualTo(planRequest.planCreatedBy?.name)
    assertThat(planFromService.planCreatedBy?.jobRole).isEqualTo(planRequest.planCreatedBy?.jobRole)
    assertThat(planFromService.examAccessArrangements).isEqualTo(planRequest.examAccessArrangements)
    assertThat(planFromService.hasCurrentEhcp).isEqualTo(planRequest.hasCurrentEhcp)
    assertThat(planFromService.learningEnvironmentAdjustments).isEqualTo(planRequest.learningEnvironmentAdjustments)
    assertThat(planFromService.lnspSupport).isEqualTo(planRequest.lnspSupport)
    assertThat(planFromService.specificTeachingSkills).isEqualTo(planRequest.specificTeachingSkills)
    assertThat(planFromService.detail).isEqualTo(planRequest.detail)
    assertThat(planFromService.otherContributors?.get(0)?.name).isEqualTo(planRequest.otherContributors?.get(0)?.name)
    assertThat(planFromService.otherContributors?.get(0)?.jobRole).isEqualTo(planRequest.otherContributors?.get(0)?.jobRole)
  }

  private fun createPlanRequest(): CreateEducationSupportPlanRequest = CreateEducationSupportPlanRequest(
    prisonId = "BXI",
    hasCurrentEhcp = false,
    lnspSupport = "lnspSupport",
    learningEnvironmentAdjustments = "learningEnvironmentAdjustments",
    teachingAdjustments = "teachingAdjustments",
    specificTeachingSkills = "specificTeachingSkills",
    examAccessArrangements = "examAccessArrangements",
    detail = "detail",
    reviewDate = LocalDate.now(),
    planCreatedBy = PlanContributor("Fred Johns", "manager"),
    otherContributors = listOf(PlanContributor("John Smith", "education coordinator")),
  )

  @Test
  fun `Fail when prisoner already has a plan`() {
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

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(planRequest)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .expectBody(ErrorResponse::class.java)
      .returnResult()

    // Then
    val actual = response.responseBody
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Prisoner [$prisonNumber] already has a plan")
  }
}

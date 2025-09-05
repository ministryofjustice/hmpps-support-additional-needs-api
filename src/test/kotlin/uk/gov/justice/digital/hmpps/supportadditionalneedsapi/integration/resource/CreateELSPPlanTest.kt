package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus as ReviewScheduleStatusEntity

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
    aValidPlanCreationScheduleExists(prisonNumber)

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
    assertThat(planFromService.createdByDisplayName).isEqualTo("Test User")
    assertThat(planFromService.planCreatedBy?.name).isEqualTo(planRequest.planCreatedBy?.name)
    assertThat(planFromService.planCreatedBy?.jobRole).isEqualTo(planRequest.planCreatedBy?.jobRole)
    assertThat(planFromService.examAccessArrangements).isEqualTo(planRequest.examAccessArrangements)
    assertThat(planFromService.hasCurrentEhcp).isEqualTo(planRequest.hasCurrentEhcp)
    assertThat(planFromService.lnspSupport).isEqualTo(planRequest.lnspSupport)
    assertThat(planFromService.lnspSupportHours).isEqualTo(planRequest.lnspSupportHours)
    assertThat(planFromService.individualSupport).isEqualTo(planRequest.individualSupport)
    assertThat(planFromService.specificTeachingSkills).isEqualTo(planRequest.specificTeachingSkills)
    assertThat(planFromService.detail).isEqualTo(planRequest.detail)
    assertThat(planFromService.otherContributors?.get(0)?.name).isEqualTo(planRequest.otherContributors?.get(0)?.name)
    assertThat(planFromService.otherContributors?.get(0)?.jobRole).isEqualTo(planRequest.otherContributors?.get(0)?.jobRole)

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.COMPLETED)

    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatusEntity.SCHEDULED)
    assertThat(reviewSchedule.deadlineDate).isEqualTo(planRequest.reviewDate)

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.ELSP_CREATED)
  }

  @Test
  fun `Create an ELSP plan for a given prisoner when they have previously declined to create a plan`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planRequest = createPlanRequest()
    aValidPlanCreationScheduleExists(prisonNumber, status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY)

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
    assertThat(planFromService.createdByDisplayName).isEqualTo("Test User")
    assertThat(planFromService.planCreatedBy?.name).isEqualTo(planRequest.planCreatedBy?.name)
    assertThat(planFromService.planCreatedBy?.jobRole).isEqualTo(planRequest.planCreatedBy?.jobRole)
    assertThat(planFromService.examAccessArrangements).isEqualTo(planRequest.examAccessArrangements)
    assertThat(planFromService.hasCurrentEhcp).isEqualTo(planRequest.hasCurrentEhcp)
    assertThat(planFromService.lnspSupport).isEqualTo(planRequest.lnspSupport)
    assertThat(planFromService.lnspSupportHours).isEqualTo(planRequest.lnspSupportHours)
    assertThat(planFromService.individualSupport).isEqualTo(planRequest.individualSupport)
    assertThat(planFromService.specificTeachingSkills).isEqualTo(planRequest.specificTeachingSkills)
    assertThat(planFromService.detail).isEqualTo(planRequest.detail)
    assertThat(planFromService.otherContributors?.get(0)?.name).isEqualTo(planRequest.otherContributors?.get(0)?.name)
    assertThat(planFromService.otherContributors?.get(0)?.jobRole).isEqualTo(planRequest.otherContributors?.get(0)?.jobRole)

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.COMPLETED)

    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatusEntity.SCHEDULED)
    assertThat(reviewSchedule.deadlineDate).isEqualTo(planRequest.reviewDate)

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.ELSP_CREATED)
  }

  @Test
  fun `Create an ELSP plan given no LNSP support and support hours`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planRequest = createPlanRequest().copy(
      lnspSupport = null,
      lnspSupportHours = null,
    )
    aValidPlanCreationScheduleExists(prisonNumber)

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
    assertThat(planFromService.createdByDisplayName).isEqualTo("Test User")
    assertThat(planFromService.planCreatedBy?.name).isEqualTo(planRequest.planCreatedBy?.name)
    assertThat(planFromService.planCreatedBy?.jobRole).isEqualTo(planRequest.planCreatedBy?.jobRole)
    assertThat(planFromService.examAccessArrangements).isEqualTo(planRequest.examAccessArrangements)
    assertThat(planFromService.hasCurrentEhcp).isEqualTo(planRequest.hasCurrentEhcp)
    assertThat(planFromService.lnspSupport).isNull()
    assertThat(planFromService.lnspSupportHours).isNull()
    assertThat(planFromService.individualSupport).isEqualTo(planRequest.individualSupport)
    assertThat(planFromService.specificTeachingSkills).isEqualTo(planRequest.specificTeachingSkills)
    assertThat(planFromService.detail).isEqualTo(planRequest.detail)
    assertThat(planFromService.otherContributors?.get(0)?.name).isEqualTo(planRequest.otherContributors?.get(0)?.name)
    assertThat(planFromService.otherContributors?.get(0)?.jobRole).isEqualTo(planRequest.otherContributors?.get(0)?.jobRole)

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.COMPLETED)

    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatusEntity.SCHEDULED)
    assertThat(reviewSchedule.deadlineDate).isEqualTo(planRequest.reviewDate)

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.ELSP_CREATED)
  }

  @Test
  fun `Fail when request has LNSP support, but no support hours`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val planRequest = createPlanRequestNoSupportHours()
    aValidPlanCreationScheduleExists(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(planRequest)
      .exchange()
      .expectStatus()
      .isBadRequest
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasUserMessage("LNSP Support Hours must be specified if LNSP is populated")
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

  private fun createPlanRequestNoSupportHours(): CreateEducationSupportPlanRequest = CreateEducationSupportPlanRequest(
    prisonId = "BXI",
    hasCurrentEhcp = false,
    lnspSupport = "lnspSupport",
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

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

class CreateChallengeTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges"
  }

  @Test
  fun `Create a list of challenges for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val savedChallenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(savedChallenges.size).isEqualTo(2)
    val memoryChallenge = savedChallenges.find { it.challengeType.key.code == "MEMORY" }
    assertThat(memoryChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(memoryChallenge.challengeType.key.code).isEqualTo("MEMORY")
    assertThat(memoryChallenge.fromALNScreener).isFalse()
    assertThat(memoryChallenge.createdAtPrison).isEqualTo("BXI")
    assertThat(memoryChallenge.howIdentified).isEqualTo(setOf(IdentificationSource.WIDER_PRISON))
    assertThat(memoryChallenge.symptoms).isEqualTo("Struggles to remember a sequence of numbers")

    val processingSpeedChallenge = savedChallenges.find { it.challengeType.key.code == "SPEED_OF_CALCULATION" }
    assertThat(processingSpeedChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeedChallenge.challengeType.key.code).isEqualTo("SPEED_OF_CALCULATION")
    assertThat(processingSpeedChallenge.challengeType.areaDescription).isEqualTo("Cognition & Learning")
    assertThat(processingSpeedChallenge.challengeType.categoryDescription).isEqualTo("Numeracy Skills")
    assertThat(processingSpeedChallenge.fromALNScreener).isFalse()
    assertThat(processingSpeedChallenge.createdAtPrison).isEqualTo("BXI")
    assertThat(processingSpeedChallenge.reference).isNotNull()

    // check the timeline entries:
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].additionalInfo).isEqualTo("ChallengeType:MEMORY")
    assertThat(timelineEntries[1].additionalInfo).isEqualTo("ChallengeType:SPEED_OF_CALCULATION")
  }

  @Test
  fun `Create challenges for a prisoner where a person is in education - generate plan creation schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    prisonerInEducation(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    val planCreationScheduleEntity = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(planCreationScheduleEntity?.deadlineDate).isNull()
    assertThat(planCreationScheduleEntity?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `Create challenges for a prisoner where a person is in education - and has a plan creation schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    prisonerInEducation(prisonNumber)
    aValidPlanCreationScheduleExists(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    val planCreationScheduleEntity = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    // check that this new need doesn't change the deadline date
    assertThat(planCreationScheduleEntity?.deadlineDate).isNotNull()
    assertThat(planCreationScheduleEntity?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `Create challenges for a prisoner where a person is in education and has an ELSP - generate review schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    prisonerInEducation(prisonNumber)
    anElSPExists(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewScheduleEntity?.deadlineDate).isNull()
    assertThat(reviewScheduleEntity?.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }

  @Test
  fun `Create challenges for a prisoner where a person is in education and has an ELSP and review schedule - no change to review schedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    prisonerInEducation(prisonNumber)
    anElSPExists(prisonNumber)
    aValidReviewScheduleExists(prisonNumber)

    // When
    val response = webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(challengesList)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(reviewScheduleEntity?.deadlineDate).isNotNull()
    assertThat(reviewScheduleEntity?.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }

  private fun createChallengesList(): CreateChallengesRequest = CreateChallengesRequest(
    listOf(
      ChallengeRequest(
        prisonId = "BXI",
        challengeTypeCode = "MEMORY",
        howIdentified = listOf(IdentificationSourceModel.WIDER_PRISON),
        symptoms = "Struggles to remember a sequence of numbers",
      ),
      ChallengeRequest(
        prisonId = "BXI",
        challengeTypeCode = "SPEED_OF_CALCULATION",
      ),
    ),
  )
}

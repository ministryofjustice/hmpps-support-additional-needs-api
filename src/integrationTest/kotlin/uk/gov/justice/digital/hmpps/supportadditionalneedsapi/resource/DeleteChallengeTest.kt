package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate
import java.util.UUID

class DeleteChallengeTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/challenges/{reference}"
    private const val DEFAULT_QUERY = "?prisonId=BXI&reason=ENTERED_IN_ERROR"
  }

  @Test
  fun `delete a challenge for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val challenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(challenges).isEmpty()
  }

  @Test
  fun `delete one of several challenges for a prisoner leaves the others`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")

    val memoryChallenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )
    val sensoryChallenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = sensory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, memoryChallenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val remaining = challengeRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().reference).isEqualTo(sensoryChallenge.reference)
    assertThat(remaining.first().challengeType.key.code).isEqualTo("SENSORY_PROCESSING")
  }

  @Test
  fun `delete an archived (inactive) challenge`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val challenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        active = false,
        archiveReason = "previously archived",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(challenges).isEmpty()
  }

  @Test
  fun `delete a challenge that does not exist for the prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Challenge with reference [$ref] not found for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to delete a challenge that is from an ALN screener returns 409`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val alnScreener = alnScreenerRepository.saveAndFlush(
      ALNScreenerEntity(
        screeningDate = LocalDate.now(),
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        prisonNumber = prisonNumber,
      ),
    )

    val challenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        alnScreenerId = alnScreener.id,
      ),
    )

    // When
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Challenge with reference [${challenge.reference}] cannot be modified as it is an ALN screener challenge for prisoner [$prisonNumber]")

    // and the challenge is still present
    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber)).hasSize(1)
  }

  @Test
  fun `delete a challenge belonging to a different prisoner returns 404`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonerA = randomValidPrisonNumber()
    val prisonerB = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val challenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonerA,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When — try to delete prisonerA's challenge under prisonerB's path
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonerB, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Challenge with reference [${challenge.reference}] not found for prisoner [$prisonerB]")

    // and the original is untouched
    assertThat(challengeRepository.findAllByPrisonNumber(prisonerA)).hasSize(1)
  }

  @Test
  fun `delete a challenge with no role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf(), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete a challenge with read-only role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When — read-only role cannot delete
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete a challenge without prisonId query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?reason=ENTERED_IN_ERROR", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete a challenge without reason query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete a challenge with an unknown reason value`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI&reason=NOT_A_REAL_REASON", prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `deleting the only need-source cascades schedule to EXEMPT_NO_NEED`() {
    // Given
    stubForBankHoliday()
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    prisonerInEducation(prisonNumber)

    // create a single manual challenge via the existing API so the schedule is set up correctly
    val createResponse = webTestClient.post()
      .uri("/profile/{prisonNumber}/challenges", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(
        CreateChallengesRequest(
          listOf(
            ChallengeRequest(
              prisonId = "BXI",
              challengeTypeCode = "MEMORY",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    val created = createResponse.responseBody.blockFirst()!!
    val challengeReference = created.challenges.first().reference

    val scheduleBefore = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleBefore?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, challengeReference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    assertThat(needService.hasNeed(prisonNumber)).isFalse()
    val scheduleAfter = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleAfter?.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NO_NEED)
  }

  @Test
  fun `deleting one challenge does NOT cascade when prisoner still has another need source`() {
    // Given
    stubForBankHoliday()
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    prisonerInEducation(prisonNumber)

    val createResponse = webTestClient.post()
      .uri("/profile/{prisonNumber}/challenges", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(
        CreateChallengesRequest(
          listOf(
            ChallengeRequest(
              prisonId = "BXI",
              challengeTypeCode = "MEMORY",
            ),
            ChallengeRequest(
              prisonId = "BXI",
              challengeTypeCode = "SENSORY_PROCESSING",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ChallengeListResponse::class.java)

    val created = createResponse.responseBody.blockFirst()!!
    val memoryReference = created.challenges.first { it.challengeType.code == "MEMORY" }.reference

    val scheduleBefore = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleBefore?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, memoryReference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    assertThat(needService.hasNeed(prisonNumber)).isTrue()
    val scheduleAfter = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleAfter?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `delete records a CHALLENGE_DELETED timeline event with reference and reason in additionalInfo`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")

    val challenge = challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = memory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, challenge.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    val deletionEntry = timelineEntries.firstOrNull { it.event == TimelineEventType.CHALLENGE_DELETED }
    assertThat(deletionEntry).isNotNull()
    assertThat(deletionEntry!!.additionalInfo)
      .isEqualTo("challengeReference=${challenge.reference}|reason=ENTERED_IN_ERROR")
    assertThat(deletionEntry.createdAtPrison).isEqualTo("BXI")
  }
}

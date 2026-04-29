package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.UUID
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source as ModelSource

class DeleteConditionTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/conditions/{reference}"
    private const val DEFAULT_QUERY = "?prisonId=BXI&reason=ENTERED_IN_ERROR"
  }

  @Test
  fun `delete a condition for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(conditions).isEmpty()
  }

  @Test
  fun `delete one of several conditions for a prisoner leaves the others`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")
    val dyslexia = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "DYSLEXIA"))
      ?: throw IllegalStateException("Reference data not found")

    val adhdCondition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )
    val dyslexiaCondition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = dyslexia,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, adhdCondition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val remaining = conditionRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(remaining).hasSize(1)
    assertThat(remaining.first().reference).isEqualTo(dyslexiaCondition.reference)
    assertThat(remaining.first().conditionType.key.code).isEqualTo("DYSLEXIA")
  }

  @Test
  fun `delete an archived (inactive) condition`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        active = false,
        archiveReason = "previously archived",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(conditions).isEmpty()
  }

  @Test
  fun `delete a condition that does not exist for the prisoner`() {
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
      .hasUserMessage("Condition with reference [$ref] not found for prisoner [$prisonNumber]")
  }

  @Test
  fun `delete a condition belonging to a different prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonerA = randomValidPrisonNumber()
    val prisonerB = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonerA,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When — try to delete prisonerA's condition under prisonerB's path
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonerB, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Condition with reference [${condition.reference}] not found for prisoner [$prisonerB]")

    // and the original is untouched
    assertThat(conditionRepository.findAllByPrisonNumber(prisonerA)).hasSize(1)
  }

  @Test
  fun `delete a condition with no role`() {
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
  fun `delete a condition with wrong role`() {
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
  fun `delete a condition without prisonId query param`() {
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
  fun `delete a condition without reason query param`() {
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
  fun `delete a condition with an unknown reason value`() {
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

    // create a single condition via the existing API so the schedule is set up correctly
    val createResponse = webTestClient.post()
      .uri("/profile/{prisonNumber}/conditions", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(
        CreateConditionsRequest(
          listOf(
            ConditionRequest(
              source = ModelSource.SELF_DECLARED,
              prisonId = "BXI",
              conditionTypeCode = "ADHD",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ConditionListResponse::class.java)

    val created = createResponse.responseBody.blockFirst()!!
    val conditionReference = created.conditions.first().reference

    val scheduleBefore = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleBefore?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, conditionReference)
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
  fun `deleting one condition does NOT cascade when prisoner still has another need source`() {
    // Given
    stubForBankHoliday()
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    prisonerInEducation(prisonNumber)

    val createResponse = webTestClient.post()
      .uri("/profile/{prisonNumber}/conditions", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(
        CreateConditionsRequest(
          listOf(
            ConditionRequest(
              source = ModelSource.SELF_DECLARED,
              prisonId = "BXI",
              conditionTypeCode = "ADHD",
            ),
            ConditionRequest(
              source = ModelSource.SELF_DECLARED,
              prisonId = "BXI",
              conditionTypeCode = "DYSLEXIA",
            ),
          ),
        ),
      )
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(ConditionListResponse::class.java)

    val created = createResponse.responseBody.blockFirst()!!
    val adhdReference = created.conditions.first { it.conditionType.code == "ADHD" }.reference

    val scheduleBefore = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(scheduleBefore?.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, adhdReference)
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
  fun `delete records a CONDITION_DELETED timeline event with reference and reason in additionalInfo`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    val deletionEntry = timelineEntries.firstOrNull { it.event == TimelineEventType.CONDITION_DELETED }
    assertThat(deletionEntry).isNotNull()
    assertThat(deletionEntry!!.additionalInfo)
      .isEqualTo("conditionReference=${condition.reference}|reason=ENTERED_IN_ERROR")
    assertThat(deletionEntry.createdAtPrison).isEqualTo("BXI")
  }
}

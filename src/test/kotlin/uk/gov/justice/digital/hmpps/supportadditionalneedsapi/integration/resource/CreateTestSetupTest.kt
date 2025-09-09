package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.EducationNeedRequest
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate

class CreateTestSetupTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}"
  }

  @Test
  fun `Set up data for a prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    stubForBankHoliday()
    val prisonNumber = randomValidPrisonNumber()
    val educationNeedRequest = createEducationNeedRequest()

    // When
    val response = webTestClient.post()
      .uri("$URI_TEMPLATE/set-up-data", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(educationNeedRequest)
      .exchange()
      .expectStatus()
      .isCreated
      .returnResult(PlanCreationScheduleEntity::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
  }

  @Test
  fun `test ALN trigger`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    stubForBankHoliday()
    val prisonNumber = randomValidPrisonNumber()
    stubGetCurious2LearnerAssessments(prisonNumber, createTestALNAssessment(prisonNumber, assessmentDate = LocalDate.of(2025, 1, 28)))

    // When
    webTestClient.post()
      .uri("$URI_TEMPLATE/aln-trigger", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val alnAssessment = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      assertThat(alnAssessment!!.hasNeed).isTrue()
      assertThat(alnAssessment.screeningDate).isEqualTo(LocalDate.of(2025, 1, 28))
    } matches { it != null }

    assertThat(needService.hasALNScreenerNeed(prisonNumber)).isTrue()
    assertThat(needService.hasNeed(prisonNumber)).isTrue()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_ASSESSMENT_TRIGGER)
    assertThat(timelineEntries[0].additionalInfo).contains("curiousReference:")
  }

  @Test
  fun `test Education trigger`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    stubForBankHoliday()
    val prisonNumber = randomValidPrisonNumber()
    stubGetCurious2InEducation(prisonNumber, inEducationResponse(prisonNumber, "PES"))

    // When
    webTestClient.post()
      .uri("$URI_TEMPLATE/education-trigger", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val education = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      assertThat(education!!.inEducation).isTrue()
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(enrolments).hasSize(1)
    assertThat(enrolments[0].endDate).isNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
  }

  private fun createEducationNeedRequest(): EducationNeedRequest = EducationNeedRequest(
    prisonId = "BXI",
    alnNeed = true,
    lddNeed = true,
    conditionSelfDeclared = true,
    conditionConfirmed = true,
    challengeNotALN = true,
    strengthNotALN = true,
    inEducation = true,
    alnScreener = true,
  )
}

fun inEducationResponse(prisonNumber: String, fundingType: String = "PES"): String = """{
    "v1": [],
    "v2": [
        {
            "prn": "$prisonNumber",
            "establishmentId": "CFI",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "60322457",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "2025-10-02",
            "learningPlannedEndDate": "2025-01-31",
            "learnerOnRemand": null,
            "isAccredited": true,
            "aimType": null,
            "fundingType": "$fundingType",
            "deliveryApproach": null,
            "deliveryLocationpostcode": null,
            "completionStatus": "Continuing",
            "learningActualEndDate": null,
            "outcome": null,
            "outcomeGrade": null,
            "outcomeDate": null,
            "withdrawalReason": null,
            "withdrawalReasonAgreed": null,
            "withdrawalReviewed": false
        }
    ]
}"""

fun createTestALNAssessment(prisonNumber: String, hasNeed: Boolean = true): String = """{
  "v2": {
    "assessments": {
      "aln": [
        {
          "assessmentDate": "2025-01-28",
          "assessmentOutcome": "${if (hasNeed) "Yes" else "No"}",
          "establishmentId": "123",
          "establishmentName": "WTI",
          "hasPrisonerConsent": "Yes",
          "stakeholderReferral": "yes"
        }
      ]
    },
    "prn": "$prisonNumber"
  }
}"""

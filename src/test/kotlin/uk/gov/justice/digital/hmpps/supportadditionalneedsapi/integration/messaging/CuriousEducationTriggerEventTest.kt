package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidEducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.util.*

@Isolated
class CuriousEducationTriggerEventTest : IntegrationTestBase() {

  @Test
  fun `should process Curious Education domain event and mark the person as being in education`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    // person has a need:
    aValidChallengeExists(prisonNumber)

    // Then
    putInEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    // person has a need:
    aValidChallengeExists(prisonNumber)
    putInEducationAndValidate(prisonNumber)
    // Then
    endEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education and Exempt the planCreationSchedule`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    // person has a need:
    aValidChallengeExists(prisonNumber)

    putInEducationAndValidate(prisonNumber)
    endEducationAndValidate(prisonNumber)

    // Then
    // the plan creationSchedule should be marked as exempt
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education and Exempt the reviewSchedule`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aValidReviewScheduleExists(prisonNumber)
    // has no need

    putInEducationAndValidate(prisonNumber)
    endEducationAndValidate(prisonNumber)

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  private fun putInEducationAndValidate(prisonNumber: String) {
    stubGetCurious2InEducation(prisonNumber, inEducationResponse(prisonNumber))
    // When
    val curiousReference = UUID.randomUUID()
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(curiousReference),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(sqsMessage)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val education = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      Assertions.assertThat(education!!.inEducation).isTrue()
      Assertions.assertThat(education.curiousReference).isEqualTo(curiousReference)
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    Assertions.assertThat(enrolments).hasSize(1)
    Assertions.assertThat(enrolments[0].endDate).isNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
    Assertions.assertThat(timelineEntries[0].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  private fun endEducationAndValidate(prisonNumber: String) {
    stubGetCurious2OutEducation(prisonNumber, endedEducationResponse(prisonNumber))
    // When
    val curiousReference = UUID.randomUUID()
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(curiousReference),
      description = "EDUCATION_COMPLETED",
    )
    sendCuriousEducationMessage(sqsMessage)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val education = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      Assertions.assertThat(education!!.inEducation).isFalse()
      Assertions.assertThat(education.curiousReference).isEqualTo(curiousReference)
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    Assertions.assertThat(enrolments).hasSize(1)
    Assertions.assertThat(enrolments[0].endDate).isNotNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[1].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
    Assertions.assertThat(timelineEntries[1].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  fun inEducationResponse(prisonNumber: String): String = """{
    "v1": [],
    "v2": [
        {
            "prn": "$prisonNumber",
            "establishmentId": "CFI",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "60322457",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "2025-01-01",
            "learningPlannedEndDate": "2025-01-31",
            "learnerOnRemand": null,
            "isAccredited": true,
            "aimType": null,
            "fundingType": "PES",
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

  fun endedEducationResponse(prisonNumber: String): String = """{
    "v1": [],
    "v2": [
        {
            "prn": "$prisonNumber",
            "establishmentId": "CFI",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "60322457",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "2025-01-01",
            "learningPlannedEndDate": "2025-01-31",
            "learnerOnRemand": null,
            "isAccredited": true,
            "aimType": null,
            "fundingType": "PES",
            "deliveryApproach": null,
            "deliveryLocationpostcode": null,
            "completionStatus": "Continuing",
            "learningActualEndDate": "2025-08-01",
            "outcome": null,
            "outcomeGrade": null,
            "outcomeDate": null,
            "withdrawalReason": null,
            "withdrawalReasonAgreed": null,
            "withdrawalReviewed": false
        }
    ]
}"""

  private fun sendCuriousEducationMessage(sqsMessage: SqsMessage) {
    sendDomainEvent(sqsMessage)
  }
}

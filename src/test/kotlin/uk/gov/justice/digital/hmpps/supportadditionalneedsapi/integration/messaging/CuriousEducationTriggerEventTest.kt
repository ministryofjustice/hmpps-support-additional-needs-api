package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidEducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.PLAN_DEADLINE_DAYS_TO_ADD
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.*

@Isolated
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CuriousEducationTriggerEventTest : IntegrationTestBase() {

  @BeforeAll
  fun beforeAll() {
    stubForBankHoliday()
  }

  @Test
  fun `should process Curious Education domain event and mark the person as being in education`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)

    // Then
    val educationStartDate = LocalDate.of(2025, 10, 2)
    putInEducationAndValidate(prisonNumber, educationStartDate = educationStartDate)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isEqualTo(educationStartDate)
    Assertions.assertThat(planCreationSchedule.createdAtPrison).isEqualTo("CFI")
    // because the education start date is greater than PES date then the deadline date is education start date + DEADLINE_DAYS_TO_ADD working days
    val expectedDate = workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, educationStartDate)
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(expectedDate)
  }

  @Test
  fun `should process Curious Education domain event and NOT mark the person as being in education as courses are in different prison`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "BXI")
    // person has a need:
    aValidChallengeExists(prisonNumber)

    // Then
    val educationStartDate = LocalDate.of(2025, 10, 2)
    processEducationAndValidatePrisonerNotInEducation(prisonNumber, educationStartDate = educationStartDate)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule).isNull()
  }

  @Test
  fun `should process Curious Education domain event with two educations and mark the person as being in education`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)

    // Then
    putInTwoEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isEqualTo(LocalDate.of(2025, 6, 12))
    // because the education start date is less than PES date then the deadline date is PES date + 5 working days
    val expectedDate =
      workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, LocalDate.of(2025, 10, 1))
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(expectedDate)
  }

  @Test
  fun `should process Curious Education domain event for non PES education - check schedule has IN FUTURE deadline date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)

    // Then person is on a non PES course
    putInEducationAndValidate(prisonNumber, "Not PES")
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
  }

  @Test
  fun `should process Curious Education domain event for PES education AND an ALN assessment message with the same assessment date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    createALNAssessmentMessage(prisonNumber, hasNeed = true, assessmentDate = LocalDate.now())

    // Then person is on a non PES course
    putInEducationAndValidate(prisonNumber, "PES", educationStartDate = LocalDate.now())
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNotNull()
    Assertions.assertThat(planCreationSchedule.deadlineDate).isBefore(IN_THE_FUTURE_DATE)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)
    putInEducationAndValidate(prisonNumber)
    // Then
    endEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  @Test
  fun `should put someone in education then out and then back in on the same course`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)
    putInEducationAndValidate(prisonNumber, educationStartDate = LocalDate.of(2025, 1, 1))
    // Then
    endEducationAndValidate(prisonNumber)
    // then
    restartEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education when the person had refused a plan`() {
    // Given
    stubGetTokenFromHmppsAuth()
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has a need:
    aValidChallengeExists(prisonNumber)
    aValidPlanCreationScheduleExists(prisonNumber, status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY)
    putInEducationAndValidate(prisonNumber)
    // Then
    endEducationAndValidate(prisonNumber)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as out of education and Exempt the planCreationSchedule`() {
    // Given
    stubGetTokenFromHmppsAuth()
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
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
    stubGetTokenFromHmppsAuth()
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    aValidReviewScheduleExists(prisonNumber)
    // has no need

    putInEducationAndValidate(prisonNumber)
    endEducationAndValidate(prisonNumber)

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
  }

  @Test
  fun `ensure education enrollments from other prisons are end dated`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val educationEnrolmentOne = EducationEnrolmentEntity(
      prisonNumber = prisonNumber,
      establishmentId = "BXI",
      qualificationCode = "4321",
      learningStartDate = LocalDate.now().minusMonths(1),
      plannedEndDate = LocalDate.now().plusMonths(1),
      fundingType = "Previous contract",
      completionStatus = null,
      endDate = null,
      lastCuriousReference = null,
    )
    val educationEnrolmentTwo = EducationEnrolmentEntity(
      prisonNumber = prisonNumber,
      establishmentId = "BXI",
      qualificationCode = "1234",
      learningStartDate = LocalDate.now().minusMonths(1),
      plannedEndDate = LocalDate.now().plusMonths(1),
      fundingType = "Previous contract",
      completionStatus = null,
      endDate = null,
      lastCuriousReference = null,
    )
    educationEnrolmentRepository.saveAll(listOf(educationEnrolmentOne, educationEnrolmentTwo))

    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    aValidReviewScheduleExists(prisonNumber)

    putInEducationAndValidate(prisonNumber, expectedNumberOfEnrolments = 3)
    endEducationAndValidate(prisonNumber, expectedNumberOfEnrolments = 3)

    // Then all the enrollments should be end dated
    educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber).forEach {
      Assertions.assertThat(it.endDate).isNotNull()
    }
  }

  @Test
  fun `already has a review schedule put on another education course and review schedule will be the sooner of the two dates`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    aValidReviewScheduleExists(prisonNumber = prisonNumber, deadlineDate = LocalDate.of(2025, 7, 1))
    // person has a need:
    aValidChallengeExists(prisonNumber)

    putInEducationAndValidate(prisonNumber)

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.deadlineDate).isEqualTo(LocalDate.of(2025, 7, 1))
  }

  @Test
  fun `Person put on another education course with a date before the plan date and review schedule date will not change`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    anElSPExists(prisonNumber = prisonNumber)
    aValidReviewScheduleExists(prisonNumber = prisonNumber, deadlineDate = LocalDate.now().plusMonths(2))
    // person has a need:
    aValidChallengeExists(prisonNumber)

    putInEducationAndValidate(prisonNumber)

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.deadlineDate).isEqualTo(LocalDate.now().plusMonths(2))
  }

  @Test
  fun `Person put on another education course with a date the same as the plan date and review schedule date will not change`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    anElSPExists(prisonNumber = prisonNumber)
    aValidReviewScheduleExists(prisonNumber = prisonNumber, deadlineDate = LocalDate.now().plusMonths(2))
    // person has a need:
    aValidChallengeExists(prisonNumber)

    putInEducationAndValidate(prisonNumber, educationStartDate = LocalDate.now())

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.deadlineDate).isEqualTo(LocalDate.now().plusMonths(2))
  }

  @Test
  fun `Person put on another education course with a date before the most recent review and review schedule date will not change`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    anOldElSPExists(prisonNumber = prisonNumber)
    val oldReviewSchedule = aValidReviewScheduleExists(prisonNumber = prisonNumber, deadlineDate = LocalDate.now().plusMonths(1), status = ReviewScheduleStatus.COMPLETED)
    anElSPReviewExists(prisonNumber = prisonNumber, oldReviewSchedule.reference)
    aValidReviewScheduleExists(prisonNumber = prisonNumber, deadlineDate = LocalDate.now().plusMonths(1))
    // person has a need:
    aValidChallengeExists(prisonNumber)
    putInEducationAndValidate(prisonNumber)

    // Then
    // the reviewSchedule should be marked as exempt
    val reviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewSchedule!!.deadlineDate).isEqualTo(LocalDate.now().plusMonths(1))
  }

  @Test
  fun `should process Curious Education domain event and due to already having an ELSP should create a review`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    // person has a need:
    aValidChallengeExists(prisonNumber)
    anElSPExists(prisonNumber)

    // Then
    val educationStartDate = LocalDate.of(2025, 10, 2)
    putInEducationAndValidate(prisonNumber, educationStartDate = educationStartDate)
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewScheduleEntity!!.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
    Assertions.assertThat(reviewScheduleEntity.deadlineDate).isNotNull()
    // this is the date that the education starts from the curious API
    val deadlineDate =
      workingDayService.getNextWorkingDayNDaysFromDate(reviewConfig.reviewDeadlineDaysToAdd, educationStartDate)

    Assertions.assertThat(reviewScheduleEntity.deadlineDate).isEqualTo(deadlineDate)
  }

  @Test
  fun `should process Curious Education domain event for PES education AND an ALN assessment message with the later assessment date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    stubGetTokenFromHmppsAuth()
    // person has a need:
    createALNAssessmentMessage(prisonNumber, hasNeed = true, assessmentDate = LocalDate.now().plusDays(5))

    // Then person is on a non PES course
    putInEducationAndValidate(prisonNumber, "PES", educationStartDate = LocalDate.now())
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
  }

  private fun processEducationAndValidatePrisonerNotInEducation(
    prisonNumber: String,
    fundingType: String = "PES",
    educationStartDate: LocalDate = LocalDate.now().minusMonths(5),
  ) {
    stubGetCurious2InEducation(prisonNumber, inEducationResponse(prisonNumber, fundingType, educationStartDate))
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
      Assertions.assertThat(education).isNull()
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    Assertions.assertThat(enrolments).hasSize(0)
  }

  private fun putInEducationAndValidate(
    prisonNumber: String,
    fundingType: String = "PES",
    educationStartDate: LocalDate = LocalDate.now().minusMonths(5),
    expectedNumberOfEnrolments: Int = 1,
  ) {
    stubGetCurious2InEducation(prisonNumber, inEducationResponse(prisonNumber, fundingType, educationStartDate))
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
      val enrolmentCount = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber).size
      Assertions.assertThat(enrolmentCount).isEqualTo(expectedNumberOfEnrolments)
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    Assertions.assertThat(enrolments).hasSize(expectedNumberOfEnrolments)
    Assertions.assertThat(enrolments[expectedNumberOfEnrolments - 1].endDate).isNull()
  }

  private fun putInTwoEducationAndValidate(prisonNumber: String, fundingType: String = "PES") {
    stubGetCurious2InEducation(prisonNumber, inEducationResponseWithTwoEducations(prisonNumber, fundingType))
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

  private fun endEducationAndValidate(prisonNumber: String, expectedNumberOfEnrolments: Int = 1) {
    stubGetCurious2OutEducation(prisonNumber, endedEducationResponse(prisonNumber))
    // When
    val curiousReference = UUID.randomUUID()
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(curiousReference),
      description = "EDUCATION_STOPPED",
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
    Assertions.assertThat(enrolments).hasSize(expectedNumberOfEnrolments)
    Assertions.assertThat(enrolments[expectedNumberOfEnrolments - 1].endDate).isNotNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[1].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
    Assertions.assertThat(timelineEntries[1].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  private fun restartEducationAndValidate(prisonNumber: String) {
    stubGetCurious2OutEducation(prisonNumber, restartedEducationResponse(prisonNumber))
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
    Assertions.assertThat(timelineEntries[2].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
    Assertions.assertThat(timelineEntries[2].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  fun inEducationResponse(prisonNumber: String, fundingType: String = "PES", educationStartDate: LocalDate): String = """{
    "v1": [],
    "v2": [
        {
            "prn": "$prisonNumber",
            "establishmentId": "CFI",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "60322457",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "$educationStartDate",
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

  fun restartedEducationResponse(prisonNumber: String): String = """{
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

  fun inEducationResponseWithTwoEducations(
    prisonNumber: String,
    fundingType: String = "PES",
    establishmentId: String = "CFI",
  ): String = """{
    "v1": [],
    "v2": [
        {
            "prn": "$prisonNumber",
            "establishmentId": "$establishmentId",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "1231231",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "2025-06-12",
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
        },
        {
            "prn": "$prisonNumber",
            "establishmentId": "$establishmentId",
            "establishmentName": "CARDIFF (HMP)",
            "qualificationCode": "60322457",
            "qualificationName": "Award in Cycle Maintenance",
            "learningStartDate": "2025-01-02",
            "learningPlannedEndDate": "2025-01-31",
            "learnerOnRemand": null,
            "isAccredited": true,
            "aimType": null,
            "fundingType": "$fundingType",
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

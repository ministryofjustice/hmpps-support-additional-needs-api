package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.specialtests.messaging

import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.test.context.ActiveProfiles
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousEducationCompletionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidEducationDto
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidV2Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.aValidEducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.aValidPrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleStatus as ReviewScheduleStatusApi

/**
 * Test class testing scenarios that typically involve several messages
 *
 * The tests use the ticking-clock profile as that uses a clock with a known date of 2026-03-21. This means that any
 * date calculation and expectations will be based from this date.
 */
@ActiveProfiles("integration-test", "ticking-clock")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class MultipleMessagesScenariosTest : IntegrationTestBase() {

  private lateinit var today: LocalDate

  @BeforeAll
  fun beforeAll() {
    today = LocalDate.now(clock)
    stubForBankHoliday()
  }

  /**
   * Scenario:
   * * Prisoner in PVI
   *   * On a PES course
   *   * No needs
   *   * Does not have an ELSP Creation schedule
   * * Prisoner transfers to HOI
   *   * Education stop message, but education staff in PVI have no yet marked the prisoner as withdrawn from the course
   * A need is recorded (conditions)
   * Education staff in PVI mark the prisoner as having withdrawn from the course, but no education stop message is sent/received
   * Prisoner enrols in a non-PES course in HOI
   *
   * Expectation is that a ELSP Creation Schedule is created, with a status of SCHEDULED and a deadline date of 2099-12-31.
   * A Plan Creation Schedule with a deadline of 2099-12-31 effectively means no deadline date.
   *
   * The prisoner has a need and is in education, though not a PES course. This means they can have an ELSP created if
   * they wish, but there are no contractural deadlines to do so.
   *
   * This scenario is essentially the prisoner in on both PES and non-PES courses, and has needs. But the needs are only conditions
   * and do not include an ALN Assessment indicating a need.
   * Prisoner needs to be on a PES course and have an ALN Assessment indicating a need in order to create a
   * Plan Creation Schedule with a deadline.
   */
  @Test
  fun `should create plan creation schedule with no deadline date given a prisoner with a need, PES course has ended, and a non-PES course is started`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists in PVI with no needs, is on a PES course, and has no ELSP Plan Creation Schedule
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "PVI",
    )
    prisonerInEducation(
      prisonNumber = prisonNumber,
      learningStartDate = today.minusWeeks(10),
      endDate = null,
      qualificationCode = "Bricklaying",
      establishmentId = "PVI",
    )
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          qualificationCode = "Bricklaying",
          establishmentId = "PVI",
          fundingType = "PES",
          completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
        ),
      ),
    )

    // prisoner transfers to HOI
    // reset the prisoner search stub so that when the transfer admission listener processes the message the prisoner is now in the correct prison
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "HOI",
    )
    sendPrisonerTransferMessage(
      prisonNumber = prisonNumber,
      prisonId = "HOI",
    )
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // shortly after the transfer message (usually within 5 minutes) Curious sends the Education Stop message because the prisoner has transferred
    // from PVI so therefore cannot be on a course in PVI. In this circumstance Curious cannot and do not automatically change the course
    // status, so typically we will receive the Education Stop message (following the Transfer), and when we call Curious API to
    // get the course details, the course is apparently still in progress.
    shortDelay()
    val educationStopMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STOPPED",
    )
    sendCuriousEducationMessage(educationStopMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Conditions are recorded for the prisoner, which indicates the prisoner has a need ("needs sources" will be conditions based and will not include an ALN Assessment)
    conditionsExist(prisonNumber)

    // Education staff in PVI retrospectively update the course in Curious to reflect the prisoner has having withdrawn
    // Prisoner also enrols in a non-PES course in HOI
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Bricklaying",
            establishmentId = "PVI",
            fundingType = "PES",
            completionStatus = CuriousEducationCompletionStatus.WITHDRAWN,
          ),
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Retrofit",
            establishmentId = "HOI",
            fundingType = "DPS",
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
        ),
      ),
    )

    // When
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    val planCreationSchedules = getPlanCreationSchedules(prisonNumber)
    assertThat(planCreationSchedules)
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasStatus(PlanCreationStatus.SCHEDULED)
          .hasDeadlineDate(LocalDate.parse("2099-12-31"))
      }
  }

  /**
   * Scenario:
   * * Prisoner in HOI
   *   * Has a previously withdrawn PES course from another prison
   *   * Is on a PES course in current prison
   *   * Has needs
   *   * Has had their ELSP created
   *   * Is in the review cycle
   * * Prisoner completes PES course in current prison
   *   * Review Schedule is marked as EXEMPT_NOT_IN_EDUCATION
   * * Prisoner starts a new non-PES course in the current prison
   *
   * Expectation is that the review schedule is not re-scheduled, and it is left as EXEMPT_NOT_IN_EDUCATION.
   *
   * Prisoners should only need a plan and reviews if they are in PES education and have a need.
   */
  @Disabled("Enable this test when the implementation has been fixed")
  @Test
  fun `should not reschedule a previously exempted review schedule given a prisoner with needs and a non-PES course is started`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists in HOI
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "HOI",
    )

    // prisoner has conditions
    conditionsExist(prisonNumber)

    // The SAN education_enrolment table has 2 courses recorded.
    // The first is from a previous prison and has an end date.
    // The second is for this prison and is an active course (no end date).
    prisonerInEducation(
      prisonNumber = prisonNumber,
      learningStartDate = today.minusWeeks(10),
      endDate = today.minusWeeks(2),
      fundingType = "PES",
      qualificationCode = "Bricklaying",
      establishmentId = "PVI",
    )
    prisonerInEducation(
      prisonNumber = prisonNumber,
      learningStartDate = today.minusWeeks(2),
      endDate = null,
      fundingType = "PES",
      qualificationCode = "Metalwork",
      establishmentId = "HOI",
    )

    // The prisoner has had their plan created (schedule is completed, with 2 history records)
    aValidPlanCreationScheduleExists(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.COMPLETED,
    )
    anElSPExists(prisonNumber)

    // prisoner has a review scheduled
    aValidReviewScheduleExists(
      prisonNumber = prisonNumber,
      status = ReviewScheduleStatus.SCHEDULED,
    )

    // The prisoner completes/un-enrols from the PES course in HOI.
    // The Curious education data shows the PES HOI course as being completed, and also the PVI course as being withdrawn
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Bricklaying",
            establishmentId = "PVI",
            fundingType = "PES",
            learningActualEndDate = today.minusWeeks(2),
            completionStatus = CuriousEducationCompletionStatus.WITHDRAWN,
          ),
          aValidV2Education(
            prn = prisonNumber,
            fundingType = "PES",
            qualificationCode = "Metalwork",
            establishmentId = "HOI",
            learningActualEndDate = today,
            completionStatus = CuriousEducationCompletionStatus.COMPLETED,
          ),
        ),
      ),
    )
    val educationStopMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STOPPED",
    )
    sendCuriousEducationMessage(educationStopMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // The review schedule should have been updated to be EXEMPT_NOT_IN_EDUCATION
    assertThat(getReviewSchedules(prisonNumber))
      .hasNumberOfReviewSchedules(2)
      .reviewSchedule(1) { it.hasStatus(ReviewScheduleStatusApi.SCHEDULED) }
      .reviewSchedule(2) { it.hasStatus(ReviewScheduleStatusApi.EXEMPT_NOT_IN_EDUCATION) }

    shortDelay()

    // When
    // the prisoner starts a new non-PES course
    // The Curious education data now shows the PVI course that was withdrawn, the PES HOI course that was completed, and
    // now the new non-PES course that has started
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Bricklaying",
            establishmentId = "PVI",
            fundingType = "PES",
            learningActualEndDate = today.minusWeeks(2),
            completionStatus = CuriousEducationCompletionStatus.WITHDRAWN,
          ),
          aValidV2Education(
            prn = prisonNumber,
            fundingType = "PES",
            qualificationCode = "Metalwork",
            establishmentId = "HOI",
            learningActualEndDate = today,
            completionStatus = CuriousEducationCompletionStatus.COMPLETED,
          ),
          aValidV2Education(
            prn = prisonNumber,
            fundingType = "DPS",
            qualificationCode = "Retrofit",
            establishmentId = "HOI",
            learningActualEndDate = null,
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
        ),
      ),
    )
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    // we do not expect the Review Schedules to have been re-scheduled
    assertThat(getReviewSchedules(prisonNumber))
      .hasNumberOfReviewSchedules(2)
      .reviewSchedule(1) { it.hasStatus(ReviewScheduleStatusApi.SCHEDULED) }
      .reviewSchedule(2) { it.hasStatus(ReviewScheduleStatusApi.EXEMPT_NOT_IN_EDUCATION) }
  }

  /**
   * Scenario:
   * * Prisoner
   *   * No needs
   *   * Does not have an ELSP Creation schedule
   * A need is recorded (conditions)
   * Prisoner enrols in a non-PES course
   *
   * Expectation is that a ELSP Creation Schedule is created, with a status of SCHEDULED and a deadline date of 2099-12-31.
   * A Plan Creation Schedule with a deadline of 2099-12-31 effectively means no deadline date.
   *
   * The prisoner has a need and is in education, though not a PES course. This means they can have an ELSP created if
   * they wish, but there are no contractural deadlines to do so.
   *
   * This scenario is essentially the prisoner in on a non-PES courses, and has needs. But the needs are only conditions
   * and do not include an ALN Assessment indicating a need.
   * Prisoner needs to be on a PES course and have an ALN Assessment indicating a need in order to create a
   * Plan Creation Schedule with a deadline.
   */
  @Test
  fun `should create plan creation schedule with no deadline date given a prisoner with a need, and a non-PES course is started`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists with no needs, and has no ELSP Plan Creation Schedule
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
    )

    // Conditions are recorded for the prisoner, which indicates the prisoner has a need ("needs sources" will be conditions based and will not include an ALN Assessment)
    conditionsExist(prisonNumber)

    // Prisoner enrols in a non-PES course
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Retrofit",
            establishmentId = "BXI",
            fundingType = "DPS",
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
        ),
      ),
    )

    // When
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    val planCreationSchedules = getPlanCreationSchedules(prisonNumber)
    assertThat(planCreationSchedules)
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasStatus(PlanCreationStatus.SCHEDULED)
          .hasDeadlineDate(LocalDate.parse("2099-12-31"))
      }
  }

  /**
   * Scenario:
   * * Prisoner
   *   * No needs
   *   * Does not have an ELSP Creation schedule
   * A need is recorded (conditions)
   * Prisoner enrols in a PES course
   *
   * Expectation is that a ELSP Creation Schedule is created, with a status of SCHEDULED and a deadline date of 2099-12-31.
   * A Plan Creation Schedule with a deadline of 2099-12-31 effectively means no deadline date.
   *
   * The prisoner has a need and is in education on a PES course. But the need does not include an ALN Assessment. This
   * means they can have an ELSP created if they wish, but there are no contractural deadlines to do so.
   *
   * This scenario is essentially the prisoner in on a PES courses, and has needs. But the needs are only conditions
   * and do not include an ALN Assessment indicating a need.
   * Prisoner needs to be on a PES course and have an ALN Assessment indicating a need in order to create a
   * Plan Creation Schedule with a deadline.
   */
  @Test
  fun `should create plan creation schedule with no deadline date given a prisoner with a need, and a PES course is started, but the need does not trigger the KPI rules`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists with no needs, and has no ELSP Plan Creation Schedule
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
    )

    // Conditions are recorded for the prisoner, which indicates the prisoner has a need ("needs sources" will be conditions based and will not include an ALN Assessment)
    conditionsExist(prisonNumber)

    // Prisoner enrols in a non-PES course
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Woodworking and joinery",
            establishmentId = "BXI",
            fundingType = "PES",
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
        ),
      ),
    )

    // When
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    val planCreationSchedules = getPlanCreationSchedules(prisonNumber)
    assertThat(planCreationSchedules)
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasStatus(PlanCreationStatus.SCHEDULED)
          .hasDeadlineDate(LocalDate.parse("2099-12-31"))
      }
  }

  /**
   * Scenario:
   * * Prisoner
   *   * No needs
   *   * Does not have an ELSP Creation schedule
   * A need is recorded (ALN Assessment indicating needs)
   * Prisoner enrols in a non-PES course
   *
   * Expectation is that a ELSP Creation Schedule is created, with a status of SCHEDULED and a deadline date of 2099-12-31.
   * A Plan Creation Schedule with a deadline of 2099-12-31 effectively means no deadline date.
   *
   * The prisoner has a need and is in education, though not a PES course. This means they can have an ELSP created if
   * they wish, but there are no contractural deadlines to do so.
   *
   * This scenario is essentially the prisoner in on a non-PES courses, and has needs indicated by an ALN Assessment.
   * Prisoner needs to be on a PES course and have an ALN Assessment indicating a need in order to create a
   * Plan Creation Schedule with a deadline.
   */
  @Test
  fun `should create plan creation schedule with no deadline date given a prisoner with an ALN Assessment need, and a non-PES course is started`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists with no needs, and has no ELSP Plan Creation Schedule
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
    )

    // An ALN Assessment exists for the prisoner, indicating they have a need
    aValidAlnAssessmentExists(
      prisonNumber = prisonNumber,
      hasNeed = true,
      screeningDate = today.minusWeeks(2),
    )

    // Prisoner enrols in a non-PES course after the ALN Assessment was recorded
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Retrofit",
            establishmentId = "BXI",
            fundingType = "DPS",
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
            learningStartDate = today,
          ),
        ),
      ),
    )

    // When
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    val planCreationSchedules = getPlanCreationSchedules(prisonNumber)
    assertThat(planCreationSchedules)
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasStatus(PlanCreationStatus.SCHEDULED)
          .hasDeadlineDate(LocalDate.parse("2099-12-31"))
      }
  }

  /**
   * Scenario:
   * * Prisoner
   *   * No needs
   *   * Does not have an ELSP Creation schedule
   * A need is recorded (ALN Assessment indicating needs)
   * Prisoner enrols in a PES course
   *
   * Expectation is that a ELSP Creation Schedule is created, with a status of SCHEDULED and a deadline date of
   * today + 5 working days. (NOT calendar days)
   *
   * The prisoner has a need and is in education on a PES course. This means they need to have an ELSP created by the
   * deadline.
   *
   * This scenario is essentially the prisoner in on a PES courses, and has needs indicated by an ALN Assessment.
   * Prisoner needs to be on a PES course and have an ALN Assessment indicating a need in order to create a
   * Plan Creation Schedule with a deadline.
   */
  @Test
  fun `should create plan creation schedule with a deadline date given a prisoner with an ALN Assessment need, and a PES course is started`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // a prisoner exists with no needs, and has no ELSP Plan Creation Schedule
    val prisonNumber = randomValidPrisonNumber()
    aPrisonerExists(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
    )

    // An ALN Assessment exists for the prisoner, indicating they have a need
    aValidAlnAssessmentExists(
      prisonNumber = prisonNumber,
      hasNeed = true,
      screeningDate = today.minusWeeks(2),
    )

    // Prisoner enrols in a non-PES course after the ALN Assessment was recorded
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            qualificationCode = "Metalwork",
            establishmentId = "BXI",
            fundingType = "PES",
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
            learningStartDate = today,
          ),
        ),
      ),
    )

    // When
    val educationStartMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = aValidEducationStatusUpdateAdditionalInformation(),
      description = "EDUCATION_STARTED",
    )
    sendCuriousEducationMessage(educationStartMessage)
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // Then
    val planCreationSchedules = getPlanCreationSchedules(prisonNumber)
    assertThat(planCreationSchedules)
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasStatus(PlanCreationStatus.SCHEDULED)
          .hasDeadlineDate(LocalDate.parse("2026-03-27")) // The test sets today as Saturday 2026-03-21. 5 working days from there is Friday 2026-03-27
      }
  }

  private fun sendPrisonerTransferMessage(prisonNumber: String, prisonId: String) {
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_RECEIVED_INTO_PRISON,
      additionalInformation = aValidPrisonerReceivedAdditionalInformation(
        prisonNumber = prisonNumber,
        prisonId = prisonId,
        reason = AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TRANSFERRED,
      ),
    )
    sendDomainEvent(sqsMessage)
  }

  private fun sendCuriousEducationMessage(sqsMessage: SqsMessage) {
    sendDomainEvent(sqsMessage)
  }
}

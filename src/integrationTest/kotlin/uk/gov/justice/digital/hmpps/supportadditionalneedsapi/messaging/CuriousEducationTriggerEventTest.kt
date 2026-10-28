package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousEducationCompletionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidEducationDto
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.aValidV2Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.PLAN_DEADLINE_DAYS_TO_ADD
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
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
    // person has a need but no ALN need
    aValidChallengeExists(prisonNumber)

    // Then
    val educationStartDate = LocalDate.of(2025, 10, 2)
    putInEducationAndValidate(prisonNumber, educationStartDate = educationStartDate)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    Assertions.assertThat(planCreationSchedule.createdAtPrison).isEqualTo("CFI")
    // Even though the education start date is greater than PES date and the person had a Challenge need
    // This person should have the in the future deadline date as per RR-2248
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
  }

  @Test
  fun `should process Curious Education domain event and mark the person as being in education with a deadline date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "CFI")
    // person has an ALN need
    createALNAssessmentMessage(prisonNumber, assessmentDate = LocalDate.now().minusMonths(5), hasNeed = true)

    // Then
    val educationStartDate = LocalDate.now().minusMonths(4)
    putInEducationAndValidate(prisonNumber, educationStartDate = educationStartDate)
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isEqualTo(educationStartDate)
    Assertions.assertThat(planCreationSchedule.createdAtPrison).isEqualTo("CFI")

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
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    // Even though the education start date is greater than PES date and the person had a Challenge need
    // This person should have the in the future deadline date as per RR-2248
    Assertions.assertThat(planCreationSchedule.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
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
  fun `ESWE-2425 - AC01 - stopping a non-PES course records the stop without changing schedules`() {
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    aValidReviewScheduleExists(prisonNumber)
    val originalPlanSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    val originalReviewSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    val scenario = givenPrisonerOnCourses(prisonNumber, stoppedCourseFundingType = "DPS", hasOtherActivePesCourse = false, hasOtherActiveNonPesCourse = false)
    val stoppedCourse = whenCourseIsStopped(scenario)
    thenStoppedCourseIsRecorded(scenario, stoppedCourse)

    Assertions.assertThat(planCreationScheduleRepository.findByPrisonNumber(prisonNumber))
      .usingRecursiveComparison().isEqualTo(originalPlanSchedule)
    Assertions.assertThat(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .usingRecursiveComparison().isEqualTo(originalReviewSchedule)
  }

  @Test
  fun `ESWE-2425 - AC02 - stopping the last PES course with an active ESP exempts the review schedule`() {
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)
    aValidReviewScheduleExists(prisonNumber)

    val scenario = givenPrisonerOnCourses(prisonNumber, stoppedCourseFundingType = "PES", hasOtherActivePesCourse = false, hasOtherActiveNonPesCourse = true)
    val stoppedCourse = whenCourseIsStopped(scenario)
    thenStoppedCourseIsRecorded(scenario, stoppedCourse)

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
    Assertions.assertThat(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).isNull()
  }

  @Test
  fun `ESWE-2425 - AC03 - stopping a PES course with an active ESP and another active PES course leaves scheduling unchanged`() {
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)
    aValidReviewScheduleExists(prisonNumber)
    val originalSchedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    val scenario = givenPrisonerOnCourses(prisonNumber, stoppedCourseFundingType = "PES", hasOtherActivePesCourse = true, hasOtherActiveNonPesCourse = false)
    val stoppedCourse = whenCourseIsStopped(scenario)
    thenStoppedCourseIsRecorded(scenario, stoppedCourse)

    Assertions.assertThat(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
      .usingRecursiveComparison().isEqualTo(originalSchedule)
    Assertions.assertThat(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).isNull()
  }

  @Test
  fun `ESWE-2425 - AC04 - stopping the last PES course with plan creation scheduled exempts the plan creation schedule`() {
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)

    val scenario = givenPrisonerOnCourses(prisonNumber, stoppedCourseFundingType = "PES", hasOtherActivePesCourse = false, hasOtherActiveNonPesCourse = false)
    val stoppedCourse = whenCourseIsStopped(scenario)
    thenStoppedCourseIsRecorded(scenario, stoppedCourse)

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
    Assertions.assertThat(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).isNull()
  }

  @Test
  fun `ESWE-2425 - AC05 - stopping a PES course with plan creation scheduled and another active PES course leaves scheduling unchanged`() {
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    val originalSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    val scenario = givenPrisonerOnCourses(prisonNumber, stoppedCourseFundingType = "PES", hasOtherActivePesCourse = true, hasOtherActiveNonPesCourse = false)
    val stoppedCourse = whenCourseIsStopped(scenario)
    thenStoppedCourseIsRecorded(scenario, stoppedCourse)

    Assertions.assertThat(planCreationScheduleRepository.findByPrisonNumber(prisonNumber))
      .usingRecursiveComparison().isEqualTo(originalSchedule)
    Assertions.assertThat(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).isNull()
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

  private val stopTestPrisonId = "CFI"
  private val stopTestCourseStartDate = LocalDate.now().minusMonths(2)

  private data class CourseStopScenario(
    val prisonNumber: String,
    val stoppedCourseFundingType: String,
    val hasOtherActivePesCourse: Boolean,
    val hasOtherActiveNonPesCourse: Boolean,
  )

  private data class StoppedCourse(
    val endDate: LocalDate,
    val curiousReference: UUID,
  )

  // Arrange: enrol the prisoner on the course that will be stopped and any other active PES or non-PES courses.
  private fun givenPrisonerOnCourses(
    prisonNumber: String,
    stoppedCourseFundingType: String,
    hasOtherActivePesCourse: Boolean,
    hasOtherActiveNonPesCourse: Boolean,
  ): CourseStopScenario {
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = stopTestPrisonId)
    prisonerInEducation(
      prisonNumber,
      learningStartDate = stopTestCourseStartDate,
      establishmentId = stopTestPrisonId,
      qualificationCode = "STOPPED",
      fundingType = stoppedCourseFundingType,
    )
    if (hasOtherActivePesCourse) {
      prisonerInEducation(
        prisonNumber,
        learningStartDate = stopTestCourseStartDate,
        establishmentId = stopTestPrisonId,
        qualificationCode = "ACTIVE",
      )
    }
    if (hasOtherActiveNonPesCourse) {
      prisonerInEducation(
        prisonNumber,
        learningStartDate = stopTestCourseStartDate,
        establishmentId = stopTestPrisonId,
        qualificationCode = "ACTIVE_NON_PES",
        fundingType = "DPS",
      )
    }
    return CourseStopScenario(prisonNumber, stoppedCourseFundingType, hasOtherActivePesCourse, hasOtherActiveNonPesCourse)
  }

  // Act: Curious reports the course as ended, and the EDUCATION_STOPPED message is sent
  private fun whenCourseIsStopped(scenario: CourseStopScenario): StoppedCourse {
    val endDate = LocalDate.now().minusDays(1)
    val curiousReference = UUID.randomUUID()
    val courses = mutableListOf(
      aValidV2Education(
        prn = scenario.prisonNumber,
        establishmentId = stopTestPrisonId,
        qualificationCode = "STOPPED",
        learningStartDate = stopTestCourseStartDate,
        learningActualEndDate = endDate,
        fundingType = scenario.stoppedCourseFundingType,
        completionStatus = CuriousEducationCompletionStatus.WITHDRAWN,
      ),
    )
    if (scenario.hasOtherActivePesCourse) {
      courses += aValidV2Education(
        prn = scenario.prisonNumber,
        establishmentId = stopTestPrisonId,
        qualificationCode = "ACTIVE",
        learningStartDate = stopTestCourseStartDate,
      )
    }
    if (scenario.hasOtherActiveNonPesCourse) {
      courses += aValidV2Education(
        prn = scenario.prisonNumber,
        establishmentId = stopTestPrisonId,
        qualificationCode = "ACTIVE_NON_PES",
        learningStartDate = stopTestCourseStartDate,
        fundingType = "DPS",
      )
    }
    stubGetCurious2Education(scenario.prisonNumber, aValidEducationDto(courses))

    sendCuriousEducationMessage(
      aValidHmppsDomainEventsSqsMessage(
        prisonNumber = scenario.prisonNumber,
        eventType = EventType.EDUCATION_STATUS_UPDATE,
        additionalInformation = aValidEducationStatusUpdateAdditionalInformation(curiousReference),
        description = "EDUCATION_STOPPED",
      ),
    )
    return StoppedCourse(endDate, curiousReference)
  }

  // Assert: the stop is recorded in the education enrolment table
  private fun thenStoppedCourseIsRecorded(scenario: CourseStopScenario, stoppedCourse: StoppedCourse) {
    // The stop message is consumed asynchronously from an SQS queue, so wait for the enrolment transaction to commit
    // before the calling test inspects scheduling.
    await.untilAsserted {
      val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(scenario.prisonNumber)
      val expectedEnrolmentCount = 1 + listOf(scenario.hasOtherActivePesCourse, scenario.hasOtherActiveNonPesCourse).count { it }
      Assertions.assertThat(enrolments).hasSize(expectedEnrolmentCount)
      val stopped = enrolments.single { it.qualificationCode == "STOPPED" }
      Assertions.assertThat(stopped.endDate).isEqualTo(stoppedCourse.endDate)
      Assertions.assertThat(stopped.fundingType).isEqualTo(scenario.stoppedCourseFundingType)
      Assertions.assertThat(stopped.completionStatus).isEqualTo(CuriousEducationCompletionStatus.WITHDRAWN.value)
      Assertions.assertThat(stopped.lastCuriousReference).isEqualTo(stoppedCourse.curiousReference)
      if (scenario.hasOtherActivePesCourse) {
        Assertions.assertThat(enrolments.single { it.qualificationCode == "ACTIVE" }.endDate).isNull()
      }
      if (scenario.hasOtherActiveNonPesCourse) {
        val activeNonPesCourse = enrolments.single { it.qualificationCode == "ACTIVE_NON_PES" }
        Assertions.assertThat(activeNonPesCourse.endDate).isNull()
        Assertions.assertThat(activeNonPesCourse.fundingType).isEqualTo("DPS")
      }
    }
  }

  private fun processEducationAndValidatePrisonerNotInEducation(
    prisonNumber: String,
    fundingType: String = "PES",
    educationStartDate: LocalDate = LocalDate.now().minusMonths(5),
  ) {
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          learningStartDate = educationStartDate,
          fundingType = fundingType,
        ),
      ),
    )

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
      val isInEducation = educationService.hasActiveEducationEnrollment(prisonNumber)
      Assertions.assertThat(isInEducation).isFalse()
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
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          learningStartDate = educationStartDate,
          fundingType = fundingType,
          completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
        ),
      ),
    )

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
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        listOf(
          aValidV2Education(
            prn = prisonNumber,
            fundingType = fundingType,
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
          aValidV2Education(
            prn = prisonNumber,
            fundingType = fundingType,
            completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
          ),
        ),
      ),
    )

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
      val isInEducation = educationService.hasActiveEducationEnrollment(prisonNumber)
      Assertions.assertThat(isInEducation).isTrue()
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
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          learningStartDate = LocalDate.parse("2025-01-01"),
          learningPlannedEndDate = LocalDate.parse("2025-01-31"),
          learningActualEndDate = LocalDate.parse("2025-08-01"),
          completionStatus = CuriousEducationCompletionStatus.COMPLETED,
        ),
      ),
    )

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
      val isInEducation = educationService.hasActiveEducationEnrollment(prisonNumber)
      Assertions.assertThat(isInEducation).isFalse()
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
    stubGetCurious2Education(
      prisonNumber,
      aValidEducationDto(
        aValidV2Education(
          prn = prisonNumber,
          learningStartDate = LocalDate.parse("2025-01-01"),
          learningPlannedEndDate = LocalDate.parse("2025-01-31"),
          learningActualEndDate = null,
          completionStatus = CuriousEducationCompletionStatus.IN_PROGRESS,
        ),
      ),
    )

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
      val isInEducation = educationService.hasActiveEducationEnrollment(prisonNumber)
      Assertions.assertThat(isInEducation).isTrue()
    } matches { it != null }

    // also check the education enrolment(s) have been saved
    val enrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    Assertions.assertThat(enrolments).hasSize(1)
    Assertions.assertThat(enrolments[0].endDate).isNull()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[2].event).isEqualTo(TimelineEventType.CURIOUS_EDUCATION_TRIGGER)
    Assertions.assertThat(timelineEntries[2].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  private fun sendCuriousEducationMessage(sqsMessage: SqsMessage) {
    sendDomainEvent(sqsMessage)
  }
}

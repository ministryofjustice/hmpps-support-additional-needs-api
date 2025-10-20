package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.PLAN_DEADLINE_DAYS_TO_ADD
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate
import java.util.*

@Isolated
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CuriousALNTriggerEventTest : IntegrationTestBase() {

  @BeforeAll
  fun beforeAll() {
    stubForBankHoliday()
  }

  @Test
  fun `should process Curious ALN domain event and mark the person as having an ALN need`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)

    // Then
    Assertions.assertThat(needService.hasALNScreenerNeed(prisonNumber)).isTrue()
    Assertions.assertThat(needService.hasNeed(prisonNumber)).isTrue()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_ASSESSMENT_TRIGGER)
    Assertions.assertThat(timelineEntries[0].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  @Test
  fun `should process Curious ALN domain event where assessment date greater than education start date, mark the person as having an ALN need and create schedule with no deadline date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    // education with a date before ALN assessment
    prisonerInEducation(prisonNumber, learningStartDate = LocalDate.now().minusDays(1))

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true, assessmentDate = LocalDate.now())

    // Then
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    Assertions.assertThat(planCreationSchedule).isNotNull
    Assertions.assertThat(planCreationSchedule!!.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    Assertions.assertThat(planCreationSchedule.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious ALN domain event where assessment date = education start date, mark the person as having an ALN need and create schedule with a KPI deadline date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    val learningStartDate = LocalDate.now()
    // education with a date the same as ALN assessment
    prisonerInEducation(prisonNumber, learningStartDate = learningStartDate)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true, assessmentDate = LocalDate.now())

    // Then
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    Assertions.assertThat(planCreationSchedule).isNotNull
    Assertions.assertThat(planCreationSchedule!!.deadlineDate)
      .isEqualTo(planDeadlineDateBasedOnPESContractDate(learningStartDate))
    Assertions.assertThat(planCreationSchedule.createdAtPrison).isEqualTo("KMI")
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isEqualTo(learningStartDate)
    Assertions.assertThat(planCreationSchedule.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious ALN domain event where assessment date less than education start date, mark the person as having an ALN need and create schedule with a KPI deadline date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    val learningStartDate = LocalDate.now()
    // education with a date the same as ALN assessment
    prisonerInEducation(prisonNumber, learningStartDate = learningStartDate)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(
      prisonNumber,
      curiousReference,
      hasNeed = true,
      assessmentDate = LocalDate.now().minusDays(3),
    )

    // Then
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    Assertions.assertThat(planCreationSchedule).isNotNull
    Assertions.assertThat(planCreationSchedule!!.deadlineDate)
      .isEqualTo(planDeadlineDateBasedOnPESContractDate(learningStartDate))
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isEqualTo(learningStartDate)
    Assertions.assertThat(planCreationSchedule.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  private fun planDeadlineDateBasedOnPESContractDate(learningStartDate: LocalDate): LocalDate = if (LocalDate.now() < pesContractDate) {
    workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, pesContractDate)
  } else {
    workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, learningStartDate)
  }

  @Test
  fun `should process Curious ALN domain event, mark the person as having an ALN need and has plan create review schedule with the non KPI date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber = prisonNumber, learningStartDate = LocalDate.now())
    anElSPExists(prisonNumber)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(
      prisonNumber,
      curiousReference,
      hasNeed = true,
      assessmentDate = LocalDate.now().plusDays(1),
    )

    // Then
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    Assertions.assertThat(reviewScheduleEntity).isNotNull
    Assertions.assertThat(reviewScheduleEntity!!.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
    Assertions.assertThat(reviewScheduleEntity.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious ALN domain event, mark the person as having an ALN need and has plan create review schedule with the KPI date`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    val educationStartDate = LocalDate.now()
    prisonerInEducation(prisonNumber = prisonNumber, learningStartDate = educationStartDate)
    anElSPExists(prisonNumber)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true, assessmentDate = educationStartDate)

    // Then
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    Assertions.assertThat(reviewScheduleEntity).isNotNull

    // This date is going to be the latter of today + REVIEW_DEADLINE_DAYS_TO_ADD (working days) or
    // pesContractDate + REVIEW_DEADLINE_DAYS_TO_ADD (working days)
    val deadlineDate = maxOf(
      workingDayService.getNextWorkingDayNDaysFromDate(reviewConfig.reviewDeadlineDaysToAdd, educationStartDate),
      workingDayService.getNextWorkingDayNDaysFromDate(reviewConfig.reviewDeadlineDaysToAdd, pesContractDate),
    )

    Assertions.assertThat(reviewScheduleEntity!!.deadlineDate).isEqualTo(deadlineDate)
    Assertions.assertThat(reviewScheduleEntity.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious ALN domain event and mark the person as NOT having an ALN need`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    // when
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = false)
    // then
    Assertions.assertThat(needService.hasALNScreenerNeed(prisonNumber)).isFalse()
    Assertions.assertThat(needService.hasNeed(prisonNumber)).isFalse()

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    Assertions.assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.CURIOUS_ASSESSMENT_TRIGGER)
    Assertions.assertThat(timelineEntries[0].additionalInfo).isEqualTo("curiousReference:$curiousReference")
  }

  @Test
  fun `should set up a review schedule as having an non KPI deadline date`() {
    // This is an obscure scenario where somehow a person has an ELSP created but doesn't have a need.
    // maybe their need was temporary in the past.
    // Then they are in education and they have a need added.
    // this should schedule a review BUT not will set the deadline date to be in the distant future

    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber = prisonNumber, learningStartDate = LocalDate.now())
    anElSPExists(prisonNumber)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true, assessmentDate = LocalDate.now().plusDays(1))

    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewScheduleEntity!!.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
    Assertions.assertThat(reviewScheduleEntity.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }

  // The person has a challenge and an aln and a plan creation schedule then has an aln need removed via a message
  // test that the person still has a need and that the plan creation schedule remains SCHEDULED
  @Test
  fun `person has a san need then gets an ALN need then has that need removed - the person should still have a SCHEDULED PCS`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber)
    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)
    // person has a challenge
    aValidChallengeExists(prisonNumber)
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = false)

    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(planCreationSchedule).isNotNull
    Assertions.assertThat(planCreationSchedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  // The person has a challenge and an aln and a Review schedule then has an aln need removed via a message
  // test that the person still has a need and that the Review schedule remains SCHEDULED
  @Test
  fun `person has a san need then gets an ALN need then has that need removed - the person should still have a SCHEDULED RS`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber)
    anElSPExists(prisonNumber)
    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)
    // person has a challenge
    aValidChallengeExists(prisonNumber)
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = false)

    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(reviewScheduleEntity!!.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
  }
}

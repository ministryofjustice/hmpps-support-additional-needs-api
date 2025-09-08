package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidEducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
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
  fun `should process Curious ALN domain event, mark the person as having an ALN need and create schedule`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber)
    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)

    // Then
    val planCreationSchedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)

    Assertions.assertThat(planCreationSchedule).isNotNull
    Assertions.assertThat(planCreationSchedule!!.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
    Assertions.assertThat(planCreationSchedule.earliestStartDate).isNull()
    Assertions.assertThat(planCreationSchedule.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
  }

  @Test
  fun `should process Curious ALN domain event, mark the person as having an ALN need and has plan create review schedule`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber)
    anElSPExists(prisonNumber)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)

    // Then
    val reviewScheduleEntity = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    Assertions.assertThat(reviewScheduleEntity).isNotNull
    Assertions.assertThat(reviewScheduleEntity!!.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
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
  fun `should set up a review schedule as having a null deadline date`() {
    // This is an obscure scenario where somehow a person has an ELSP created but doesn't have a need.
    // maybe their need was temporary in the past.
    // Then they are in education and they have a need added.
    // this should schedule a review BUT not set a deadline date.

    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    prisonerInEducation(prisonNumber)
    anElSPExists(prisonNumber)

    // When
    val curiousReference = UUID.randomUUID()
    createALNAssessmentMessage(prisonNumber, curiousReference, hasNeed = true)

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

  private fun createALNAssessmentMessage(prisonNumber: String, curiousReference: UUID, hasNeed: Boolean = true) {
    stubGetCurious2LearnerAssessments(prisonNumber, createTestALNAssessment(prisonNumber, hasNeed = hasNeed))
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
      additionalInformation = aValidEducationALNAssessmentUpdateAdditionalInformation(curiousReference),
      description = "ASSESSMENT_COMPLETED",
    )
    sendCuriousALNMessage(sqsMessage)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val alnAssessment = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      if (hasNeed) {
        Assertions.assertThat(alnAssessment!!.hasNeed).isTrue()
      } else {
        Assertions.assertThat(alnAssessment!!.hasNeed).isFalse()
      }
      Assertions.assertThat(alnAssessment.curiousReference).isEqualTo(curiousReference)
      Assertions.assertThat(alnAssessment.screeningDate).isEqualTo(LocalDate.of(2025, 1, 28))
    } matches { it != null }
  }

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

  private fun sendCuriousALNMessage(sqsMessage: SqsMessage) {
    sendDomainEvent(sqsMessage)
  }
}

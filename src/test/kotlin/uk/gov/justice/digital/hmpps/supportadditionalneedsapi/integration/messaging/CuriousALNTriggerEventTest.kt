package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidEducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import java.time.LocalDate
import java.util.*

@Isolated
class CuriousALNTriggerEventTest : IntegrationTestBase() {

  @Test
  fun `should process Curious ALN domain event and mark the person as having an ALN need`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    stubGetCurious2LearnerAssessments(prisonNumber, createTestALNAssessment(prisonNumber))
    // When
    val curiousReference = UUID.randomUUID()
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
      Assertions.assertThat(alnAssessment!!.hasNeed).isTrue()
      Assertions.assertThat(alnAssessment.curiousReference).isEqualTo(curiousReference)
      Assertions.assertThat(alnAssessment.screeningDate).isEqualTo(LocalDate.of(2025, 1, 28))
    } matches { it != null }

    Assertions.assertThat(needService.hasALNScreenerNeed(prisonNumber)).isTrue()
    Assertions.assertThat(needService.hasNeed(prisonNumber)).isTrue()
  }

  @Test
  fun `should process Curious ALN domain event and mark the person as NOT having an ALN need`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    stubGetCurious2LearnerAssessments(prisonNumber, createTestALNAssessment(prisonNumber = prisonNumber, hasNeed = false))
    // When
    val curiousReference = UUID.randomUUID()
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
      Assertions.assertThat(alnAssessment!!.hasNeed).isFalse()
      Assertions.assertThat(alnAssessment.curiousReference).isEqualTo(curiousReference)
      Assertions.assertThat(alnAssessment.screeningDate).isEqualTo(LocalDate.of(2025, 1, 28))
    } matches { it != null }

    Assertions.assertThat(needService.hasALNScreenerNeed(prisonNumber)).isFalse()
    Assertions.assertThat(needService.hasNeed(prisonNumber)).isFalse()
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

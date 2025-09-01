package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidPrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TRANSFERRED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@Isolated
class PrisonerTransferEventTest : IntegrationTestBase() {

  @Test
  fun `should process prisoner transfer event and change the status of the plan creation schedule to EXEMPT_PRISONER_TRANSFER`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    prisonerInEducation(prisonNumber)

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_TRANSFER)
    //prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isFalse()
  }

  @Test
  fun `should process prisoner transfer event and change the status of the review schedule to EXEMPT_PRISONER_TRANSFER`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)
    prisonerInEducation(prisonNumber)

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_TRANSFER)
    //prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isFalse()
  }

  private fun sendPrisonerTransferMessage(prisonNumber: String) {
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_RECEIVED_INTO_PRISON,
      additionalInformation = aValidPrisonerReceivedAdditionalInformation(
        prisonNumber = prisonNumber,
        reason = TRANSFERRED,
      ),
    )
    sendDomainEvent(sqsMessage)
  }
}

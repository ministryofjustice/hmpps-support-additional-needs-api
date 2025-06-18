package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidPrisonerMergedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@Isolated
class PrisonerMergedEventTest : IntegrationTestBase() {

  @Test
  fun `should process prisoner merge event setting plan creation schedule to exempt due to prisoner merge`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val newPrisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    // When
    sendPrisonerMergeMessage(prisonNumber, newPrisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_MERGE)
  }

  @Test
  fun `should process prisoner merge event setting review schedule to exempt due to prisoner merge`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val newPrisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)

    // When
    sendPrisonerMergeMessage(prisonNumber, newPrisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_MERGE)
  }

  private fun sendPrisonerMergeMessage(prisonNumber: String, newPrisonNumber: String) {
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_MERGED,
      additionalInformation = aValidPrisonerMergedAdditionalInformation(
        prisonNumber = newPrisonNumber,
        removedNomsNumber = prisonNumber,
      ),
    )
    sendDomainEvent(sqsMessage)
  }
}

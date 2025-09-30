package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import org.assertj.core.api.Assertions
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidPrisonerReleasedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReleasedAdditionalInformation.Reason.RELEASED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

@Isolated
class PrisonerReleasedEventTest : IntegrationTestBase() {

  @Test
  fun `should process prisoner release event setting plan creation schedule to exempt due to prisoner release`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)

    // When
    sendPrisonerReleaseMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_RELEASE)
  }

  @Test
  fun `should process prisoner release event setting review schedule to exempt due to prisoner release`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)

    // When
    sendPrisonerReleaseMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_RELEASE)
  }

  private fun sendPrisonerReleaseMessage(prisonNumber: String) {
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_RELEASED_FROM_PRISON,
      additionalInformation = aValidPrisonerReleasedAdditionalInformation(
        prisonNumber = prisonNumber,
        reason = RELEASED,
      ),
    )

    // When
    sendDomainEvent(sqsMessage)
  }
}

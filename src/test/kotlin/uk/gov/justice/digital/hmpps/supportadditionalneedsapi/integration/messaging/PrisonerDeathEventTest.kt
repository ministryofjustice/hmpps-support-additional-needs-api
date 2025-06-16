package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import mu.KotlinLogging
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.parallel.Isolated
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidPrisonerReleasedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

private val log = KotlinLogging.logger {}

@Isolated
class PrisonerDeathEventTest : IntegrationTestBase() {

  @Test
  fun `should process prisoner death event`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()

    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_RELEASED_FROM_PRISON,
      additionalInformation = aValidPrisonerReleasedAdditionalInformation(
        prisonNumber = prisonNumber,
        nomisMovementReasonCode = "DEC",
      ),
    )

    // When
    sendDomainEvent(sqsMessage)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    // TODO() add asserts once the code that processes these messages is finished.
  }
}

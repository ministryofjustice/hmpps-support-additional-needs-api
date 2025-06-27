package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.messaging

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue

private val log = KotlinLogging.logger {}

@Isolated
class PrisonerDeathEventTest : IntegrationTestBase() {

  @Test
  fun `should process prisoner death event setting plan creation schedule to exempt due to prisoner death`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    // When
    sendPrisonerDeathMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_DEATH)

    val history = planCreationScheduleHistoryRepository.findAllByPrisonNumberOrderByVersionAsc(prisonNumber)
    assertThat(history).hasSize(2)
    assertThat(history[0].version).isEqualTo(0)
    assertThat(history[1].version).isEqualTo(1)
  }

  @Test
  fun `should process prisoner death event setting review schedule to exempt due to prisoner death`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)
    // When
    sendPrisonerDeathMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_DEATH)
  }

  private fun sendPrisonerDeathMessage(prisonNumber: String) {
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.PRISONER_RELEASED_FROM_PRISON,
      additionalInformation = aValidPrisonerReleasedAdditionalInformation(
        prisonNumber = prisonNumber,
        nomisMovementReasonCode = "DEC",
      ),
    )
    sendDomainEvent(sqsMessage)
  }
}

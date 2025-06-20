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
    val prisonNumber2 = randomValidPrisonNumber()
    aValidPlanCreationScheduleExists(prisonNumber)
    aValidPlanCreationScheduleExists(prisonNumber2)
    // When
    sendPrisonerDeathMessage(prisonNumber)
    sendPrisonerDeathMessage(prisonNumber2)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_DEATH)

    val history = planCreationScheduleHistoryRepository.findAllByPrisonNumber(prisonNumber)
    assertThat(history).hasSize(2)

    val prisoner1Schedules = planCreationScheduleService.getSchedules(prisonNumber)
    val prisoner2Schedules = planCreationScheduleService.getSchedules(prisonNumber2)

    assertThat(prisoner1Schedules.planCreationSchedules[0].version).isEqualTo(1)
    assertThat(prisoner1Schedules.planCreationSchedules[1].version).isEqualTo(2)
    assertThat(prisoner2Schedules.planCreationSchedules[0].version).isEqualTo(1)
    assertThat(prisoner2Schedules.planCreationSchedules[1].version).isEqualTo(2)
  }

  @Test
  fun `should process prisoner death event setting review schedule to exempt due to prisoner death`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber)
    val prisonNumber2 = randomValidPrisonNumber()
    aValidReviewScheduleExists(prisonNumber2)
    // When
    sendPrisonerDeathMessage(prisonNumber)
    sendPrisonerDeathMessage(prisonNumber2)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_DEATH)

    val prisoner1Schedules = reviewScheduleService.getSchedules(prisonNumber)
    val prisoner2Schedules = reviewScheduleService.getSchedules(prisonNumber2)

    assertThat(prisoner1Schedules.reviewSchedules[0].version).isEqualTo(1)
    assertThat(prisoner1Schedules.reviewSchedules[1].version).isEqualTo(2)
    assertThat(prisoner2Schedules.reviewSchedules[0].version).isEqualTo(1)
    assertThat(prisoner2Schedules.reviewSchedules[1].version).isEqualTo(2)
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

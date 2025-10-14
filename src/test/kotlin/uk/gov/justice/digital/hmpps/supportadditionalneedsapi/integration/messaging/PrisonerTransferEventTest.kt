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
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "BXI")
    aValidPlanCreationScheduleExists(prisonNumber)
    // in education at a different prison CFI to their current prison BXI so should end all
    // non BXI education.
    prisonerInEducation(prisonNumber, establishmentId = "CFI")

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.EXEMPT_PRISONER_TRANSFER)
    // prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isFalse()
  }

  @Test
  fun `should process prisoner transfer event and NOT change the status of the plan creation schedule to EXEMPT_PRISONER_TRANSFER`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "BXI")
    aValidPlanCreationScheduleExists(prisonNumber)
    // in education at the same prison already so should not be exempt
    prisonerInEducation(prisonNumber, establishmentId = "BXI")

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(PlanCreationScheduleStatus.SCHEDULED)
    // prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isTrue()
  }

  @Test
  fun `should process prisoner transfer event and change the status of the review schedule to EXEMPT_PRISONER_TRANSFER`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "BXI")
    aValidReviewScheduleExists(prisonNumber)
    // in education at a different prison CFI to their current prison BXI so should end all
    // non BXI education.
    prisonerInEducation(prisonNumber, establishmentId = "CFI")

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.EXEMPT_PRISONER_TRANSFER)
    // prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isFalse()
  }

  @Test
  fun `should process prisoner transfer event and NOT change the status of the review schedule to EXEMPT_PRISONER_TRANSFER`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(prisonNumber, prisonId = "BXI")
    aValidReviewScheduleExists(prisonNumber)
    // in education at the same prison already
    prisonerInEducation(prisonNumber, establishmentId = "BXI")

    // When
    sendPrisonerTransferMessage(prisonNumber)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }

    val schedule = reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(schedule!!.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
    // prisoner should no longer be in education
    val educationEntity = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    Assertions.assertThat(educationEntity?.inEducation).isTrue()
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

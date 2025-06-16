package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerMergedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReleasedAdditionalInformation

private val log = KotlinLogging.logger {}

/**
 * Service class that processes SQS events. For each type the [InboundEvent]'s `additionalInformation` property is
 * deserialized into it's corresponding type, and then the [InboundEvent] and it's [AdditionalInformation] are sent
 * to the relevant service specific to that event type.
 */
@Service
class InboundEventsService(
  private val mapper: ObjectMapper,
) {

  fun process(inboundEvent: InboundEvent) = with(inboundEvent) {
    log.info { "Processing inbound event $eventType" }

    when (eventType) {
      EventType.PRISONER_RECEIVED_INTO_PRISON -> {
        val additionalInformation = eventAdditionalInformation<PrisonerReceivedAdditionalInformation>(inboundEvent)
        log.info("Received inbound event $eventType with additional information: $additionalInformation")
      }
      EventType.PRISONER_RELEASED_FROM_PRISON -> {
        val additionalInformation = eventAdditionalInformation<PrisonerReleasedAdditionalInformation>(inboundEvent)
        log.info("Received inbound event $eventType with additional information: $additionalInformation")
      }
      EventType.PRISONER_MERGED -> {
        val additionalInformation = eventAdditionalInformation<PrisonerMergedAdditionalInformation>(inboundEvent)
        log.info("Received inbound event $eventType with additional information: $additionalInformation")
      }
    }
  }

  private inline fun <reified T : AdditionalInformation> eventAdditionalInformation(inboundEvent: InboundEvent): T = this.mapper.readValue(inboundEvent.additionalInformation, T::class.java)
}

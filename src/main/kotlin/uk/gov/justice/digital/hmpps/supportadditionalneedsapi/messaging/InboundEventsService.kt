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

  private val eventTypeToClassMap = mapOf(
    EventType.PRISONER_RECEIVED_INTO_PRISON to PrisonerReceivedAdditionalInformation::class.java,
    EventType.PRISONER_RELEASED_FROM_PRISON to PrisonerReleasedAdditionalInformation::class.java,
    EventType.PRISONER_MERGED to PrisonerMergedAdditionalInformation::class.java,
  )

  fun process(inboundEvent: InboundEvent) {
    log.info { "Processing inbound event ${inboundEvent.eventType}" }

    val additionalInformation = eventTypeToClassMap[inboundEvent.eventType]

    if (additionalInformation == null) {
      log.warn { "No handler defined for eventType=${inboundEvent.eventType}" }
      return
    }

    val info = mapper.treeToValue(inboundEvent.additionalInformation, additionalInformation)
    log.info("Received inbound event ${inboundEvent.eventType} with additional information: $info")
  }
}

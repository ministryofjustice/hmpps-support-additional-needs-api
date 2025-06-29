package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import io.awspring.cloud.sqs.annotation.SqsListener
import mu.KotlinLogging
import org.springframework.stereotype.Component

private val log = KotlinLogging.logger {}

private const val NOTIFICATION = "Notification"

/**
 * Simple SQS Listener class that sends all received "Notification" messages to the [InboundEventsService] for further
 * processing.
 */
@Component
class InboundEventsListener(
  private val mapper: ObjectMapper,
  private val inboundEventsService: InboundEventsService,
) {

  @SqsListener("supportadditionalneeds", factory = "hmppsQueueContainerFactoryProxy")
  internal fun onMessage(sqsMessage: SqsMessage) {
    log.debug { "Inbound event message: ${sqsMessage.Type}" }

    when (sqsMessage.Type) {
      NOTIFICATION -> inboundEventsService.process(mapper.readValue(sqsMessage.Message, InboundEvent::class.java))
      else -> log.info { "Unrecognised message type: ${sqsMessage.Type}" }
    }
  }
}

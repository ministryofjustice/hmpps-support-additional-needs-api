package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.sns.model.PublishRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.ServiceProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import uk.gov.justice.hmpps.sqs.eventTypeMessageAttributes
import java.net.URI
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

private val log = KotlinLogging.logger {}

@Component
class EventPublisher(
  private val hmppsQueueService: HmppsQueueService,
  private val objectMapper: ObjectMapper,
  private val serviceProperties: ServiceProperties,
) {

  internal val eventTopic by lazy { hmppsQueueService.findByTopicId("domainevents") as HmppsTopic }

  fun createAndPublishPlanCreationSchedule(prisonNumber: String, occurredAt: Instant = Instant.now()) {
    log.info { "Publishing plan creation schedule for prisoner [$prisonNumber]" }
    createAndPublishEvent(
      prisonNumber = prisonNumber,
      occurredAt = occurredAt,
      eventType = "san.plan-creation-schedule.updated",
      description = "A Support for Additional Needs plan creation schedule created or amended",
      detailPath = "profile/{prisonerNumber}/plan-creation-schedule",
    )
  }

  fun createAndPublishReviewScheduleEvent(prisonNumber: String) {
    log.info { "Publishing review schedule for prisoner [$prisonNumber]" }
    createAndPublishEvent(
      prisonNumber = prisonNumber,
      occurredAt = Instant.now(),
      eventType = "san.review-schedule.updated",
      description = "A prisoner learning plan review schedule created or amended",
      detailPath = "profile/{prisonerNumber}/reviews/review-schedules",
    )
  }

  private fun publishEvent(event: HmppsDomainEvent) {
    log.info("Publishing event of type ${event.eventType} for person reference ${event.personReference.identifiers}")
    eventTopic.snsClient.publish(
      PublishRequest.builder()
        .topicArn(eventTopic.arn)
        .message(objectMapper.writeValueAsString(event))
        .eventTypeMessageAttributes(event.eventType)
        .build()
        .also { log.info("Published event $event to outbound topic") },
    ).get()
  }

  private fun createAndPublishEvent(
    prisonNumber: String,
    occurredAt: Instant,
    eventType: String,
    description: String,
    detailPath: String,
  ) {
    val event = HmppsDomainEvent(
      eventType = eventType,
      description = description,
      detailUrl = constructDetailUrl(detailPath, prisonNumber),
      occurredAt = occurredAt.atZone(ZoneId.of("Europe/London")).toLocalDateTime(),
      personReference = PersonReference(identifiers = listOf(Identifier("NOMS", prisonNumber))),
    )
    publishEvent(event)
  }

  private fun constructDetailUrl(detailPath: String, prisonerNumber: String): String {
    // Replace placeholder "{prisonerNumber}" with the actual prisonerNumber
    val updatedPath = detailPath.replace("{prisonerNumber}", prisonerNumber)
    return URI.create("${serviceProperties.baseUrl}/$updatedPath").toString()
  }

  data class HmppsDomainEvent(
    val version: Int = 1,
    val eventType: String,
    val description: String,
    val detailUrl: String,
    val occurredAt: LocalDateTime,
    val personReference: PersonReference,
  )

  data class PersonReference(
    val identifiers: List<Identifier>,
  )

  data class Identifier(
    val type: String,
    val value: String,
  )
}

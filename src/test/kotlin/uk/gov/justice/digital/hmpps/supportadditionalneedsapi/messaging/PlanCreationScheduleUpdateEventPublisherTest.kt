package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import software.amazon.awssdk.services.sns.SnsAsyncClient
import software.amazon.awssdk.services.sns.model.MessageAttributeValue
import software.amazon.awssdk.services.sns.model.PublishRequest
import software.amazon.awssdk.services.sns.model.PublishResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.ServiceProperties
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.HmppsTopic
import java.time.Instant
import java.time.ZoneId
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletableFuture.completedFuture

class PlanCreationScheduleUpdateEventPublisherTest {
  private val hmppsQueueService: HmppsQueueService = mock()
  private val snsClient: SnsAsyncClient = mock()
  private val objectMapper: ObjectMapper = mock()
  private val serviceProperties = ServiceProperties(baseUrl = "http://localhost:8080")
  private val service = EventPublisher(hmppsQueueService, objectMapper, serviceProperties)

  @Test
  fun `publish creates to plan creation schedule update event`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicUrn", snsClient))

    val publishResponse = PublishResponse.builder().messageId("123").build()
    val completableFuture = CompletableFuture<PublishResponse>()
    completableFuture.complete(publishResponse)
    val occurredAt = Instant.now()

    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(publishResponse))
    service.createAndPublishPlanCreationSchedule("A1234AC", occurredAt)
    verify(objectMapper).writeValueAsString(
      check<EventPublisher.HmppsDomainEvent> {
        assertThat(it).isEqualTo(
          EventPublisher.HmppsDomainEvent(
            eventType = "san.plan-creation-schedule.updated",
            detailUrl = "http://localhost:8080/profile/A1234AC/plan-creation-schedule",
            description = "A Support for Additional Needs plan creation schedule created or amended",
            occurredAt = occurredAt
              .atZone(ZoneId.of("Europe/London")).toLocalDateTime(),
            personReference = EventPublisher.PersonReference(
              identifiers = listOf(
                EventPublisher.Identifier(
                  "NOMS",
                  "A1234AC",
                ),
              ),
            ),
          ),
        )
      },
    )
  }

  @Test
  fun `send event sends to the sns client`() {
    whenever(objectMapper.writeValueAsString(any())).thenReturn("messageAsJson")
    whenever(hmppsQueueService.findByTopicId("domainevents")).thenReturn(HmppsTopic("id", "topicArn", snsClient))
    val publishResponse = mock<PublishResponse>()
    whenever(snsClient.publish(any<PublishRequest>())).thenReturn(completedFuture(publishResponse))

    service.createAndPublishPlanCreationSchedule("A1234AC")
    verify(snsClient).publish(
      PublishRequest.builder().message("messageAsJson")
        .topicArn("topicArn")
        .messageAttributes(
          mapOf(
            "eventType" to MessageAttributeValue.builder().dataType("String")
              .stringValue("san.plan-creation-schedule.updated")
              .build(),
          ),
        )
        .build(),
    )
  }
}

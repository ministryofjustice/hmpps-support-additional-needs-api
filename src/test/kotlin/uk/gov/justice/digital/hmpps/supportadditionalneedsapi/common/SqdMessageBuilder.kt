package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common

import com.fasterxml.jackson.databind.ObjectMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

fun aValidHmppsDomainEventsSqsMessage(
  prisonNumber: String = randomValidPrisonNumber(),
  eventType: EventType = EventType.PRISONER_RECEIVED_INTO_PRISON,
  occurredAt: Instant = Instant.now().minusSeconds(10),
  publishedAt: Instant = Instant.now(),
  description: String = "A prisoner has been received into prison",
  version: String = "1.0",
  removedNomsNumber: String = "",
  additionalInformation: AdditionalInformation =
    when (eventType) {
      EventType.PRISONER_RECEIVED_INTO_PRISON -> aValidPrisonerReceivedAdditionalInformation(prisonNumber)
      EventType.PRISONER_RELEASED_FROM_PRISON -> aValidPrisonerReleasedAdditionalInformation(prisonNumber)
      EventType.PRISONER_MERGED -> aValidPrisonerMergedAdditionalInformation(prisonNumber, removedNomsNumber)
      EventType.EDUCATION_STATUS_UPDATE -> TODO()
      EventType.EDUCATION_ALN_ASSESSMENT_UPDATE -> TODO()
    },
): SqsMessage = SqsMessage(
  Type = "Notification",
  Message = """
        {
          "eventType": "${eventType.eventType}",
          "personReference": { "identifiers": [ { "type": "NOMS", "value": "$prisonNumber" } ] },
          "occurredAt": "$occurredAt",
          "publishedAt": "$publishedAt",
          "description": "$description",
          "version": "$version",
          "additionalInformation": ${ObjectMapper().writeValueAsString(additionalInformation)}
        }        
  """.trimIndent(),
  MessageId = UUID.randomUUID(),
)

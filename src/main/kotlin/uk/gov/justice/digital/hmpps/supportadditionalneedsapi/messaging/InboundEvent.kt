package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging

import com.fasterxml.jackson.databind.JsonNode
import java.time.Instant

data class InboundEvent(
  val eventType: EventType,
  val personReference: PersonReference,
  val additionalInformation: JsonNode,
  val occurredAt: Instant,
  val publishedAt: Instant,
  val description: String,
  val version: String,
) {
  fun prisonNumber(): String = personReference.identifiers.first { it.type == "NOMS" }.value
}

data class PersonReference(val identifiers: List<Identifier>)

data class Identifier(val type: String, val value: String)

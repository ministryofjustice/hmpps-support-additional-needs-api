package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client

import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.ValueDeserializer

/**
 * A Jackson deserializer that can be used on a String field, performing a full trim on the value before setting the field.
 */
class StringTrimDeserializer : ValueDeserializer<String>() {
  override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): String = jsonParser.valueAsString.trim()
}

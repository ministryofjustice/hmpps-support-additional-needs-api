package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.deser.std.StdDeserializer

/**
 * A Jackson deserializer that can be used on a String field, performing a full trim on the value before setting the field.
 */
class StringTrimDeserializer(vc: Class<*>?) : StdDeserializer<String>(vc) {
  override fun deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): String = jsonParser.valueAsString.trim()
}

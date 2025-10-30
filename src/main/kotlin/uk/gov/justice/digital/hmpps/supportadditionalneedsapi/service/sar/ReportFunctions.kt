package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

/** common SAR report functions
 *
 */

// convert boolean to yes/no/unknown
fun toYesNo(value: Boolean?): String = when (value) {
  true -> "Yes"
  false -> "No"
  null -> "Unknown"
}

// get a comma separated list of enum names
fun <E : Enum<E>> toCommaSeparatedString(list: List<E>?): String = list?.joinToString(", ") { e ->
  e.name.lowercase().replace("_", " ")
} ?: ""

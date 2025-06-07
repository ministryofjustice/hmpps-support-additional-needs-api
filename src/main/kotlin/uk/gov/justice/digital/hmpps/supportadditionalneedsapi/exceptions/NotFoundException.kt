package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions

fun <E : RuntimeException> verify(boolean: Boolean, exception: () -> E) {
  if (!boolean) throw exception()
}

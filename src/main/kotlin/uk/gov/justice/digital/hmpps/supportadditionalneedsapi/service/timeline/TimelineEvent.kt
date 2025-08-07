package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EventType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TimelineEvent(
  val eventType: EventType,
  val prisonNumberParam: String = "prisonNumber",
  val additionalInfoPrefix: String = "",
  val additionalInfoField: String = "",
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class TimelineEvent(
  val eventType: TimelineEventType,
  val prisonNumberParam: String = "prisonNumber",
  val additionalInfoPrefix: String = "",
  val additionalInfoField: String = "",
)

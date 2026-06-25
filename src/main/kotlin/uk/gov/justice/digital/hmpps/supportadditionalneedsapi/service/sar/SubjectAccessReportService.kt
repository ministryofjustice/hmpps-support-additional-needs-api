package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SubjectAccessReportService(
  private val educationSupportPlanService: EducationSupportPlanService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInstance = fromDate?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
    val toDateInstance = toDate?.atStartOfDay()?.plusDays(1)?.atOffset(ZoneOffset.UTC)

    val educationSupportPlan = getEducationSupportPlan(prn, fromDateInstance, toDateInstance)

    return if (educationSupportPlan != null) {
      HmppsSubjectAccessRequestContent(
        content = SubjectAccessRequestContent(
          educationSupportPlan = educationSupportPlan,
        ),
      )
    } else {
      null
    }
  }

  private fun getEducationSupportPlan(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): EducationSupportPlanResponse? = educationSupportPlanService.getOriginalPlan(prn)
    ?.takeIf { it.createdAt.inRange(fromDateInstance, toDateInstance) }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from)) &&
  (to == null || this.isBefore(to))

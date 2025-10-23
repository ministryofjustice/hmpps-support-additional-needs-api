package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SubjectAccessReportService(
  private val educationSupportPlanService: EducationSupportPlanService,
  private val challengeService: ChallengeService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInstance = fromDate
      ?.atStartOfDay()
      ?.atOffset(ZoneOffset.UTC)

    val toDateInstance = toDate?.atStartOfDay()
      ?.plusDays(1)
      ?.atOffset(ZoneOffset.UTC)

    // get raw data for the date range
    val educationSupportPlan = getEducationSupportPlan(prn, fromDateInstance, toDateInstance)
    val challenges = getChallenges(prn, fromDateInstance, toDateInstance)

    // convert the raw data into SAR report
    val sanContent = SupportAdditionalNeedsContent(
      educationSupportPlan = educationSupportPlan?.toReportModel(),
      challenges = challenges.map { it.toReportModel() },
    )

    // return if any data is in the report
    return if (sanContent.hasContent()) {
      HmppsSubjectAccessRequestContent(content = sanContent)
    } else {
      null
    }
  }

  private fun getEducationSupportPlan(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ) = if (educationSupportPlanService.hasPlan(prn)) {
    educationSupportPlanService.getPlan(prn)
      .takeIf {
        it.createdAt.inRange(fromDateInstance, toDateInstance)
      }
  } else {
    null
  }

  private fun getChallenges(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ChallengeResponse> = challengeService
    .getChallenges(prisonNumber = prn)
    .challenges
    .filter { !it.fromALNScreener && it.createdAt.inRange(fromDateInstance, toDateInstance) }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from)) &&
  (to == null || this.isBefore(to))

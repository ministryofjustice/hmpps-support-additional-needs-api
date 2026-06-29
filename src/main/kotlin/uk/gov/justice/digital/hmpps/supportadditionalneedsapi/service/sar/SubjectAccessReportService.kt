package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.sar.SarChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SarChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
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
  private val sarChallengeMapper: SarChallengeMapper,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInstance = fromDate?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
    val toDateInstance = toDate?.atStartOfDay()?.plusDays(1)?.atOffset(ZoneOffset.UTC)

    val originalEducationSupportPlan = getOriginalEducationSupportPlan(prn, fromDateInstance, toDateInstance)
    val challenges = getChallenges(prn, fromDateInstance, toDateInstance)

    return if (originalEducationSupportPlan != null) {
      HmppsSubjectAccessRequestContent(
        content = SubjectAccessRequestContent(
          originalEducationSupportPlan = originalEducationSupportPlan,
          challenges = challenges,
        ),
      )
    } else {
      null
    }
  }

  private fun getOriginalEducationSupportPlan(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): EducationSupportPlanResponse? = educationSupportPlanService.getOriginalPlan(prn)
    ?.takeIf { it.createdAt.inRange(fromDateInstance, toDateInstance) }

  /**
   * Obtain manually added challenges of the prisoner (excluding challenges from ALN screener)
   */
  private fun getChallenges(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<SarChallengeResponse> = challengeService.getChallenges(prisonNumber = prn, includeAln = false).challenges
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedByDescending { it.createdAt }
    .map { sarChallengeMapper.fromResponse(it) }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from)) &&
  (to == null || this.isBefore(to))

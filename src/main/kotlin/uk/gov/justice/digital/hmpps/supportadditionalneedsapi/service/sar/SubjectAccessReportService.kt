package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SupportStrategyService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SubjectAccessReportService(
  private val educationSupportPlanService: EducationSupportPlanService,
  private val supportStrategyService: SupportStrategyService,
  private val strengthService: StrengthService,
  private val challengeService: ChallengeService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInstance = fromDate?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
    val toDateInstance = toDate?.atStartOfDay()?.plusDays(1)?.atOffset(ZoneOffset.UTC)

    val originalEducationSupportPlan = getOriginalEducationSupportPlan(prn, fromDateInstance, toDateInstance)
    val supportStrategies = getSupportStrategies(prn, fromDateInstance, toDateInstance)
    val nonAlnStrengths = getNonAlnStrengths(prn, fromDateInstance, toDateInstance)
    val nonAlnChallenges = getNonAlnChallenges(prn, fromDateInstance, toDateInstance)

    return if (
      originalEducationSupportPlan != null ||
      supportStrategies.isNotEmpty() ||
      nonAlnStrengths.isNotEmpty() ||
      nonAlnChallenges.isNotEmpty()
    ) {
      HmppsSubjectAccessRequestContent(
        content = SubjectAccessRequestContent(
          originalEducationSupportPlan = originalEducationSupportPlan,
          supportStrategies = supportStrategies,
          nonAlnStrengths = nonAlnStrengths,
          nonAlnChallenges = nonAlnChallenges,
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

  private fun getSupportStrategies(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<SupportStrategyResponse> = supportStrategyService.getSupportStrategies(prn).supportStrategies
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedBy { it.createdAt }

  private fun getNonAlnStrengths(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<StrengthResponse> = strengthService.getStrengths(prn, includeAln = false).strengths
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedBy { it.createdAt }

  /**
   * Obtain manually added challenges of the prisoner (excluding challenges from ALN screener)
   */
  private fun getNonAlnChallenges(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ChallengeResponse> = challengeService.getChallenges(prisonNumber = prn, includeAln = false).challenges
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedByDescending { it.createdAt }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from)) &&
  (to == null || this.isBefore(to))

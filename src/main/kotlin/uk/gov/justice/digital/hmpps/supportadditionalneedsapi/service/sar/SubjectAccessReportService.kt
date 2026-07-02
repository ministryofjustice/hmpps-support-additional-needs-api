package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNAssessmentResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ALNScreenerService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SupportStrategyService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SubjectAccessReportService(
  private val educationSupportPlanService: EducationSupportPlanService,
  private val supportStrategyService: SupportStrategyService,
  private val strengthService: StrengthService,
  private val challengeService: ChallengeService,
  private val alnScreenerService: ALNScreenerService,
  private val planCreationScheduleService: PlanCreationScheduleService,
  private val conditionService: ConditionService,
  private val needService: NeedService,
) : HmppsPrisonSubjectAccessRequestService {

  override fun getPrisonContentFor(
    prn: String,
    fromDate: LocalDate?,
    toDate: LocalDate?,
  ): HmppsSubjectAccessRequestContent? {
    val fromDateInstance = fromDate?.atStartOfDay()?.atOffset(ZoneOffset.UTC)
    val toDateInstance = toDate?.atEndOfDay()?.atOffset(ZoneOffset.UTC)

    val originalEducationSupportPlan = getOriginalEducationSupportPlanIfCreatedBeforeToDate(prn, toDateInstance)
    val supportStrategies = getSupportStrategies(prn, fromDateInstance, toDateInstance)
    val nonAlnStrengths = getNonAlnStrengths(prn, fromDateInstance, toDateInstance)
    val nonAlnChallenges = getNonAlnChallenges(prn, fromDateInstance, toDateInstance)
    val alnScreeners = getAlnScreeners(prn, fromDateInstance, toDateInstance)
    val planCreationSchedules = getPlanCreationSchedules(prn, fromDateInstance, toDateInstance)
    val conditions = getConditions(prn, fromDateInstance, toDateInstance)
    val alnAssessments = getAlnAssessments(prn, fromDateInstance, toDateInstance)

    return if (
      originalEducationSupportPlan != null ||
      supportStrategies.isNotEmpty() ||
      nonAlnStrengths.isNotEmpty() ||
      nonAlnChallenges.isNotEmpty() ||
      alnScreeners.isNotEmpty() ||
      planCreationSchedules.isNotEmpty() ||
      conditions.isNotEmpty() ||
      alnAssessments.isNotEmpty()
    ) {
      HmppsSubjectAccessRequestContent(
        content = SubjectAccessRequestContent(
          originalEducationSupportPlan = originalEducationSupportPlan,
          supportStrategies = supportStrategies,
          nonAlnStrengths = nonAlnStrengths,
          nonAlnChallenges = nonAlnChallenges,
          alnScreeners = alnScreeners,
          planCreationSchedules = planCreationSchedules,
          conditions = conditions,
          alnAssessments = alnAssessments,
        ),
      )
    } else {
      null
    }
  }

  private fun getOriginalEducationSupportPlanIfCreatedBeforeToDate(
    prn: String,
    toDateInstance: OffsetDateTime?,
  ): EducationSupportPlanResponse? = educationSupportPlanService.getOriginalPlan(prn)
    ?.takeIf { it.createdAt.isEqualOrBefore(toDateInstance) }

  private fun getSupportStrategies(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<SupportStrategyResponse> = supportStrategyService.getSupportStrategies(prn).supportStrategies
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedByDescending { it.createdAt }

  private fun getNonAlnStrengths(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<StrengthResponse> = strengthService.getStrengths(prn, includeAln = false).strengths
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedByDescending { it.createdAt }

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

  private fun getAlnScreeners(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ALNScreenerResponse> = alnScreenerService.getScreeners(prn).screeners
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }

  private fun getPlanCreationSchedules(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<PlanCreationScheduleResponse> = planCreationScheduleService.getSchedules(prn, includeAllHistory = true).planCreationSchedules
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedBy { it.version }

  /**
   * Obtain all conditions of the prisoner
   */
  private fun getConditions(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ConditionResponse> = conditionService.getConditions(prisonNumber = prn).conditions
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
    .sortedByDescending { it.createdAt }

  private fun getAlnAssessments(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ALNAssessmentResponse> = needService.getAlnScreenerNeeds(prn).assessments
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = this.isEqualOrAfter(from) && this.isEqualOrBefore(to)

private fun OffsetDateTime.isEqualOrAfter(other: OffsetDateTime?): Boolean = (other == null || this == other || isAfter(other))

private fun OffsetDateTime.isEqualOrBefore(other: OffsetDateTime?): Boolean = (other == null || this == other || this.isBefore(other))

private fun LocalDate.atEndOfDay(): LocalDateTime = LocalDateTime.of(this, LocalTime.parse("23:59:59.999"))

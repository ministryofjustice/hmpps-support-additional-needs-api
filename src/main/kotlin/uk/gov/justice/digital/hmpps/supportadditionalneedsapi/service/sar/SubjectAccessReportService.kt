package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ALNScreenerService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import uk.gov.justice.hmpps.kotlin.sar.HmppsPrisonSubjectAccessRequestService
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
class SubjectAccessReportService(
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val conditionService: ConditionService,
  private val elspPlanHistoryRepository: ElspPlanHistoryRepository,
  private val elspReviewHistoryRepository: ElspReviewHistoryRepository,
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository,
  private val alnScreenerService: ALNScreenerService,
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
    val educationSupportPlans = getEducationSupportPlans(prn, fromDateInstance, toDateInstance)
    val reviews = getReviews(prn, fromDateInstance, toDateInstance)
    val challenges = getChallenges(prn, fromDateInstance, toDateInstance)
    val strengths = getStrengths(prn, fromDateInstance, toDateInstance)
    val conditions = getConditions(prn, fromDateInstance, toDateInstance)
    val planCreationSchedules = getPlanCreationSchedules(prn, fromDateInstance, toDateInstance)
    val reviewSchedules = getReviewSchedules(prn, fromDateInstance, toDateInstance)
    val alnScreeners = getAlnScreeners(prn, fromDateInstance, toDateInstance)

    // convert the raw data into SAR report
    val sanContent = SupportAdditionalNeedsContent(
      educationSupportPlans = educationSupportPlans.map { it.toReportModel() },
      reviews = reviews.map { it.toReportModel() },
      challenges = challenges.map { it.toReportModel() },
      strengths = strengths.map { it.toReportModel() },
      conditions = conditions.map { it.toReportModel() },
      planCreationSchedules = planCreationSchedules.map { it.toReportModel() },
      reviewSchedules = reviewSchedules.map { it.toReportModel() },
      alnScreeners = alnScreeners.map { it.toReportModel() },
    )

    // return if any data is in the report
    return if (sanContent.hasContent()) {
      HmppsSubjectAccessRequestContent(content = sanContent)
    } else {
      null
    }
  }

  private fun getEducationSupportPlans(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ElspPlanHistoryEntity> = elspPlanHistoryRepository.findAllByPrisonNumber(prisonNumber = prn)
    .filter { it.updatedAt.inRange(fromDateInstance, toDateInstance) }

  private fun getReviews(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ElspReviewHistoryEntity> = elspReviewHistoryRepository.findAllByPrisonNumber(prisonNumber = prn)
    .filter { it.updatedAt.inRange(fromDateInstance, toDateInstance) }

  private fun getPlanCreationSchedules(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<PlanCreationScheduleHistoryEntity> = planCreationScheduleHistoryRepository.findAllByPrisonNumberOrderByVersionAsc(prisonNumber = prn)
    .filter { it.updatedAt.inRange(fromDateInstance, toDateInstance) }

  private fun getReviewSchedules(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ReviewScheduleHistoryEntity> = reviewScheduleHistoryRepository.findAllByPrisonNumber(prisonNumber = prn)
    .filter { it.updatedAt.inRange(fromDateInstance, toDateInstance) }

  private fun getChallenges(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ChallengeResponse> = challengeService
    .getChallenges(prisonNumber = prn)
    .challenges
    .filter { !it.fromALNScreener && it.createdAt.inRange(fromDateInstance, toDateInstance) }

  private fun getStrengths(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<StrengthResponse> = strengthService
    .getStrengths(prisonNumber = prn)
    .strengths
    .filter { !it.fromALNScreener && it.createdAt.inRange(fromDateInstance, toDateInstance) }

  private fun getConditions(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ConditionResponse> = conditionService
    .getConditions(prisonNumber = prn)
    .conditions
    .filter { it.createdAt.inRange(fromDateInstance, toDateInstance) }

  private fun getAlnScreeners(
    prn: String,
    fromDateInstance: OffsetDateTime?,
    toDateInstance: OffsetDateTime?,
  ): List<ALNScreenerResponse> = alnScreenerService.getScreeners(prisonNumber = prn).screeners
    .filter { it.updatedAt.inRange(fromDateInstance, toDateInstance) }
}

private fun OffsetDateTime.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from)) &&
  (to == null || this.isBefore(to))

private fun Instant.inRange(from: OffsetDateTime?, to: OffsetDateTime?): Boolean = (from == null || !this.isBefore(from.toInstant())) &&
  (to == null || this.isBefore(to.toInstant()))

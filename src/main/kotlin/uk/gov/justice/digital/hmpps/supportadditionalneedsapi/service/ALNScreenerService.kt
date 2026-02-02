package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.ALNAssessment
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.DEFAULT_PRISON_ID
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.ALN_SCREENER_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CURIOUS_ASSESSMENT_TRIGGER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ALNScreenerMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreeners
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.time.LocalDate

private val log = KotlinLogging.logger {}
private const val YES = "YES"

@Service
class ALNScreenerService(
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val curiousApiClient: CuriousApiClient,
  private val needService: NeedService,
  private val alnScreenerMapper: ALNScreenerMapper,
  private val scheduleService: ScheduleService,
) {
  @Transactional
  @TimelineEvent(
    eventType = ALN_SCREENER_ADDED,
    additionalInfoPrefix = "screenerDate:",
    additionalInfoField = "screenerDate",
  )
  fun createScreener(prisonNumber: String, request: ALNScreenerRequest) {
    // Whenever a new screener is added mark any existing screener challenges/strengths as inactive.
    challengeService.archiveAllScreenerChallenges(prisonNumber)
    strengthService.archiveAllScreenerStrengths(prisonNumber)

    with(request) {
      val alnScreener = alnScreenerRepository.saveAndFlush(
        ALNScreenerEntity(
          prisonNumber = prisonNumber,
          createdAtPrison = prisonId,
          updatedAtPrison = prisonId,
          screeningDate = screenerDate,
          hasChallenges = challenges.isNotEmpty(),
          hasStrengths = strengths.isNotEmpty(),
        ),
      )
      challengeService.createAlnChallenges(prisonNumber, challenges, prisonId, alnScreener.id)
      strengthService.createAlnStrengths(prisonNumber, strengths, prisonId, alnScreener.id)
      alnScreenerRepository.saveAndFlush(alnScreener)
    }
  }

  fun getScreeners(prisonNumber: String): ALNScreeners {
    val screeners = alnScreenerRepository
      .findAllByPrisonNumber(prisonNumber)
      .sortedWith(
        // Sort by most recent screening first.
        // If two or more screeners have the same date, use most recently updated
        compareByDescending<ALNScreenerEntity> { it.screeningDate }
          .thenByDescending { it.updatedAt },
      )

    return ALNScreeners(screeners.map { alnScreenerMapper.toModel(it) })
  }

  @Transactional
  @TimelineEvent(
    eventType = CURIOUS_ASSESSMENT_TRIGGER,
    additionalInfoPrefix = "curiousReference:",
    additionalInfoField = "curiousExternalReference",
  )
  fun processALNAssessmentUpdate(
    prisonNumber: String,
    info: EducationALNAssessmentUpdateAdditionalInformation,
    inboundEvent: InboundEvent,
  ) {
    log.info(
      "Processing ALN assessment update event: ${inboundEvent.description} for $prisonNumber\n" +
        "Detail URL: ${inboundEvent.detailUrl}, reference: ${info.curiousExternalReference}",
    )

    val originalOverallNeed = needService.hasNeed(prisonNumber = prisonNumber)

    log.info("Retrieving ALN assessments for $prisonNumber")
    val alnAssessments = curiousApiClient.getALNAssessment(prisonNumber).alnAssessments.orEmpty()

    val latestAssessment = alnAssessments.maxWithOrNull(
      compareBy<ALNAssessment> { it.assessmentDate }
        .thenBy { it.modifiedDate },
    )

    if (latestAssessment == null) {
      log.warn("No valid ALN assessments found for $prisonNumber — skipping need recording.")
      return
    }

    val hasNeed = latestAssessment.assessmentOutcome.equals(YES, ignoreCase = true)
    val prisonId = getPrisonId(latestAssessment)
    log.info("Identified ALN need for $prisonNumber: hasNeed = $hasNeed")

    needService.recordAlnScreenerNeed(
      prisonNumber = prisonNumber,
      hasNeed = hasNeed,
      curiousReference = info.curiousExternalReference,
      screenerDate = latestAssessment.assessmentDate,
    )

    val overallNeed = needService.hasNeed(prisonNumber = prisonNumber)
    // has the overall need changed?
    if (originalOverallNeed != overallNeed) {
      val alnAssessmentDate: LocalDate? =
        if (hasNeed) latestAssessment.assessmentDate else null
      log.info("The ALN need update changed the overall need of $prisonNumber")
      scheduleService.processNeedChange(prisonNumber, overallNeed, alnAssessmentDate, prisonId = prisonId)
    } else {
      log.info("The ALN need update did not change the overall need of $prisonNumber")
    }
    log.info("Processed ALN assessment for $prisonNumber: $latestAssessment")
  }

  // Attempt to get the prisonId from the assessment
  private fun getPrisonId(latestAssessment: ALNAssessment): String = latestAssessment.establishmentId
    ?.takeIf { it.length == 3 }
    ?: DEFAULT_PRISON_ID
}

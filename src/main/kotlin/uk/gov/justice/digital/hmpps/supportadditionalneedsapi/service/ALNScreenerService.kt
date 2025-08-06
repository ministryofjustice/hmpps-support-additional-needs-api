package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest

private val log = KotlinLogging.logger {}
private const val YES = "YES"

@Service
class ALNScreenerService(
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val curiousApiClient: CuriousApiClient,
  private val needService: NeedService,
) {
  @Transactional
  fun createScreener(prisonNumber: String, request: ALNScreenerRequest) {
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

  @Transactional
  fun processALNAssessmentUpdate(
    inboundEvent: InboundEvent,
    info: EducationALNAssessmentUpdateAdditionalInformation,
  ) {
    val prisonNumber = inboundEvent.prisonNumber()

    log.info(
      "Processing ALN assessment update event: ${inboundEvent.description} for $prisonNumber\n" +
        "Detail URL: ${inboundEvent.detailUrl}, reference: ${info.curiousExternalReference}",
    )

    log.info("Retrieving ALN assessments for $prisonNumber")
    val alnAssessments = curiousApiClient.getALNAssessment(prisonNumber).alnAssessments.orEmpty()

    val latestAssessment = alnAssessments
      .filter { it.assessmentDate != null }
      .maxByOrNull { it.assessmentDate!! }

    if (latestAssessment == null) {
      log.warn("No valid ALN assessments found for $prisonNumber — skipping need recording.")
      return
    }

    val hasNeed = latestAssessment.assessmentOutcome.equals(YES, ignoreCase = true)
    log.info("Identified ALN need for $prisonNumber: hasNeed = $hasNeed")

    needService.recordAlnScreenerNeed(
      prisonNumber = prisonNumber,
      hasNeed = hasNeed,
      curiousReference = info.curiousExternalReference,
      screenerDate = latestAssessment.assessmentDate!!,
    )

    // TODO Now update the plan creation schedule or the plan review schedule
    // if there is a schedule and now there is no need - EXEMPT due to NO_NEED
    // if there is no schedule AND the person is in EDUCATION create a new plan creation schedule OR
    // review schedule

    log.info("Processed ALN assessment for $prisonNumber: $latestAssessment")
  }
}

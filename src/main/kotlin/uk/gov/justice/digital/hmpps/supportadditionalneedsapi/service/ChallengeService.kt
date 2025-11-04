package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.ALN_CHALLENGE_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CHALLENGE_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeAlnScreenerException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeArchivedException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class ChallengeService(
  private val challengeRepository: ChallengeRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val challengeMapper: ChallengeMapper,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val scheduleService: ScheduleService,
  private val needService: NeedService,
) {
  fun getChallenges(prisonNumber: String): ChallengeListResponse {
    val nonAlnChallenges = challengeRepository
      .findAllByPrisonNumberAndAlnScreenerIdIsNull(prisonNumber)

    val alnScreener = alnScreenerRepository
      .findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)

    val alnChallenges = alnScreener
      ?.challenges
      .orEmpty()

    val allChallenges = nonAlnChallenges + alnChallenges

    val models = allChallenges.map { challengeMapper.toModel(it, alnScreener?.screeningDate) }
    return ChallengeListResponse(models)
  }

  @Transactional
  @TimelineEvent(
    eventType = CHALLENGE_ADDED,
    additionalInfoPrefix = "ChallengeType:",
    additionalInfoField = "challengeTypeCode",
  )
  fun createChallenges(prisonNumber: String, request: CreateChallengesRequest): ChallengeListResponse {
    val challengeTypeEntities = resolveChallengeTypes(request)

    val challenges = challengeTypeEntities.map { (challengeType, requestItem) ->
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = challengeType,
        createdAtPrison = requestItem.prisonId,
        updatedAtPrison = requestItem.prisonId,
        symptoms = requestItem.symptoms,
        howIdentified = challengeMapper.toEntity(requestItem.howIdentified),
        howIdentifiedOther = requestItem.howIdentifiedOther,
        active = true,
      )
    }

    val savedChallenges = challengeRepository.saveAllAndFlush(challenges)

    // update schedules and send messages
    val prisonId = savedChallenges.first().createdAtPrison
    scheduleService.processNeedChange(prisonNumber, true, prisonId = prisonId)
    return ChallengeListResponse(savedChallenges.map { challengeMapper.toModel(it) })
  }

  private fun resolveChallengeTypes(request: CreateChallengesRequest): List<Pair<ReferenceDataEntity, ChallengeRequest>> = request.challenges.map { challengeRequest ->
    val code = requireNotNull(challengeRequest.challengeTypeCode) { "Challenge type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CHALLENGE, code))
    type to challengeRequest
  }

  @Transactional
  @TimelineEvent(
    eventType = ALN_CHALLENGE_ADDED,
    additionalInfoPrefix = "ChallengeType:",
    additionalInfoField = "challengeTypeCode",
  )
  fun createAlnChallenges(
    prisonNumber: String,
    alnChallenges: List<ALNChallenge>,
    prisonId: String,
    alnScreenerId: UUID,
  ) {
    if (alnChallenges.isNotEmpty()) {
      val challengeTypeEntities = resolveChallengeTypes(alnChallenges)

      val challenges = challengeTypeEntities.map { (challengeType) ->
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = challengeType,
          createdAtPrison = prisonId,
          updatedAtPrison = prisonId,
          alnScreenerId = alnScreenerId,
          active = true,
        )
      }
      challengeRepository.saveAll(challenges)
    }
  }

  private fun resolveChallengeTypes(request: List<ALNChallenge>): List<Pair<ReferenceDataEntity, ALNChallenge>> = request.map { challenge ->
    val code = requireNotNull(challenge.challengeTypeCode) { "Challenge type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CHALLENGE, code))
    type to challenge
  }

  @Transactional
  fun updateChallenge(
    prisonNumber: String,
    challengeReference: UUID,
    request: UpdateChallengeRequest,
  ): ChallengeResponse {
    val
    challenge = challengeRepository.getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      ?: throw ChallengeNotFoundException(prisonNumber, challengeReference)

    challenge.symptoms = request.symptoms
    challenge.howIdentified = challengeMapper.toEntity(request.howIdentified)
    challenge.howIdentifiedOther = request.howIdentifiedOther
    challenge.updatedAtPrison = request.prisonId

    return challengeMapper.toModel(challengeRepository.save(challenge))
  }

  fun getChallenge(
    prisonNumber: String,
    challengeReference: UUID,
  ): ChallengeResponse {
    val
    challenge = challengeRepository.getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      ?: throw ChallengeNotFoundException(prisonNumber, challengeReference)
    return challengeMapper.toModel(challenge)
  }

  @Transactional
  fun archiveChallenge(
    prisonNumber: String,
    challengeReference: UUID,
    request: ArchiveChallengeRequest,
  ) {
    val challenge = challengeRepository.getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      ?: throw ChallengeNotFoundException(prisonNumber, challengeReference)

    if (!challenge.active) {
      throw ChallengeArchivedException(prisonNumber, challengeReference)
    }
    // only non screener challenge can be archived:
    if (challenge.alnScreenerId != null) {
      throw ChallengeAlnScreenerException(prisonNumber, challengeReference)
    }

    challenge.active = false
    challenge.archiveReason = request.archiveReason
    challenge.updatedAtPrison = request.prisonId
    challengeRepository.save(challenge)

    // has this changed the persons need? do we need to update MN?
    val hasNeed = needService.hasNeed(prisonNumber = prisonNumber)
    // has archiving this record caused the overall need changed?
    if (!hasNeed) {
      log.info("Prisoner $prisonNumber no longer has a need due to archived challenge.")
      scheduleService.processNeedChange(prisonNumber, hasNeed, prisonId = request.prisonId)
    } else {
      log.info("The challenge update did not change the overall need of $prisonNumber")
    }
    log.info("Processed Archive challenge for $prisonNumber")
  }
}

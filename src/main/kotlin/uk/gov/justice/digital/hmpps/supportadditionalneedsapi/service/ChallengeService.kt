package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

@Service
class ChallengeService(
  private val challengeRepository: ChallengeRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val challengeMapper: ChallengeMapper,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val scheduleService: ScheduleService,
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

  fun updateChallenge(
    prisonNumber: String,
    challengeReference: UUID,
    request: UpdateChallengeRequest,
  ): ChallengeResponse {
    val
    challenge = challengeRepository.getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      ?: throw ChallengeNotFoundException(prisonNumber, challengeReference)

    challenge.active = request.active
    challenge.updatedAtPrison = request.prisonId

    return challengeMapper.toModel(challengeRepository.save(challenge))
  }
}

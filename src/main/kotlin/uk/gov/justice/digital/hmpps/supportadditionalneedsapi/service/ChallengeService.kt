package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import java.util.*

@Service
class ChallengeService(
  private val challengeRepository: ChallengeRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val challengeMapper: ChallengeMapper,
) {
  fun getChallenges(prisonNumber: String): ChallengeListResponse {
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    val models = challenges.map { challengeMapper.toModel(it) }
    return ChallengeListResponse(models)
  }

  @Transactional
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
    return ChallengeListResponse(savedChallenges.map { challengeMapper.toModel(it) })
  }

  private fun resolveChallengeTypes(request: CreateChallengesRequest): List<Pair<ReferenceDataEntity, ChallengeRequest>> = request.challenges.map { challengeRequest ->
    val code = requireNotNull(challengeRequest.challengeTypeCode) { "Challenge type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CHALLENGE, code))
    type to challengeRequest
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

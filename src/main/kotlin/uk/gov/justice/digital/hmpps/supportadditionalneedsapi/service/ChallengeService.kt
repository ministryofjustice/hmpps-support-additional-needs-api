package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.DuplicateChallengeException
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

  fun createChallenges(prisonNumber: String, request: CreateChallengesRequest): ChallengeListResponse {
    validateNoDuplicateCodesInRequest(prisonNumber, request)

    val challengeTypeEntities = resolveChallengeTypes(request)

    validateNoDuplicateChallengesInDatabase(prisonNumber, challengeTypeEntities)

    val challenges = challengeTypeEntities.map { (challengeType, requestItem) ->
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = challengeType,
        createdAtPrison = requestItem.prisonId,
        updatedAtPrison = requestItem.prisonId,
        symptoms = requestItem.symptoms,
        howIdentified = requestItem.howIdentified,
        active = true,
      )
    }

    val savedChallenges = challengeRepository.saveAll(challenges)
    return ChallengeListResponse(savedChallenges.map { challengeMapper.toModel(it) })
  }

  private fun validateNoDuplicateCodesInRequest(prisonNumber: String, request: CreateChallengesRequest) {
    val duplicateCodes = request.challenges
      .mapNotNull { it.challengeTypeCode }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .keys

    if (duplicateCodes.isNotEmpty()) {
      throw DuplicateChallengeException(prisonNumber, duplicateCodes.joinToString(", "))
    }
  }

  private fun resolveChallengeTypes(request: CreateChallengesRequest): List<Pair<ReferenceDataEntity, ChallengeRequest>> = request.challenges.map { challengeRequest ->
    val code = requireNotNull(challengeRequest.challengeTypeCode) { "Challenge type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CHALLENGE, code))
    type to challengeRequest
  }

  private fun validateNoDuplicateChallengesInDatabase(
    prisonNumber: String,
    challengeTypeEntities: List<Pair<ReferenceDataEntity, ChallengeRequest>>,
  ) {
    val existingCodes = challengeRepository.findAllByPrisonNumber(prisonNumber)
      .map { it.challengeType.key.code }
      .toSet()

    val newCodes = challengeTypeEntities.map { (type, _) -> type.key.code }

    val alreadyExists = newCodes.intersect(existingCodes)
    if (alreadyExists.isNotEmpty()) {
      throw DuplicateChallengeException(prisonNumber, alreadyExists.joinToString(", "))
    }
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

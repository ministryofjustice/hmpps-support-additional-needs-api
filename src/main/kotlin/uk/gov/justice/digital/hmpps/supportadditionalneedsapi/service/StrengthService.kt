package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.StrengthRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.DuplicateStrengthException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.StrengthMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import java.util.*

@Service
class StrengthService(
  private val strengthRepository: StrengthRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val strengthMapper: StrengthMapper,
) {
  fun getStrengths(prisonNumber: String): StrengthListResponse {
    val strengths = strengthRepository.findAllByPrisonNumber(prisonNumber)
    val models = strengths.map { strengthMapper.toModel(it) }
    return StrengthListResponse(models)
  }

  @Transactional
  fun createStrengths(prisonNumber: String, request: CreateStrengthsRequest): StrengthListResponse {
    validateNoDuplicateCodesInRequest(prisonNumber, request)

    val strengthTypeEntities = resolveStrengthTypes(request)

    validateNoDuplicateStrengthsInDatabase(prisonNumber, strengthTypeEntities)

    val strengths = strengthTypeEntities.map { (strengthType, requestItem) ->
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = strengthType,
        createdAtPrison = requestItem.prisonId,
        updatedAtPrison = requestItem.prisonId,
        symptoms = requestItem.symptoms,
        howIdentified = strengthMapper.toEntity(requestItem.howIdentified),
        howIdentifiedOther = requestItem.howIdentifiedOther,
        active = true,
      )
    }

    val savedStrengths = strengthRepository.saveAllAndFlush(strengths)
    return StrengthListResponse(savedStrengths.map { strengthMapper.toModel(it) })
  }

  private fun validateNoDuplicateCodesInRequest(prisonNumber: String, request: CreateStrengthsRequest) {
    val duplicateCodes = request.strengths
      .map { it.strengthTypeCode }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .keys

    if (duplicateCodes.isNotEmpty()) {
      throw DuplicateStrengthException(prisonNumber, duplicateCodes.joinToString(", "))
    }
  }

  private fun resolveStrengthTypes(request: CreateStrengthsRequest): List<Pair<ReferenceDataEntity, StrengthRequest>> = request.strengths.map { strengthRequest ->
    val code = requireNotNull(strengthRequest.strengthTypeCode) { "Strength type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CHALLENGE, code))
    type to strengthRequest
  }

  private fun validateNoDuplicateStrengthsInDatabase(
    prisonNumber: String,
    strengthTypeEntities: List<Pair<ReferenceDataEntity, StrengthRequest>>,
  ) {
    val existingCodes = strengthRepository.findAllByPrisonNumber(prisonNumber)
      .map { it.strengthType.key.code }
      .toSet()

    val newCodes = strengthTypeEntities.map { (type, _) -> type.key.code }

    val alreadyExists = newCodes.intersect(existingCodes)
    if (alreadyExists.isNotEmpty()) {
      throw DuplicateStrengthException(prisonNumber, alreadyExists.joinToString(", "))
    }
  }

  fun updateStrength(
    prisonNumber: String,
    strengthReference: UUID,
    request: UpdateStrengthRequest,
  ): StrengthResponse {
    val
      strength = strengthRepository.getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      ?: throw StrengthNotFoundException(prisonNumber, strengthReference)

    strength.active = request.active
    strength.updatedAtPrison = request.prisonId

    return strengthMapper.toModel(strengthRepository.save(strength))
  }
}
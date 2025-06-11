package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.DuplicateConditionException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ConditionMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateConditionRequest
import java.util.*

@Service
class ConditionService(
  private val conditionRepository: ConditionRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val conditionMapper: ConditionMapper,
) {
  fun getConditions(prisonNumber: String): ConditionListResponse {
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    val models = conditions.map { conditionMapper.toModel(it) }
    return ConditionListResponse(models)
  }

  fun createConditions(prisonNumber: String, request: CreateConditionsRequest): ConditionListResponse {
    validateNoDuplicateCodesInRequest(prisonNumber, request)

    val conditionTypeEntities = resolveConditionTypes(request)

    validateNoDuplicateConditionsInDatabase(prisonNumber, conditionTypeEntities)

    val conditions = conditionTypeEntities.map { (conditionType, requestItem) ->
      conditionMapper.toEntity(prisonNumber, conditionType, requestItem)
    }

    val savedConditions = conditionRepository.saveAll(conditions)
    return ConditionListResponse(savedConditions.map { conditionMapper.toModel(it) })
  }

  private fun validateNoDuplicateCodesInRequest(prisonNumber: String, request: CreateConditionsRequest) {
    val duplicateCodes = request.conditions
      .mapNotNull { it.conditionTypeCode }
      .groupingBy { it }
      .eachCount()
      .filterValues { it > 1 }
      .keys

    if (duplicateCodes.isNotEmpty()) {
      throw DuplicateConditionException(prisonNumber, duplicateCodes.joinToString(", "))
    }
  }

  private fun resolveConditionTypes(request: CreateConditionsRequest): List<Pair<ReferenceDataEntity, ConditionRequest>> = request.conditions.map { conditionRequest ->
    val code = requireNotNull(conditionRequest.conditionTypeCode) { "Condition type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CONDITION, code))
    type to conditionRequest
  }

  private fun validateNoDuplicateConditionsInDatabase(
    prisonNumber: String,
    conditionTypeEntities: List<Pair<ReferenceDataEntity, ConditionRequest>>,
  ) {
    val existingCodes = conditionRepository.findAllByPrisonNumber(prisonNumber)
      .map { it.conditionType.key.code }
      .toSet()

    val newCodes = conditionTypeEntities.map { (type, _) -> type.key.code }

    val alreadyExists = newCodes.intersect(existingCodes)
    if (alreadyExists.isNotEmpty()) {
      throw DuplicateConditionException(prisonNumber, alreadyExists.joinToString(", "))
    }
  }

  fun updateCondition(
    prisonNumber: String,
    conditionReference: UUID,
    request: UpdateConditionRequest,
  ): ConditionResponse {
    val condition = conditionRepository.getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      ?: throw ConditionNotFoundException(prisonNumber, conditionReference)

    condition.active = request.active
    condition.updatedAtPrison = request.prisonId

    return conditionMapper.toModel(conditionRepository.save(condition))
  }
}

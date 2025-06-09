package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ConditionMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest

@Service
class ConditionService(
  private val conditionRepository: ConditionRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val conditionMapper: ConditionMapper,
) {
  fun createConditions(prisonNumber: String, request: CreateConditionsRequest): ConditionListResponse {
    val conditions = request.conditions.map { conditionRequest ->
      val conditionTypeCode = requireNotNull(conditionRequest.conditionTypeCode) { "Condition type code must not be null" }

      val conditionType = referenceDataRepository.validateReferenceData(
        ReferenceDataKey(Domain.CONDITION, conditionTypeCode),
      )

      ConditionEntity(
        prisonNumber = prisonNumber,
        conditionType = conditionType,
        source = conditionMapper.toEntity(conditionRequest.source),
        createdAtPrison = conditionRequest.prisonId,
        updatedAtPrison = conditionRequest.prisonId,
        active = true,
      )
    }

    val savedConditions = conditionRepository.saveAll(conditions)
    val models = savedConditions.map { conditionMapper.toModel(it) }

    return ConditionListResponse(models)
  }

  fun getConditions(prisonNumber: String): ConditionListResponse {
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    val models = conditions.map { conditionMapper.toModel(it) }
    return ConditionListResponse(models)
  }
}

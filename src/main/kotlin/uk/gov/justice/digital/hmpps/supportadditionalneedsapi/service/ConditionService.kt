package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ConditionMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
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

  @Transactional
  @TimelineEvent(
    eventType = EventType.CONDITION_ADDED,
    additionalInfoPrefix = "ConditionType:",
    additionalInfoField = "conditionTypeCode",
  )
  fun createConditions(prisonNumber: String, request: CreateConditionsRequest): ConditionListResponse {
    val conditionTypeEntities = resolveConditionTypes(request)

    val conditions = conditionTypeEntities.map { (conditionType, requestItem) ->
      conditionMapper.toEntity(prisonNumber, conditionType, requestItem)
    }

    val savedConditions = conditionRepository.saveAllAndFlush(conditions)
    return ConditionListResponse(savedConditions.map { conditionMapper.toModel(it) })
  }

  private fun resolveConditionTypes(request: CreateConditionsRequest): List<Pair<ReferenceDataEntity, ConditionRequest>> = request.conditions.map { conditionRequest ->
    val code = requireNotNull(conditionRequest.conditionTypeCode) { "Condition type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CONDITION, code))
    type to conditionRequest
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

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CONDITION_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionArchivedException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ConditionMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class ConditionService(
  private val conditionRepository: ConditionRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val conditionMapper: ConditionMapper,
  private val scheduleService: ScheduleService,
  private val needService: NeedService,
) {
  fun getConditions(prisonNumber: String): ConditionListResponse {
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)
    val models = conditions.map { conditionMapper.toModel(it) }
    return ConditionListResponse(models)
  }

  @Transactional
  @TimelineEvent(
    eventType = CONDITION_ADDED,
    additionalInfoPrefix = "ConditionType:",
    additionalInfoField = "conditionTypeCode",
  )
  fun createConditions(prisonNumber: String, request: CreateConditionsRequest): ConditionListResponse {
    val conditionTypeEntities = resolveConditionTypes(request)

    val conditions = conditionTypeEntities.map { (conditionType, requestItem) ->
      conditionMapper.toEntity(prisonNumber, conditionType, requestItem)
    }

    val savedConditions = conditionRepository.saveAllAndFlush(conditions)
    // update schedules and send messages
    val prisonId = savedConditions.first().createdAtPrison
    scheduleService.processNeedChange(prisonNumber, true, prisonId = prisonId)
    return ConditionListResponse(savedConditions.map { conditionMapper.toModel(it) })
  }

  private fun resolveConditionTypes(request: CreateConditionsRequest): List<Pair<ReferenceDataEntity, ConditionRequest>> = request.conditions.map { conditionRequest ->
    val code = requireNotNull(conditionRequest.conditionTypeCode) { "Condition type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.CONDITION, code))
    type to conditionRequest
  }

  @Transactional
  fun updateCondition(
    prisonNumber: String,
    conditionReference: UUID,
    request: UpdateConditionRequest,
  ): ConditionResponse {
    val condition = conditionRepository.getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      ?: throw ConditionNotFoundException(prisonNumber, conditionReference)

    condition.conditionDetails = request.conditionDetails
    condition.conditionName = request.conditionName
    condition.source = conditionMapper.toEntity(request.source)
    condition.updatedAtPrison = request.prisonId

    return conditionMapper.toModel(conditionRepository.save(condition))
  }

  fun getCondition(
    prisonNumber: String,
    conditionReference: UUID,
  ): ConditionResponse {
    val condition = conditionRepository.getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      ?: throw ConditionNotFoundException(prisonNumber, conditionReference)

    return conditionMapper.toModel(condition)
  }

  @Transactional
  fun archiveCondition(prisonNumber: String, conditionReference: UUID, request: ArchiveConditionRequest) {
    val condition = conditionRepository.getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      ?: throw ConditionNotFoundException(prisonNumber, conditionReference)

    if (!condition.active) {
      throw ConditionArchivedException(prisonNumber, conditionReference)
    }

    condition.active = false
    condition.archiveReason = request.archiveReason
    condition.updatedAtPrison = request.prisonId
    conditionRepository.save(condition)

    // has this changed the persons need? do we need to update MN?
    val hasNeed = needService.hasNeed(prisonNumber = prisonNumber)
    // has archiving this record caused the overall need changed?
    if (!hasNeed) {
      log.info("Prisoner $prisonNumber no longer has a need due to archived condition.")
      scheduleService.processNeedChange(prisonNumber, hasNeed, prisonId = request.prisonId)
    } else {
      log.info("The condition update did not change the overall need of $prisonNumber")
    }
    log.info("Processed Archive condition for $prisonNumber")
  }
}

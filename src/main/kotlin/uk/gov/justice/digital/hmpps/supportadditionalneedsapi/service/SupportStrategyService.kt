package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.SupportStrategyRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.SupportStrategyNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SupportStrategyMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateSupportStrategiesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateSupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

@Service
class SupportStrategyService(
  private val supportStrategyRepository: SupportStrategyRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val supportStrategyMapper: SupportStrategyMapper,
) {
  fun getSupportStrategies(prisonNumber: String): SupportStrategyListResponse {
    val strategyEntities = supportStrategyRepository.findAllByPrisonNumber(prisonNumber)
    val models = strategyEntities.map { supportStrategyMapper.toModel(it) }
    return SupportStrategyListResponse(models)
  }

  @Transactional
  @TimelineEvent(
    eventType = TimelineEventType.SUPPORT_STRATEGY_ADDED,
    additionalInfoPrefix = "SupportStrategyType:",
    additionalInfoField = "strategyTypeCode",
  )
  fun createSupportStrategies(prisonNumber: String, request: CreateSupportStrategiesRequest): SupportStrategyListResponse {
    val typeEntities = resolveSupportStrategyTypes(request)

    val supportStrategyEntities = typeEntities.map { (supportStrategyType, requestItem) ->
      supportStrategyMapper.toEntity(prisonNumber, supportStrategyType, requestItem)
    }

    val savedSupportStrategies = supportStrategyRepository.saveAllAndFlush(supportStrategyEntities)
    return SupportStrategyListResponse(savedSupportStrategies.map { supportStrategyMapper.toModel(it) })
  }

  private fun resolveSupportStrategyTypes(request: CreateSupportStrategiesRequest): List<Pair<ReferenceDataEntity, SupportStrategyRequest>> = request.supportStrategies.map { supportStrategiesRequest ->
    val code = requireNotNull(supportStrategiesRequest.supportStrategyTypeCode) { "Support Strategy Request type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.SUPPORT_STRATEGY, code))
    type to supportStrategiesRequest
  }

  fun updateSupportStrategy(
    prisonNumber: String,
    supportStrategyReference: UUID,
    request: UpdateSupportStrategyRequest,
  ): SupportStrategyResponse {
    val supportStrategy = supportStrategyRepository.getSupportStrategyEntityByPrisonNumberAndReference(prisonNumber, supportStrategyReference)
      ?: throw SupportStrategyNotFoundException(prisonNumber, supportStrategyReference)

    supportStrategy.active = request.active
    supportStrategy.updatedAtPrison = request.prisonId

    return supportStrategyMapper.toModel(supportStrategyRepository.save(supportStrategy))
  }
}

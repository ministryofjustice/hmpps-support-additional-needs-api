package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.StrengthRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.StrengthMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNStrength
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import java.util.*

@Service
class StrengthService(
  private val strengthRepository: StrengthRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val strengthMapper: StrengthMapper,
) {
  fun getStrengths(prisonNumber: String): StrengthListResponse {
    val nonAlnStrengths = strengthRepository
      .findAllByPrisonNumberAndAlnScreenerIdIsNull(prisonNumber)

    val alnScreener = alnScreenerRepository
      .findFirstByPrisonNumberOrderByScreeningDateDesc(prisonNumber)

    val alnStrengths = alnScreener?.strengths
      .orEmpty()

    val allStrengths = nonAlnStrengths + alnStrengths

    val models = allStrengths.map { strengthMapper.toModel(it, alnScreener?.screeningDate) }
    return StrengthListResponse(models)
  }

  @Transactional
  fun createStrengths(prisonNumber: String, request: CreateStrengthsRequest): StrengthListResponse {
    val strengthTypeEntities = resolveStrengthTypes(request)

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

  private fun resolveStrengthTypes(request: CreateStrengthsRequest): List<Pair<ReferenceDataEntity, StrengthRequest>> = request.strengths.map { strengthRequest ->
    val code = requireNotNull(strengthRequest.strengthTypeCode) { "Strength type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.STRENGTH, code))
    type to strengthRequest
  }

  @Transactional
  fun createAlnStrengths(prisonNumber: String, alnStrengths: List<ALNStrength>, prisonId: String, alnScreenerId: UUID) {
    if (alnStrengths.isNotEmpty()) {
      val strengthTypeEntities = resolveStrengthTypes(alnStrengths)

      val strengths = strengthTypeEntities.map { (strengthType) ->
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = strengthType,
          createdAtPrison = prisonId,
          updatedAtPrison = prisonId,
          alnScreenerId = alnScreenerId,
          active = true,
        )
      }
      strengthRepository.saveAll(strengths)
    }
  }

  private fun resolveStrengthTypes(request: List<ALNStrength>): List<Pair<ReferenceDataEntity, ALNStrength>> = request.map { Strength ->
    val code = requireNotNull(Strength.strengthTypeCode) { "Strength type code must not be null" }
    val type = referenceDataRepository.validateReferenceData(ReferenceDataKey(Domain.STRENGTH, code))
    type to Strength
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

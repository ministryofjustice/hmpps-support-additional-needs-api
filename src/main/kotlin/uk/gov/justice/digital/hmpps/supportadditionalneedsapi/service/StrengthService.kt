package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.ALN_STRENGTH_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.STRENGTH_ADDED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.STRENGTH_DELETED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.StrengthRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.validateReferenceData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthAlnScreenerException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthArchivedException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.DeletionReasonMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.IdentificationSourceMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.StrengthMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNStrength
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class StrengthService(
  private val strengthRepository: StrengthRepository,
  private val referenceDataRepository: ReferenceDataRepository,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val strengthMapper: StrengthMapper,
) {
  fun getStrengths(prisonNumber: String, includeAln: Boolean = true): StrengthListResponse {
    val nonAlnStrengths = strengthRepository
      .findAllByPrisonNumberAndAlnScreenerIdIsNull(prisonNumber)

    val alnScreener = if (includeAln) {
      alnScreenerRepository
        .findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
    } else {
      null
    }

    val alnStrengths = alnScreener?.strengths
      .orEmpty()

    val allStrengths = nonAlnStrengths + alnStrengths

    val models = allStrengths.map { strengthMapper.toModel(it, alnScreener?.screeningDate) }
    return StrengthListResponse(models)
  }

  /**
   * get all ALN screeners' strengths, sorted by screener date and updatedAt time in descending order
   */
  fun getAllScreenerStrengths(prisonNumber: String): StrengthListResponse = alnScreenerRepository.findAllByPrisonNumber(prisonNumber)
    .flatMap { alnScreener -> alnScreener.strengths.map { strengthMapper.toModel(it, alnScreener.screeningDate) } }
    .sortedWith(
      compareByDescending<StrengthResponse> { it.alnScreenerDate }
        .thenByDescending { it.updatedAt },
    )
    .let { StrengthListResponse(it) }

  @Transactional
  @TimelineEvent(
    eventType = STRENGTH_ADDED,
    additionalInfoPrefix = "StrengthType:",
    additionalInfoField = "strengthTypeCode",
  )
  fun createStrengths(prisonNumber: String, request: CreateStrengthsRequest): StrengthListResponse {
    val strengthTypeEntities = resolveStrengthTypes(request)

    val strengths = strengthTypeEntities.map { (strengthType, requestItem) ->
      StrengthEntity(
        prisonNumber = prisonNumber,
        strengthType = strengthType,
        createdAtPrison = requestItem.prisonId,
        updatedAtPrison = requestItem.prisonId,
        symptoms = requestItem.symptoms,
        howIdentified = IdentificationSourceMapper.toEntity(requestItem.howIdentified),
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
  @TimelineEvent(
    eventType = ALN_STRENGTH_ADDED,
    additionalInfoPrefix = "StrengthType:",
    additionalInfoField = "strengthTypeCode",
  )
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

  @Transactional
  fun updateStrength(
    prisonNumber: String,
    strengthReference: UUID,
    request: UpdateStrengthRequest,
  ): StrengthResponse {
    val
    strength = strengthRepository.getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      ?: throw StrengthNotFoundException(prisonNumber, strengthReference)

    if (!strength.active) {
      throw StrengthArchivedException(prisonNumber, strengthReference)
    }

    strength.symptoms = request.symptoms
    strength.howIdentified = IdentificationSourceMapper.toEntity(request.howIdentified)
    strength.howIdentifiedOther = request.howIdentifiedOther
    strength.updatedAtPrison = request.prisonId

    return strengthMapper.toModel(strengthRepository.save(strength))
  }

  fun getStrength(
    prisonNumber: String,
    strengthReference: UUID,
  ): StrengthResponse {
    val
    strength = strengthRepository.getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      ?: throw StrengthNotFoundException(prisonNumber, strengthReference)

    return strengthMapper.toModel(strength)
  }

  @Transactional
  fun archiveStrength(
    prisonNumber: String,
    strengthReference: UUID,
    request: ArchiveStrengthRequest,
  ) {
    val strength = strengthRepository.getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      ?: throw StrengthNotFoundException(prisonNumber, strengthReference)

    if (!strength.active) {
      throw StrengthArchivedException(prisonNumber, strengthReference)
    }
    // only non screener strengths can be archived:
    if (strength.alnScreenerId != null) {
      throw StrengthAlnScreenerException(prisonNumber, strengthReference)
    }

    strength.active = false
    strength.archiveReason = request.archiveReason
    strength.updatedAtPrison = request.prisonId
    strengthRepository.save(strength)
  }

  @Transactional
  @TimelineEvent(
    eventType = STRENGTH_DELETED,
    additionalInfoField = "strengthReference,reason",
  )
  fun deleteStrength(prisonNumber: String, strengthReference: UUID, prisonId: String, reason: DeletionReason) {
    val deletionReason = DeletionReasonMapper.toEntity(reason)

    val strength = strengthRepository.getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      ?: throw StrengthNotFoundException(prisonNumber, strengthReference)

    // only non-screener (manually-added) strengths can be deleted
    if (strength.alnScreenerId != null) {
      throw StrengthAlnScreenerException(prisonNumber, strengthReference)
    }

    strengthRepository.delete(strength)
    log.info("Processed Delete strength for $prisonNumber (reason=$deletionReason)")
  }

  @Transactional
  fun archiveAllScreenerStrengths(prisonerNumber: String) {
    val alnStrengths = strengthRepository.findAllByPrisonNumberAndAlnScreenerIdIsNotNull(prisonerNumber)
    alnStrengths.forEach {
      it.active = false
      it.archiveReason = "Old screener"
    }
    strengthRepository.saveAll(alnStrengths)
  }
}

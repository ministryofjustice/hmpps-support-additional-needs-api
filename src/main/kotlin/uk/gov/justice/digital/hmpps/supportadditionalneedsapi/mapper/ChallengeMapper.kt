package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import java.time.LocalDate
import java.util.SortedSet
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource as IdentificationSourceEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

@Component
class ChallengeMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: ChallengeEntity,
    screenerDate: LocalDate? = null,
  ): ChallengeResponse = with(entity) {
    ChallengeResponse(
      fromALNScreener = fromALNScreener,
      reference = reference,
      createdBy = createdBy!!,
      createdByDisplayName = userService.getUserDetails(createdBy!!).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy!!,
      updatedByDisplayName = userService.getUserDetails(updatedBy!!).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      challengeType = challengeType.toModel(),
      symptoms = symptoms,
      howIdentified = toModel(howIdentified),
      howIdentifiedOther = howIdentifiedOther,
      alnScreenerDate = if (fromALNScreener) screenerDate else null,
      active = active,
      archiveReason = archiveReason,
    )
  }

  private fun toModel(identificationSources: Set<IdentificationSourceEntity>): List<IdentificationSourceModel>? = identificationSources
    .takeIf { it.isNotEmpty() }
    ?.map { toModel(it) }

  private fun toModel(source: IdentificationSourceEntity): IdentificationSourceModel = when (source) {
    IdentificationSourceEntity.EDUCATION_SKILLS_WORK -> IdentificationSourceModel.EDUCATION_SKILLS_WORK
    IdentificationSourceEntity.WIDER_PRISON -> IdentificationSourceModel.WIDER_PRISON
    IdentificationSourceEntity.CONVERSATIONS -> IdentificationSourceModel.CONVERSATIONS
    IdentificationSourceEntity.COLLEAGUE_INFO -> IdentificationSourceModel.COLLEAGUE_INFO
    IdentificationSourceEntity.FORMAL_PROCESSES -> IdentificationSourceModel.FORMAL_PROCESSES
    IdentificationSourceEntity.SELF_DISCLOSURE -> IdentificationSourceModel.SELF_DISCLOSURE
    IdentificationSourceEntity.OTHER_SCREENING_TOOL -> IdentificationSourceModel.OTHER_SCREENING_TOOL
    IdentificationSourceEntity.OTHER -> IdentificationSourceModel.OTHER
  }

  fun toEntity(identificationSources: List<IdentificationSourceModel>?): SortedSet<IdentificationSourceEntity> = identificationSources
    ?.map { toEntity(it) }
    ?.toSortedSet()
    ?: sortedSetOf()

  private fun toEntity(source: IdentificationSourceModel): IdentificationSourceEntity = when (source) {
    IdentificationSourceModel.EDUCATION_SKILLS_WORK -> IdentificationSourceEntity.EDUCATION_SKILLS_WORK
    IdentificationSourceModel.WIDER_PRISON -> IdentificationSourceEntity.WIDER_PRISON
    IdentificationSourceModel.CONVERSATIONS -> IdentificationSourceEntity.CONVERSATIONS
    IdentificationSourceModel.COLLEAGUE_INFO -> IdentificationSourceEntity.COLLEAGUE_INFO
    IdentificationSourceModel.FORMAL_PROCESSES -> IdentificationSourceEntity.FORMAL_PROCESSES
    IdentificationSourceModel.SELF_DISCLOSURE -> IdentificationSourceEntity.SELF_DISCLOSURE
    IdentificationSourceModel.OTHER_SCREENING_TOOL -> IdentificationSourceEntity.OTHER_SCREENING_TOOL
    IdentificationSourceModel.OTHER -> IdentificationSourceEntity.OTHER
  }
}

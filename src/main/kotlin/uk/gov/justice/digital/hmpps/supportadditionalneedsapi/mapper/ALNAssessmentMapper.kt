package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNAssessmentResponse

@Component
class ALNAssessmentMapper(
  private val instantMapper: InstantMapper,
) {

  fun toModel(entity: AlnAssessmentEntity): ALNAssessmentResponse = with(entity) {
    ALNAssessmentResponse(
      reference = reference,
      prisonNumber = prisonNumber,
      hasNeed = hasNeed,
      screeningDate = screeningDate,
      curiousReference = curiousReference,
      createdBy = createdBy!!,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      updatedBy = updatedBy!!,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
    )
  }
}

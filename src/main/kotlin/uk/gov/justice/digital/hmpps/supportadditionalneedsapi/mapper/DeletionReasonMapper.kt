package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.DeletionReason as DeletionReasonEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason as DeletionReasonModel

object DeletionReasonMapper {

  fun toEntity(reason: DeletionReasonModel): DeletionReasonEntity = when (reason) {
    DeletionReasonModel.DATA_PROCESSING_OBJECTION -> DeletionReasonEntity.DATA_PROCESSING_OBJECTION
    DeletionReasonModel.ENTERED_IN_ERROR -> DeletionReasonEntity.ENTERED_IN_ERROR
  }
}

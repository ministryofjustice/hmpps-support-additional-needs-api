package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import org.springframework.stereotype.Component
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ManageUserService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus as PlanCreationStatusEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationStatus as PlanCreationStatusModel

@Component
class PlanCreationScheduleHistoryMapper(
  private val instantMapper: InstantMapper,
  private val userService: ManageUserService,
) {

  fun toModel(
    entity: PlanCreationScheduleHistoryEntity,
  ): PlanCreationScheduleResponse = with(entity) {
    PlanCreationScheduleResponse(
      reference = reference,
      deadlineDate = deadlineDate,
      status = toPlanCreationStatus(status),
      createdBy = createdBy,
      createdByDisplayName = userService.getUserDetails(createdBy).name,
      createdAt = instantMapper.toOffsetDateTime(createdAt)!!,
      createdAtPrison = createdAtPrison,
      updatedBy = updatedBy,
      updatedByDisplayName = userService.getUserDetails(updatedBy).name,
      updatedAt = instantMapper.toOffsetDateTime(updatedAt)!!,
      updatedAtPrison = updatedAtPrison,
      exemptionReason = exemptionReason,
      version = version,
    )
  }

  private fun toPlanCreationStatus(status: PlanCreationStatusEntity): PlanCreationStatusModel = when (status) {
    PlanCreationStatusEntity.SCHEDULED -> PlanCreationStatusModel.SCHEDULED
    PlanCreationStatusEntity.COMPLETED -> PlanCreationStatusModel.COMPLETED
    PlanCreationStatusEntity.EXEMPT_SYSTEM_TECHNICAL_ISSUE -> PlanCreationStatusModel.EXEMPT_SYSTEM_TECHNICAL_ISSUE
    PlanCreationStatusEntity.EXEMPT_PRISONER_TRANSFER -> PlanCreationStatusModel.EXEMPT_PRISONER_TRANSFER
    PlanCreationStatusEntity.EXEMPT_PRISONER_RELEASE -> PlanCreationStatusModel.EXEMPT_PRISONER_RELEASE
    PlanCreationStatusEntity.EXEMPT_PRISONER_DEATH -> PlanCreationStatusModel.EXEMPT_PRISONER_DEATH
    PlanCreationStatusEntity.EXEMPT_PRISONER_MERGE -> PlanCreationStatusModel.EXEMPT_PRISONER_MERGE
    PlanCreationStatusEntity.EXEMPT_PRISONER_NOT_COMPLY -> PlanCreationStatusModel.EXEMPT_PRISONER_NOT_COMPLY
    PlanCreationStatusEntity.EXEMPT_NOT_IN_EDUCATION -> PlanCreationStatusModel.EXEMPT_NOT_IN_EDUCATION
    PlanCreationStatusEntity.EXEMPT_UNKNOWN -> PlanCreationStatusModel.EXEMPT_UNKNOWN
  }
}

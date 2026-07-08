package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.DataDeletionEventEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.DataDeletionEventRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.DeletionReasonMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason

/**
 * Service class exposing functionality for Data Deletion Events
 */
@Service
class DataDeletionEventService(
  private val dataDeletionEventRepository: DataDeletionEventRepository,
) {

  @Transactional
  fun recordDataDeletionEvent(prisonNumber: String, prisonId: String, reason: DeletionReason) {
    val entity = DataDeletionEventEntity(
      prisonNumber = prisonNumber,
      reason = DeletionReasonMapper.toEntity(reason),
      dataDeletedAtPrison = prisonId,
    )
    dataDeletionEventRepository.save(entity)
  }
}

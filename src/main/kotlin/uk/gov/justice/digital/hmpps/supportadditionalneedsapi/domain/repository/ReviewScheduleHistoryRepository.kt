package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import java.util.UUID

@Repository
interface ReviewScheduleHistoryRepository : JpaRepository<ReviewScheduleHistoryEntity, UUID> {

  @Query("SELECT MAX(h.version) FROM ReviewScheduleHistoryEntity h WHERE h.reference = :reviewScheduleReference")
  fun findMaxVersionByReviewScheduleReference(reviewScheduleReference: UUID): Int?

  fun findAllByReference(reviewScheduleReference: UUID): List<ReviewScheduleHistoryEntity>

  fun findAllByPrisonNumber(prisonNumber: String): List<ReviewScheduleHistoryEntity>
  fun getAllByPrisonNumber(prisonNumber: String, pageable: Pageable): Page<ReviewScheduleHistoryEntity>
}

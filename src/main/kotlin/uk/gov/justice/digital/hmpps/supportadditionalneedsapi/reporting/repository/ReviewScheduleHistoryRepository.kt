package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import java.util.UUID

@Repository
interface ReviewScheduleHistoryRepository : JpaRepository<ReviewScheduleHistoryEntity, UUID> {
  fun findAllByPrisonNumber(prisonNumber: String): List<ReviewScheduleHistoryEntity>
}

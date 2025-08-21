package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import java.util.UUID

@Repository
interface ReviewScheduleRepository : JpaRepository<ReviewScheduleEntity, UUID> {
  fun findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber: String): ReviewScheduleEntity?
  fun findAllByPrisonNumber(prisonNumber: String): List<ReviewScheduleEntity>
}

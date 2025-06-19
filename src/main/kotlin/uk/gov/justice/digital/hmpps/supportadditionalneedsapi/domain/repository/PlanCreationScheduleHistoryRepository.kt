package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleHistoryEntity
import java.util.UUID

@Repository
interface PlanCreationScheduleHistoryRepository : JpaRepository<PlanCreationScheduleHistoryEntity, UUID> {

  @Query("SELECT MAX(h.version) FROM PlanCreationScheduleHistoryEntity h WHERE h.reference = :scheduleReference")
  fun findMaxVersionByScheduleReference(scheduleReference: UUID?): Int?

  fun findAllByReference(planCreationScheduleReference: UUID): List<PlanCreationScheduleHistoryEntity>

  fun findAllByPrisonNumber(prisonNumber: String): List<PlanCreationScheduleHistoryEntity>
}

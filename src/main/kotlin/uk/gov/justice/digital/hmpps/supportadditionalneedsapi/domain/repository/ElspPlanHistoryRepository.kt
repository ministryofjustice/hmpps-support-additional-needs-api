package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import java.util.UUID

@Repository
interface ElspPlanHistoryRepository : JpaRepository<ElspPlanHistoryEntity, UUID> {
  fun findAllByPrisonNumber(prisonNumber: String): List<ElspPlanHistoryEntity>
}

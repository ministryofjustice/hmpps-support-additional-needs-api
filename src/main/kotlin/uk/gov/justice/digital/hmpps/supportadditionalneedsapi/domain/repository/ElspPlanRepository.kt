package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.EntityGraph
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import java.util.UUID

@Repository
interface ElspPlanRepository : JpaRepository<ElspPlanEntity, UUID> {
  @EntityGraph(attributePaths = ["otherContributors"])
  fun findByPrisonNumber(prisonNumber: String): ElspPlanEntity?
  fun existsByPrisonNumber(prisonNumber: String): Boolean
}

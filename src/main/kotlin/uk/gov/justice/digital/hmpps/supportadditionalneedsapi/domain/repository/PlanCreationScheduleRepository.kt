package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import java.util.UUID

@Repository
interface PlanCreationScheduleRepository : JpaRepository<PlanCreationScheduleEntity, UUID> {

  fun findByPrisonNumber(prisonNumber: String): PlanCreationScheduleEntity?
}

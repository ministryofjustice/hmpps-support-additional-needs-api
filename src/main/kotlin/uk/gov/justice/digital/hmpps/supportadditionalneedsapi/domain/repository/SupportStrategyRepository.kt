package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import java.util.UUID

@Repository
interface ConditionRepository : JpaRepository<ConditionEntity, UUID> {

  fun findAllByPrisonNumber(prisonNumber: String): List<ConditionEntity>

  fun getConditionEntityByPrisonNumberAndReference(prisonNumber: String, reference: UUID): ConditionEntity?
}

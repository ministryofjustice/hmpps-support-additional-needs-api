package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import java.util.UUID

@Repository
interface SupportStrategyRepository : JpaRepository<SupportStrategyEntity, UUID> {

  fun findAllByPrisonNumber(prisonNumber: String): List<SupportStrategyEntity>

  fun getSupportStrategyEntityByPrisonNumberAndReference(prisonNumber: String, reference: UUID): SupportStrategyEntity?
}

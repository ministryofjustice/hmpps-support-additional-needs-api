package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EhcpStatusHistoryEntity
import java.util.UUID

@Repository
interface EhcpStatusHistoryRepository : JpaRepository<EhcpStatusHistoryEntity, UUID> {
  fun findAllByPrisonNumber(prisonNumber: String): List<EhcpStatusHistoryEntity>
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import java.util.UUID

@Repository
interface AlnScreenerRepository : JpaRepository<ALNScreenerEntity, UUID> {

  fun getAllByPrisonNumber(prisonNumber: String): List<ALNScreenerEntity>
  fun findByPrisonNumber(prisonNumber: String): MutableList<ALNScreenerEntity>
  fun findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber: String): ALNScreenerEntity?
}

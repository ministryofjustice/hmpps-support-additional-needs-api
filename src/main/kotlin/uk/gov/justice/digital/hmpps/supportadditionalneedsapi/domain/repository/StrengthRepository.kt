package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import java.util.UUID

@Repository
interface StrengthRepository : JpaRepository<StrengthEntity, UUID> {
  fun findAllByPrisonNumber(prisonNumber: String): List<StrengthEntity>
  fun getStrengthEntityByPrisonNumberAndReference(prisonNumber: String, reference: UUID): StrengthEntity?
}

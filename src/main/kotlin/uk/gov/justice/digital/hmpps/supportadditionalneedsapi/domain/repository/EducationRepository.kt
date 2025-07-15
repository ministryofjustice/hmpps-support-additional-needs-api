package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import java.util.UUID

@Repository
interface EducationRepository : JpaRepository<EducationEntity, UUID> {
  fun findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber: String): EducationEntity?
}

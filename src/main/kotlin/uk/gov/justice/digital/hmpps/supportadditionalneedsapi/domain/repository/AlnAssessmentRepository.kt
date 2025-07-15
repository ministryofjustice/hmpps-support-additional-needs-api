package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import java.util.UUID

@Repository
interface AlnAssessmentRepository : JpaRepository<AlnAssessmentEntity, UUID> {
  fun findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber: String): AlnAssessmentEntity?
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.LddAssessmentEntity
import java.util.UUID

@Repository
interface LddAssessmentRepository : JpaRepository<LddAssessmentEntity, UUID> {

  fun getAllByPrisonNumber(prisonNumber: String): List<LddAssessmentEntity>
  fun findByPrisonNumber(prisonNumber: String): MutableList<LddAssessmentEntity>
  fun findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber: String): LddAssessmentEntity?
}

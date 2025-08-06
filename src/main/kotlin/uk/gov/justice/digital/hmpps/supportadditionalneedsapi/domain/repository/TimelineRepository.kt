package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEntity
import java.util.UUID

@Repository
interface TimelineRepository : JpaRepository<TimelineEntity, UUID> {
  fun findAllByPrisonNumberOrderByCreatedAt(prisonNumber: String): List<TimelineEntity>
}

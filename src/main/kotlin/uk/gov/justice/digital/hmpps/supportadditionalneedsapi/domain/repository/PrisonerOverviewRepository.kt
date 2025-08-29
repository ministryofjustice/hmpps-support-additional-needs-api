package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PrisonerOverviewEntity

@Repository
interface PrisonerOverviewRepository : JpaRepository<PrisonerOverviewEntity, String> {
  fun findByPrisonNumberIn(prisonNumbers: List<String>): List<PrisonerOverviewEntity>
  fun findByPrisonNumber(prisonNumber: String): PrisonerOverviewEntity?
}

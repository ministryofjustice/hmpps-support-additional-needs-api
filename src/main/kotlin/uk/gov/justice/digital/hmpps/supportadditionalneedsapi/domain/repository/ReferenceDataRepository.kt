package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import java.util.*

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceDataEntity, UUID> {
  fun findByKeyDomainOrderByListSequenceAsc(domain: Domain): Collection<ReferenceDataEntity>
}

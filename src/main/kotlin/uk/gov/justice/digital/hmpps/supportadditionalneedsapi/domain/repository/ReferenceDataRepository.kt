package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.InvalidInputException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.NotActiveException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.verify
import java.util.UUID

@Repository
interface ReferenceDataRepository : JpaRepository<ReferenceDataEntity, UUID> {
  fun findByKeyDomainOrderByListSequenceAsc(domain: Domain): Collection<ReferenceDataEntity>

  fun findByKeyDomainAndDefaultForCategoryIsTrueOrderByListSequenceAsc(domain: Domain): Collection<ReferenceDataEntity>

  fun findByKey(key: ReferenceDataKey): ReferenceDataEntity?
}

fun <T, E : RuntimeException> verifyExists(value: T?, exception: () -> E): T = value ?: throw exception()

fun ReferenceDataRepository.validateReferenceData(key: ReferenceDataKey) = verifyExists(findByKey(key)) {
  InvalidInputException(key.domain.name, key.code)
}.also {
  verify(it.isActive()) { NotActiveException(key.domain.name, key.code) }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReferenceData

@Service
@Transactional
class ReferenceDataService(
  private val referenceDataRepository: ReferenceDataRepository,
) {
  fun getReferenceDataForDomain(domain: Domain, includeInactive: Boolean): List<ReferenceData> = referenceDataRepository.findByKeyDomainOrderByListSequenceAsc(domain).filter { includeInactive || it.isActive() }
    .map {
      ReferenceData(it.code, it.description, it.listSequence, it.isActive())
    }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationRepository
import java.util.*

@Service
class EducationService(private val educationRepository: EducationRepository) {

  /**
   * Establish whether this person is currently in eduction.
   * This is managed by curious, but we will process messages from curious maintaining a cache
   * of the persons current education status.
   */
  fun inEducation(prisonNumber: String): Boolean = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.inEducation ?: false

  /**
   * Create the education record. Currently, this is simply whether the person is in education with the
   * latest record being the current status.
   * We record this record when we receive an education message from Curious.
   */
  fun recordEducationRecord(prisonNumber: String, inEducation: Boolean, curiousReference: UUID) {
    educationRepository.save(
      EducationEntity(
        inEducation = inEducation,
        prisonNumber = prisonNumber,
        curiousReference = curiousReference,
      ),
    )
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.EducationDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class EducationService(
  private val educationRepository: EducationRepository,
  private val curiousApiClient: CuriousApiClient,
) {

  /**
   * Establish whether this person is currently in eduction.
   * This is managed by curious, but we will process messages from curious maintaining a cache
   * of the persons current education status.
   */
  fun inEducation(prisonNumber: String): Boolean =
    educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.inEducation ?: false

  /**
   * Create the education record. Currently, this is simply whether the person is in education with the
   * latest record being the current status.
   * We record this record when we receive an education message from Curious.
   */
  @Transactional
  fun recordEducationRecord(prisonNumber: String, inEducation: Boolean, curiousReference: UUID, latestStartDate: LocalDate = LocalDate.now()) {
    educationRepository.save(
      EducationEntity(
        inEducation = inEducation,
        prisonNumber = prisonNumber,
        curiousReference = curiousReference,
        latestStartDate = latestStartDate
      ),
    )
  }

  @Transactional
  fun processEducationStatusUpdate(inboundEvent: InboundEvent, info: EducationStatusUpdateAdditionalInformation) {
    val prisonNumber = inboundEvent.prisonNumber()
    log.info("Processing education status update event: ${inboundEvent.description} for $prisonNumber. Detail URL: ${inboundEvent.detailUrl}, reference: ${info.curiousExternalReference}")

    log.info("Retrieving education info for $prisonNumber")
    val curiousEducationDTO = curiousApiClient.getEducation(prisonNumber)
    val (isInEducation, latestStartDate) = educationStatusPair(curiousEducationDTO)
    val sanEducationRecord = educationRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    if (isInEducation && latestStartDate != null) {
      val initialSanEducationRecord = sanEducationRecord == null
      val educationStatusChanged = sanEducationRecord != null && !sanEducationRecord.inEducation
      val isNewCourse = sanEducationRecord?.inEducation == true && latestStartDate.isAfter(sanEducationRecord.latestStartDate)

      if (initialSanEducationRecord || educationStatusChanged || isNewCourse) {
        recordEducationRecord(
          prisonNumber = prisonNumber,
          inEducation = true,
          curiousReference = info.curiousExternalReference,
          latestStartDate = latestStartDate
        )
      }
    } else {
      // Explicitly handle when person is no longer in education
      val needsUpdate = sanEducationRecord == null || sanEducationRecord.inEducation
      if (needsUpdate) {
        recordEducationRecord(
          prisonNumber = prisonNumber,
          inEducation = false,
          curiousReference = info.curiousExternalReference,
        )
      }
    }

    log.info("Retrieved education info for $prisonNumber: $curiousEducationDTO")
  }


  private fun educationStatusPair(curiousEducationDTO: EducationDTO?): Pair<Boolean, LocalDate?> {
    // Return true or false if the person is in education,
    // and if true, return the latest start date of any current education records
    val activeCourses = curiousEducationDTO
      ?.content
      ?.filter { it?.learningActualEndDate == null }
      ?.mapNotNull { it?.learningStartDate }

    return if (!activeCourses.isNullOrEmpty()) {
      Pair(true, activeCourses.maxOrNull())
    } else {
      Pair(false, null)
    }
  }
}

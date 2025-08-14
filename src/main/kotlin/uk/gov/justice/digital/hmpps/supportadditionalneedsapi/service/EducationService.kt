package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.context.annotation.Lazy
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.EducationDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CURIOUS_EDUCATION_TRIGGER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class EducationService(
  private val educationRepository: EducationRepository,
  private val curiousApiClient: CuriousApiClient,
  private val educationEnrolmentRepository: EducationEnrolmentRepository,
  private val needService: NeedService,
  private val reviewScheduleService: ReviewScheduleService,
  @Lazy private val planCreationScheduleService: PlanCreationScheduleService,
  private val elspPlanRepository: ElspPlanRepository,
) {

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
  @Transactional
  fun recordEducationRecord(prisonNumber: String, inEducation: Boolean, curiousReference: UUID) {
    educationRepository.save(
      EducationEntity(
        inEducation = inEducation,
        prisonNumber = prisonNumber,
        curiousReference = curiousReference,
      ),
    )
  }

  @Transactional
  @TimelineEvent(
    eventType = CURIOUS_EDUCATION_TRIGGER,
    additionalInfoPrefix = "curiousReference:",
    additionalInfoField = "curiousExternalReference",
  )
  fun processEducationStatusUpdate(
    prisonNumber: String,
    info: EducationStatusUpdateAdditionalInformation,
    inboundEvent: InboundEvent,
  ) {
    log.info(
      "processing education status update event: {${inboundEvent.description}} for ${inboundEvent.prisonNumber()} \n " +
        "Detail URL: ${inboundEvent.detailUrl}" +
        ", reference: ${info.curiousExternalReference}",
    )
    log.info("retrieving current education info for $prisonNumber")
    val educationDto = curiousApiClient.getEducation(prisonNumber = prisonNumber)
    log.info("retrieved current education info for $prisonNumber : $educationDto")

    // save the education record if it has changed.
    val inEducation = recordOverallEducationStatus(educationDto, prisonNumber, info)
    // Record the education enrolment if any have changed
    val enrolmentDiff = updateSanEnrolments(
      educationDto = educationDto,
      prisonNumber = prisonNumber,
      curiousRef = info.curiousExternalReference,
    )
    log.info("Enrolment diff for $prisonNumber: $enrolmentDiff")

    if (enrolmentDiff.anyChanges) {
      if (!inEducation) {
        // exempt any schedules
        // this will exempt schedules if they exist AND sent messages to MN.
        planCreationScheduleService.exemptSchedule(prisonNumber, PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
        reviewScheduleService.exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
      }
      if (needService.hasNeed(prisonNumber)) {
        // find out if this is a new enrolment
        if (enrolmentDiff.createdCount > 0) {
          // does the person have an ELSP?
          val plan = elspPlanRepository.findByPrisonNumber(prisonNumber)
          if (plan == null) {
            // create the plan creation schedule
            val startDate = enrolmentDiff.firstNewEnrolmentStart
            val newEducation = findNewlyActiveEducationForStart(educationDto, startDate!!)
            planCreationScheduleService.createOrUpdate(prisonNumber, startDate, newEducation!!.fundingType)
          } else {
            // make an update to the review
          }
        }
        log.info("education was changed and the person had a need so updating schedules as appropriate for $prisonNumber")
      }
    }
  }

  private fun findNewlyActiveEducationForStart(dto: EducationDTO, start: LocalDate): Education? = dto.educationData.firstOrNull { it.learningStartDate == start && it.learningActualEndDate == null }

  // This sets the overall education status for the person
  // this is used in a number of places for instance in the person search results page
  @Transactional
  fun recordOverallEducationStatus(
    educationDto: EducationDTO,
    prisonNumber: String,
    info: EducationStatusUpdateAdditionalInformation,
  ): Boolean {
    val currentlyInEducation = educationDto.educationData.any { it.isActive() }
    val previouslyInEducation = inEducation(prisonNumber)

    if (currentlyInEducation != previouslyInEducation) {
      recordEducationRecord(
        prisonNumber = prisonNumber,
        inEducation = currentlyInEducation,
        curiousReference = info.curiousExternalReference,
      )
      log.info("Education status changed for $prisonNumber: $previouslyInEducation -> $currentlyInEducation (recorded).")
    } else {
      log.info("Education status unchanged for $prisonNumber: $currentlyInEducation (no new record).")
    }
    return currentlyInEducation
  }

  @Transactional
  fun updateSanEnrolments(
    educationDto: EducationDTO,
    prisonNumber: String,
    curiousRef: UUID,
  ): EnrolmentProcessingResults {
    // create a key for all curious records
    val allCuriousByKey: Map<EnrolmentKey, Education> =
      educationDto.educationData.associateBy { it.key() }

    // create a set of only the currently-active keys from Curious
    val activeKeysFromCurious: Set<EnrolmentKey> =
      educationDto.educationData
        .asSequence()
        .filter { it.isActive() }
        .map { it.key() }
        .toSet()

    // get the SAN open education list and create a list of keys
    val openInDb = educationEnrolmentRepository.findAllByPrisonNumberAndEndDateIsNull(prisonNumber)
    val openByKey =
      openInDb.associateBy { EnrolmentKey(it.establishmentId, it.qualificationCode, it.learningStartDate) }
    val openKeysInDb = openByKey.keys

    // work out which ones have no ended
    val newKeys = activeKeysFromCurious - openKeysInDb
    val endedKeys = openKeysInDb - activeKeysFromCurious

    // create new enrolment records
    val createdEntities = mutableListOf<EducationEnrolmentEntity>()
    newKeys.forEach { key ->
      val src = allCuriousByKey.getValue(key) // safe: key came from Curious
      val entity = EducationEnrolmentEntity(
        prisonNumber = prisonNumber,
        establishmentId = key.establishmentId,
        qualificationCode = key.qualificationCode,
        learningStartDate = key.start,
        plannedEndDate = src.learningPlannedEndDate,
        fundingType = src.fundingType,
        completionStatus = src.completionStatus,
        endDate = null,
        lastCuriousReference = curiousRef,
      )
      createdEntities += educationEnrolmentRepository.save(entity)
    }

    // end any enrolments in SAN
    var endedCount = 0
    endedKeys.forEach { key ->
      val entity = openByKey.getValue(key)
      val src = allCuriousByKey[key]
      val endDate = src?.learningActualEndDate ?: LocalDate.now()
      if (entity.endDate != endDate || entity.completionStatus != src?.completionStatus) {
        entity.endDate = endDate
        entity.completionStatus = src?.completionStatus ?: entity.completionStatus
        entity.lastCuriousReference = curiousRef
        educationEnrolmentRepository.save(entity)
        endedCount++
      }
    }

    // create summary of what changed.
    val hasActiveEnrolmentsAfter =
      educationEnrolmentRepository.findAllByPrisonNumberAndEndDateIsNull(prisonNumber).isNotEmpty()

    val firstStart = createdEntities
      .minByOrNull { it.learningStartDate }
      ?.learningStartDate

    val result = EnrolmentProcessingResults(
      createdCount = createdEntities.size,
      closedCount = endedCount,
      anyChanges = createdEntities.isNotEmpty() || endedCount > 0,
      hasActiveEnrolments = hasActiveEnrolmentsAfter,
      firstNewEnrolmentStart = firstStart,
    )

    log.info(
      "Enrolment updated for $prisonNumber - created=${result.createdCount}, " +
        "closed=${result.closedCount}, activeNow=${result.hasActiveEnrolments}",
    )
    return result
  }

  data class EnrolmentProcessingResults(
    val createdCount: Int,
    val closedCount: Int,
    val anyChanges: Boolean,
    val hasActiveEnrolments: Boolean,
    val firstNewEnrolmentStart: LocalDate?,
  )

  private fun Education.isActive(): Boolean = this.learningActualEndDate == null

  fun Education.key(): EnrolmentKey = EnrolmentKey(establishmentId!!, qualificationCode, learningStartDate)

  data class EnrolmentKey(val establishmentId: String, val qualificationCode: String, val start: LocalDate)
}

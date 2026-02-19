package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.EducationDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.DEFAULT_PRISON_ID
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CURIOUS_EDUCATION_TRIGGER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
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
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val educationEnrolmentRepository: EducationEnrolmentRepository,
  private val needService: NeedService,
  private val reviewScheduleService: ReviewScheduleService,
  private val planCreationScheduleService: PlanCreationScheduleService,
  private val elspPlanRepository: ElspPlanRepository,
  private val alnAssessmentRepository: AlnAssessmentRepository,
) {

  fun hasActiveEducationEnrollment(prisonNumber: String): Boolean = educationEnrolmentRepository.existsWithNoEndDate(prisonNumber)

  fun getNonPESEducationStartDate(prisonNumber: String): LocalDate? = educationEnrolmentRepository.findEarliestLearningStartDateWithNoEndDate(prisonNumber)

  /**
   * Create the education record. Currently, this is simply whether the person is in education with the
   * latest record being the current status.
   * We record this record when we receive an education message from Curious.
   */
  @Transactional
  fun recordEducationRecord(prisonNumber: String, inEducation: Boolean, curiousReference: UUID?) {
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
    val currentEstablishment = getCurrentLocation(prisonNumber)
    log.info { "Current establishment: $currentEstablishment for $prisonNumber" }
    log.info { "Education update - ${inboundEvent.description} for $prisonNumber" }
    if (inboundEvent.description == "EDUCATION_STOPPED") {
      endNonCurrentEducationEnrollments(prisonNumber, currentEstablishment)
    }
    log.info(
      "processing education status update event: {${inboundEvent.description}} for ${inboundEvent.prisonNumber()} \n " +
        "Detail URL: ${inboundEvent.detailUrl}" +
        ", reference: ${info.curiousExternalReference}",
    )
    log.info("retrieving current education info for $prisonNumber")
    val educationDto = curiousApiClient.getEducation(prisonNumber = prisonNumber)
    log.info("retrieved current education info for $prisonNumber : $educationDto")

    val filteredEducationDto = educationDto.copy(
      educationData = educationDto.educationData.filter { it.establishmentId == currentEstablishment },
    )

    // save the education record if it has changed.
    val inEducation = recordOverallEducationStatus(filteredEducationDto, prisonNumber, info)
    // Record the education enrolment if any have changed
    val enrolmentDiff = updateSanEnrolments(
      educationDto = filteredEducationDto,
      prisonNumber = prisonNumber,
      curiousRef = info.curiousExternalReference,
    )
    log.info("Enrolment diff for $prisonNumber: $enrolmentDiff")

    if (enrolmentDiff.anyChanges) {
      if (!inEducation) {
        log.info("Prisoner not in education exempt schedules $prisonNumber")
        // exempt any schedules
        // this will exempt schedules if they exist AND sent messages to MN.
        val prisonId = getPrisonIdForLatestInActiveEducation(filteredEducationDto)
        planCreationScheduleService.exemptSchedule(
          prisonNumber,
          PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
          prisonId = prisonId,
        )
        reviewScheduleService.exemptSchedule(
          prisonNumber,
          ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
          prisonId = prisonId,
        )
      }
      if (needService.hasNeed(prisonNumber)) {
        log.info("Prisoner has need $prisonNumber")
        // find out if this is a new enrolment
        if (enrolmentDiff.createdCount > 0 || enrolmentDiff.reopenedCount > 0) {
          log.info("New or reopened education $prisonNumber")
          // does the person have an ELSP?
          val plan = elspPlanRepository.findByPrisonNumber(prisonNumber)
          val startDate = enrolmentDiff.firstNewEnrolmentStart
          val newEducation = findNewlyActiveEducationForStart(filteredEducationDto, startDate!!)
          val prisonId = getPrisonIdForLatestActiveEducation(filteredEducationDto)

          if (plan == null) {
            val subjectToKPIRules = subjectToKPIRules(prisonNumber = prisonNumber, enrolmentDiff = enrolmentDiff)
            // create the plan creation schedule
            planCreationScheduleService.createOrUpdateDueToEducationUpdate(
              prisonNumber,
              startDate,
              newEducation!!.fundingType,
              subjectToKPIRules,
              prisonId = prisonId,
            )
          } else {
            // make an update to the review
            reviewScheduleService.createOrUpdateDueToEducationUpdate(
              prisonNumber,
              startDate,
              newEducation!!.fundingType,
              prisonId = prisonId,
            )
          }
        }
        log.info("education was changed and the person had a need so updating schedules as appropriate for $prisonNumber")
      }
    }
  }

  fun endNonCurrentEducationEnrollments(prisonNumber: String, currentEstablishment: String) {
    // close all education records for all establishments that are not the same as the current establishment
    log.info("Ending current education enrollments for $prisonNumber that are not currently in establishment $currentEstablishment")
    val educationEnrollments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    educationEnrollments.filter { it.establishmentId != currentEstablishment }.forEach { it.endDate = LocalDate.now() }
    educationEnrolmentRepository.saveAll(educationEnrollments)
  }

  private fun getCurrentLocation(prisonNumber: String): String = prisonerSearchApiClient.getPrisoner(prisonNumber).prisonId ?: "N/A"

  // Attempt to get the prisonId from the education returned from Curious
  private fun getPrisonIdForLatestActiveEducation(educationDto: EducationDTO): String = educationDto.educationData
    .firstOrNull { it.learningActualEndDate == null }
    ?.establishmentId
    ?.takeIf { it.length == 3 }
    ?: DEFAULT_PRISON_ID

  private fun getPrisonIdForLatestInActiveEducation(educationDto: EducationDTO): String = educationDto.educationData
    .firstOrNull { it.learningActualEndDate != null }
    ?.establishmentId
    ?.takeIf { it.length == 3 }
    ?: DEFAULT_PRISON_ID

  fun subjectToKPIRules(
    prisonNumber: String,
    enrolmentDiff: EnrolmentProcessingResults,
  ): Boolean {
    val needSources = needService.getNeedSources(prisonNumber)

    // Special case: only ALN screener need
    if (needSources.size == 1 && NeedSource.ALN_SCREENER in needSources) {
      val screeningDate = alnAssessmentRepository
        .findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
        ?.screeningDate

      // Special rule only when the ALN screener has been processed but was after the education start date.
      // The prisoner will be marked as has need when the education record is processed but shouldn't make the person
      // in scope for KPI.
      val enrolmentStart = enrolmentDiff.firstNewEnrolmentStart
      if (screeningDate != null && enrolmentStart != null) {
        // Not subject to KPI if enrolment starts before the screening date
        if (enrolmentStart < screeningDate) return false
      }
    }

    // Not subject to KPI if the person has any other need sources than ALN_SCREENER
    return NeedSource.ALN_SCREENER in needSources
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
    val previouslyInEducation = hasActiveEducationEnrollment(prisonNumber)

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
    val allInDb = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    val allByKey = allInDb.associateBy { EnrolmentKey(it.establishmentId, it.qualificationCode, it.learningStartDate) }

    val openByKey = allInDb
      .asSequence()
      .filter { it.endDate == null }
      .associateBy { EnrolmentKey(it.establishmentId, it.qualificationCode, it.learningStartDate) }
    val openKeysInDb = openByKey.keys

    // work out which ones have no end date
    val newOrReopenedKeys = activeKeysFromCurious - openKeysInDb
    val endedKeys = openKeysInDb - activeKeysFromCurious

    // create new enrolment records or update existing if they have changed
    val createdOrUpdatedEntities = mutableListOf<EducationEnrolmentEntity>()
    var reopenedCount = 0
    var createdCount = 0
    newOrReopenedKeys.forEach { key ->
      val src = allCuriousByKey.getValue(key) // key from Curious

      val existingOrClosed = allByKey[key] // could be open or closed
      if (existingOrClosed?.endDate != null) {
        existingOrClosed.endDate = null
        existingOrClosed.lastCuriousReference = curiousRef
        createdOrUpdatedEntities += educationEnrolmentRepository.save(existingOrClosed)
        reopenedCount++
      } else {
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
        createdOrUpdatedEntities += educationEnrolmentRepository.save(entity)
        createdCount++
      }
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

    val firstStart = createdOrUpdatedEntities
      .minByOrNull { it.learningStartDate }
      ?.learningStartDate

    val result = EnrolmentProcessingResults(
      createdCount = createdCount,
      closedCount = endedCount,
      anyChanges = createdCount > 0 || endedCount > 0 || reopenedCount > 0,
      hasActiveEnrolments = hasActiveEnrolmentsAfter,
      firstNewEnrolmentStart = firstStart,
      reopenedCount = reopenedCount,
    )

    log.info(
      "Enrolment updated for $prisonNumber - created=${result.createdCount}, reopened=${result.reopenedCount}, " +
        "closed=${result.closedCount}, activeNow=${result.hasActiveEnrolments}",
    )
    return result
  }

  data class EnrolmentProcessingResults(
    val reopenedCount: Int,
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

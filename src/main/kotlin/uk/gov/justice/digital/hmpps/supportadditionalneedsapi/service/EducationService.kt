package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.CURIOUS_EDUCATION_TRIGGER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent
import java.time.Clock
import java.time.LocalDate
import java.util.UUID

private val log = KotlinLogging.logger {}

@Service
class EducationService(
  private val curiousApiClient: CuriousApiClient,
  private val prisonerSearchApiClient: PrisonerSearchApiClient,
  private val educationEnrolmentRepository: EducationEnrolmentRepository,
  private val needService: NeedService,
  private val reviewScheduleService: ReviewScheduleService,
  private val planCreationScheduleService: PlanCreationScheduleService,
  private val elspPlanRepository: ElspPlanRepository,
  private val alnAssessmentRepository: AlnAssessmentRepository,
  private val clock: Clock,
) {

  fun hasActiveEducationEnrollment(prisonNumber: String): Boolean = educationEnrolmentRepository.existsWithNoEndDate(prisonNumber)

  fun getNonPESEducationStartDate(prisonNumber: String): LocalDate? = educationEnrolmentRepository.findEarliestLearningStartDateWithNoEndDate(prisonNumber)

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
    log.info { "Education update - ${inboundEvent.description} for $prisonNumber" }

    val currentEstablishment = getCurrentLocation(prisonNumber)
    log.debug { "Current establishment as returned by prisoner-search-api: $currentEstablishment for $prisonNumber" }

    if (inboundEvent.description == "EDUCATION_STOPPED") {
      // Curious sends an EDUCATION_STOPPED message when they process a prisoner transfer domain event. IE. they are
      // processing the transfer event for their own records (not just education), and send a EDUCATION_STOPPED message
      // because by definition the prison must have stopped all education in the old prison.
      // We do not know whether _this_ EDUCATION_STOPPED message is as a result of a transfer or not, but it is a safe
      // assumption that we should end any eduction not in the current prison anyway.
      endAllEducationEnrolmentsNotInCurrentPrison(prisonNumber, currentEstablishment)
    }

    log.info {
      "Processing education status update event: {${inboundEvent.description}} for ${inboundEvent.prisonNumber()} \n " +
        "Detail URL: ${inboundEvent.detailUrl}" +
        ", reference: ${info.curiousExternalReference}"
    }

    log.debug { "Retrieving education info from Curious for $prisonNumber" }
    val educationDto = curiousApiClient.getEducation(prisonNumber = prisonNumber)
    log.debug { "Retrieved education info from Curious for $prisonNumber : $educationDto" }

    val coursesInCurrentPrison = educationDto.educationData.filter { it.establishmentId == currentEstablishment }
    log.debug { "Curious courses (including non-active and non-PES) for prisoner $prisonNumber for $currentEstablishment : $coursesInCurrentPrison" }

    // Record/update the EducationEnrolments if any have changed
    log.debug { "Calculating the diff between the EducationEnrolment records that SAN holds vs. the Curious courses" }
    val enrolmentDiff = updateSanEnrolments(
      coursesInCurrentPrison = coursesInCurrentPrison,
      prisonNumber = prisonNumber,
      curiousRef = info.curiousExternalReference,
    )
    log.debug { "EducationEnrolment diff for $prisonNumber: $enrolmentDiff" }

    if (enrolmentDiff.anyChanges) {
      log.debug { "There have been changes to the EducationEnrolments for $prisonNumber" }

      if (!enrolmentDiff.onAnyPesCourseInCurrentPrison) {
        log.info { "Prisoner $prisonNumber is no longer in PES education at $currentEstablishment. Exempt any Plan Creation and Review schedules" }
        // exempt any schedules
        // this will exempt schedules if they exist AND sent messages to MN.
        planCreationScheduleService.exemptSchedule(
          prisonNumber,
          PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
          prisonId = currentEstablishment,
        )
        reviewScheduleService.exemptSchedule(
          prisonNumber,
          ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
          prisonId = currentEstablishment,
        )
      }

      // Irrespective of whether the prisoner is on a PES course or not, if they have needs and have started/re-started ANY course, we need to check their Plan and Reviews, and potentially update/create them.
      val prisonerHasNeeds = needService.hasNeed(prisonNumber)
      val hasStartedOrRestartedAnyCourse = enrolmentDiff.createdCourseCount > 0 || enrolmentDiff.reopenedCourseCount > 0
      if (prisonerHasNeeds && hasStartedOrRestartedAnyCourse) {
        log.debug { "Prisoner $prisonNumber has a need and has started or re-started a course (PES or non-PES)" }

        // If the prisoner does not yet have a Plan we need to create a Plan Creation Schedule
        if (elspPlanRepository.findByPrisonNumber(prisonNumber) == null) {
          log.debug { "Prisoner $prisonNumber has started or restarted a course but does not yet have a Plan. Creating their Plan Creation Schedule" }
          // The details of how the Plan Creation Schedule is created is the responsibility of the PlanCreationScheduleService, but is based on whether
          // it is subject to the KPI rules (which is based on the prisoner's needs and the enrolment start date) and whether it is a PES course or not.
          val subjectToKPIRules = subjectToKPIRules(prisonNumber = prisonNumber, enrolmentDiff = enrolmentDiff)
          val startDate = enrolmentDiff.firstNewEnrolmentStart ?: LocalDate.now(clock)
          val isPesCourse = enrolmentDiff.hasCreatedOrRestartedPesCourse

          // create the plan creation schedule
          planCreationScheduleService.createOrUpdateDueToEducationUpdate(
            prisonNumber,
            startDate,
            isPesCourse,
            subjectToKPIRules,
            prisonId = currentEstablishment,
          )
        } else {
          // Prisoner has a Plan. If they have started or restarted a PES course we need to create or reschedule their Review Schedule
          if (enrolmentDiff.hasCreatedOrRestartedPesCourse) {
            log.debug { "Prisoner $prisonNumber has started or restarted a PES course. Creating or updating their Review Schedule" }
            val startDate = enrolmentDiff.firstNewEnrolmentStart ?: LocalDate.now(clock)
            reviewScheduleService.createOrUpdateDueToEducationUpdate(
              prisonNumber,
              startDate,
              prisonId = currentEstablishment,
            )
          } else {
            log.debug { "Prisoner $prisonNumber already has a Plan and has started or restarted a non-PES course. Review Schedule does not need to be rescheduled." }
          }
        }
      } else {
        log.debug {
          "No need to update prisoner $prisonNumber's Plan Schedule or Review Schedule.\n" +
            "Prisoner has needs: $prisonerHasNeeds \n" +
            "Prisoner has started or restarted any course: $hasStartedOrRestartedAnyCourse"
        }
      }
    }
  }

  @Transactional
  fun endAllEducationEnrolmentsNotInCurrentPrison(prisonNumber: String, currentEstablishment: String) {
    // End all education records for all establishments that are not the same as the current establishment
    log.info { "Ending current education enrolments for $prisonNumber that are not currently in establishment $currentEstablishment" }
    val educationEnrolments = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber)
    educationEnrolments.filter { it.establishmentId != currentEstablishment }.forEach { it.endDate = LocalDate.now(clock) }
    educationEnrolmentRepository.saveAll(educationEnrolments)
  }

  private fun getCurrentLocation(prisonNumber: String): String = prisonerSearchApiClient.getPrisoner(prisonNumber).prisonId ?: "N/A"

  private fun subjectToKPIRules(
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

  private fun updateSanEnrolments(
    coursesInCurrentPrison: List<Education>,
    prisonNumber: String,
    curiousRef: UUID,
  ): EnrolmentProcessingResults {
    // This method makes uses of a composite key EnrolmentKey to allow us to index into and cross reference the collections
    // of courses from Curious and EductionEnrolment records in the SAN database.
    // The Curious course data does not include any kind of reference, id, or unique key.
    // When we are processing the Curious courses, we need some way of identifying a corresponding EducationEnrolment record
    // if there is one. IE. are we processing a new Curious course where we need to create a new EducationEnrolment record,
    // or are we updating a course that we have previously recorded in the database.
    // The same course code can run across several prisons (eg: "GCSE_MATHS" can be run in BXI, WSI, LFI etc etc), and if
    // a prisoner were to transfer they could conceivably attend the "same" course in different prisons. Therefore we cannot
    // use just the course code (qualificationCode).
    // We could use the prisonId and course code as a composite key, eg: {BXI, GCSE_MATHS} , {WSI, GCSE_MATHS} etc
    // This is better, but it is possible for a prisoner to attend the same course in the same prison more than once, and each
    // course is treated as a separate piece of education.
    // The best option we have is to also include the course start date in the composite key. The composite key is made up of
    // prisonId, course code, and start date. eg {BXI, GCSE_MATHS, 2026/01/01},  {BXI, GCSE_MATHS, 2026/05/20} etc

    // create a key for all curious records
    val curiousCoursesInCurrentPrisonByKey: Map<EnrolmentKey, Education> =
      coursesInCurrentPrison.associateBy { it.key() }

    // create a set the keys of only the currently In Progress Curious courses
    val inProgressCuriousCourseKeys: Set<EnrolmentKey> =
      coursesInCurrentPrison
        .asSequence()
        .filter { it.isActive() }
        .map { it.key() }
        .toSet()

    // get all EducationEnrolment records from the SAN database and create a list of keys
    val allEducationEnrolmentRecordsInDb = educationEnrolmentRepository.findAllByPrisonNumber(prisonNumber) // ALL records that SAN holds for the prisoner, including completed, non-PES and other prisons
    val allEducationEnrolmentRecordsByKey = allEducationEnrolmentRecordsInDb.associateBy { EnrolmentKey(it.establishmentId, it.qualificationCode, it.learningStartDate) }

    // get the In-Progress EducationEnrolment records and create a list of keys
    val allInProgressEducationEnrolmentRecordsByKey = allEducationEnrolmentRecordsInDb
      .asSequence()
      .filter { it.isActive() }
      .associateBy { it.key() }
    val inProgressEducationEnrolmentRecordsKeys = allInProgressEducationEnrolmentRecordsByKey.keys

    val newOrReopenedCourseKeys = inProgressCuriousCourseKeys - inProgressEducationEnrolmentRecordsKeys // a list of keys that exist in the Curious data but not the SAN database records, hence they are new or re-opened
    val endedCourseKeys = inProgressEducationEnrolmentRecordsKeys - inProgressCuriousCourseKeys // a list of keys that exist in the SAN database records but not the Curious data, hence they are courses that have ended

    val createdOrUpdatedEntities = mutableListOf<EducationEnrolmentEntity>()
    var reopenedCourseCount = 0
    var createdCourseCount = 0
    var createdOrRestartedPesCourses = 0

    // We need to know that as a result of the diff, is the prisoner on any PES course in the current prison
    // Start by getting all the keys for current In-Progress EducationEnrolments that SAN already knows about
    val inProgressPesCourseKeys = allInProgressEducationEnrolmentRecordsByKey.keys
      .filter { key -> allInProgressEducationEnrolmentRecordsByKey.getValue(key).isPesCourse() }
      .toMutableSet()

    // Create new Education Enrolment records or update existing records if they have changed
    // Start by processing the new or re-opened courses. Re-opened courses will be processed as a database record update. New courses will be a new record in the database.
    newOrReopenedCourseKeys.forEach { key ->
      val curiousCourse = curiousCoursesInCurrentPrisonByKey.getValue(key)

      // Get the prisoner's existing EducationEnrolment record that matches the composite key from the Curious record (prisonId, course code, start date)
      // For restarted courses, this approach only works if the Curious record has not updated its start date (learningStartDate)
      // IE. original course has learningStartDate of 2026/01/01. Course is then stopped with a learningActualEndDate of 2026/02/01. When the course is restarted
      // in Curious on 2026/03/01 the learningStartDate should still be 2026/01/01. If the learningStartDate has been changed in Curious the matching based
      // on composite key will not find the corresponding EducationEnrolment record, and it will be treated as a new course.
      val correspondingEducationEnrolment = allEducationEnrolmentRecordsByKey[key]

      // If this is a PES course, add it to our set of In-Progress keys. If it's a new course it will add to the set. If it's an existing course it won't be added to the set.
      if (curiousCourse.isPesCourse()) {
        inProgressPesCourseKeys.add(key)
      }

      // If there is no EducationEnrolment record in the SAN database that corresponds to this Curious course, then this is a new course that needs to be recorded in the SAN database
      if (correspondingEducationEnrolment == null) {
        log.debug { "Curious course ${key.qualificationCode} has been started for $prisonNumber as a new Course. Creating the SAN EducationEnrolment" }
        val newEducationEnrolment = EducationEnrolmentEntity(
          prisonNumber = prisonNumber,
          establishmentId = key.establishmentId,
          qualificationCode = key.qualificationCode,
          learningStartDate = key.start,
          plannedEndDate = curiousCourse.learningPlannedEndDate,
          fundingType = curiousCourse.fundingType,
          completionStatus = curiousCourse.completionStatus,
          endDate = null,
          lastCuriousReference = curiousRef,
        )
        createdOrUpdatedEntities += educationEnrolmentRepository.save(newEducationEnrolment)
        createdCourseCount++
        if (newEducationEnrolment.isPesCourse()) {
          createdOrRestartedPesCourses++
        }
      } else if (correspondingEducationEnrolment.endDate != null) {
        // If the EducationEnrolment record in the SAN database that corresponds to this Curious course has an end date, then we need to clear that end data to make it In-Progress again
        log.debug { "Curious course ${key.qualificationCode} has been re-started for $prisonNumber. Updating the SAN EducationEnrolment" }
        correspondingEducationEnrolment.endDate = null
        correspondingEducationEnrolment.lastCuriousReference = curiousRef
        createdOrUpdatedEntities += educationEnrolmentRepository.save(correspondingEducationEnrolment)
        reopenedCourseCount++
        if (correspondingEducationEnrolment.isPesCourse()) {
          createdOrRestartedPesCourses++
        }
      } else {
        // The EducationEnrolment record in the SAN database that corresponds to this Curious course does not have an end date, which implies SAN already thinks it is In-Progress
        // This should rarely (if ever) happen
        log.warn { "Course ${key.qualificationCode} for prisoner $prisonNumber has been re-started in Curious, but the corresponding SAN record already has it as In-Progress" }
      }
    }

    // Now process the courses that have ended, processing each as a database record update.
    var endedCourseCount = 0
    endedCourseKeys.forEach { key ->
      val curiousCourse = curiousCoursesInCurrentPrisonByKey[key]
      val educationEnrolment = allInProgressEducationEnrolmentRecordsByKey.getValue(key)

      val endDate = curiousCourse?.learningActualEndDate ?: LocalDate.now(clock)
      if (educationEnrolment.endDate != endDate || educationEnrolment.completionStatus != curiousCourse?.completionStatus) {
        log.debug { "Curious course ${key.qualificationCode} has ended for $prisonNumber. Updating the SAN EducationEnrolment" }
        // remove the key from our set of In-Progress course keys
        inProgressPesCourseKeys.remove(key)

        educationEnrolment.endDate = endDate
        educationEnrolment.completionStatus = curiousCourse?.completionStatus ?: educationEnrolment.completionStatus
        educationEnrolment.lastCuriousReference = curiousRef
        educationEnrolmentRepository.save(educationEnrolment)
        endedCourseCount++
      }
    }

    val earliestStartDateOfAllCreatedOrUpdatedCourses = createdOrUpdatedEntities
      .minByOrNull { it.learningStartDate }
      ?.learningStartDate

    val result = EnrolmentProcessingResults(
      createdCourseCount = createdCourseCount,
      closedCourseCount = endedCourseCount,
      anyChanges = createdCourseCount > 0 || endedCourseCount > 0 || reopenedCourseCount > 0,
      onAnyPesCourseInCurrentPrison = inProgressPesCourseKeys.isNotEmpty(),
      hasCreatedOrRestartedPesCourse = createdOrRestartedPesCourses > 0,
      firstNewEnrolmentStart = earliestStartDateOfAllCreatedOrUpdatedCourses,
      reopenedCourseCount = reopenedCourseCount,
    )

    log.debug {
      "Enrolment updated for $prisonNumber - created=${result.createdCourseCount}, reopened=${result.reopenedCourseCount}, " +
        "closed=${result.closedCourseCount}"
    }
    return result
  }

  data class EnrolmentProcessingResults(
    val reopenedCourseCount: Int,
    val createdCourseCount: Int,
    val closedCourseCount: Int,
    val anyChanges: Boolean,
    val onAnyPesCourseInCurrentPrison: Boolean,
    val hasCreatedOrRestartedPesCourse: Boolean,
    val firstNewEnrolmentStart: LocalDate?,
  )

  fun Education.key(): EnrolmentKey = EnrolmentKey(establishmentId!!, qualificationCode, learningStartDate)
  fun EducationEnrolmentEntity.key(): EnrolmentKey = EnrolmentKey(establishmentId, qualificationCode, learningStartDate)

  data class EnrolmentKey(val establishmentId: String, val qualificationCode: String, val start: LocalDate)
}

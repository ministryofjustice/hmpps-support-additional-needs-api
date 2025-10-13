package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.DEFAULT_PRISON_ID
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerMergedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.ADMISSION
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.RETURN_FROM_COURT
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TEMPORARY_ABSENCE_RETURN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TRANSFERRED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReleasedAdditionalInformation
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class ScheduleService(
  val planCreationScheduleService: PlanCreationScheduleService,
  val reviewScheduleService: ReviewScheduleService,
  val educationService: EducationService,
  val elspPlanRepository: ElspPlanRepository,
  val prisonerSearchApiClient: PrisonerSearchApiClient,
) {

  @Transactional
  fun processReceived(info: PrisonerReceivedAdditionalInformation) {
    when (info.reason) {
      TRANSFERRED -> processTransfer(info)
      ADMISSION,
      RETURN_FROM_COURT,
      TEMPORARY_ABSENCE_RETURN,
      -> log.info { "Ignoring Processing Prisoner Received Into Prison Event with reason ${info.reason}" }
    }
    log.info("processed {${info.reason.name}} event for ${info.nomsNumber} movement code ${info.nomisMovementReasonCode}")
  }

  /**
   * Can be released or released with a movement code of DEC (deceased)
   * In both cases look up the active schedule and mark it as EXEMPT
   */
  @Transactional
  fun processReleased(info: PrisonerReleasedAdditionalInformation) {
    log.info("prisoner ${info.nomsNumber} processing release message with reason ${info.reason}")
    if (info.releaseTriggeredByPrisonerDeath) {
      planCreationScheduleService.exemptSchedule(info.nomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_DEATH, prisonId = info.prisonId)
      reviewScheduleService.exemptSchedule(info.nomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_DEATH, prisonId = info.prisonId)
    } else if (info.actualRelease) {
      planCreationScheduleService.exemptSchedule(info.nomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_RELEASE, prisonId = info.prisonId)
      reviewScheduleService.exemptSchedule(info.nomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_RELEASE, prisonId = info.prisonId)
    } else {
      log.info("prisoner ${info.nomsNumber} release message ignored due to reason ${info.reason}")
    }
  }

  /**
   * Merge is a strange case, basically one noms number in the message is the one that has
   * been removed and the other is a new nomis number - need to exempt the removed nomis number and
   * process the new one as if it was a new admission,
   */
  @Transactional
  fun processMerged(info: PrisonerMergedAdditionalInformation) {
    // exempt any schedules for the removed person:
    // merge events don't have a prison id so setting this  one to DEFAULT_PRISON_ID
    planCreationScheduleService.exemptSchedule(info.removedNomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_MERGE, prisonId = DEFAULT_PRISON_ID)
    reviewScheduleService.exemptSchedule(info.removedNomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_MERGE, prisonId = DEFAULT_PRISON_ID)
    log.info("processed {${info.reason.name}} event for ${info.nomsNumber}")
  }

  private fun processTransfer(info: PrisonerReceivedAdditionalInformation) {
    // Curious also process transfer messages and will be sending us a message to say the
    // person has been un-enrolled in education.
    // Decision was made to also exempt the schedules here, even though we will also be receiving a
    // message from Curious to say that the person is exempt due to not being in education.
    planCreationScheduleService.exemptSchedule(info.nomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_TRANSFER, prisonId = info.prisonId)
    reviewScheduleService.exemptSchedule(info.nomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_TRANSFER, prisonId = info.prisonId)

    // If the person is currently in education set them to not being in education any more
    // null curious reference since this wasn't from a curious message.
    val currentEstablishment = prisonerSearchApiClient.getPrisoner(info.nomsNumber).prisonId ?: "N/A"
    educationService.endNonCurrentEducationEnrollments(info.nomsNumber, currentEstablishment)

    val inEducation = educationService.hasActiveEducationEnrollment(info.nomsNumber)
    if (inEducation) {
      log.info("Setting ${info.nomsNumber} to no longer in education due to transfer message.")
      educationService.recordEducationRecord(
        prisonNumber = info.nomsNumber,
        inEducation = false,
        curiousReference = null,
      )
    }
    log.info("{${info.reason.name}} event for ${info.nomsNumber} received")
  }

  @Transactional
  fun processNeedChange(prisonNumber: String, hasNeed: Boolean, alnAssessmentDate: LocalDate? = null, prisonId: String) {
    log.info { "Processing needs change for $prisonNumber" }
    // If the person no longer has a need exempt any schedules
    if (!hasNeed) {
      log.debug { "Prisoner $prisonNumber has no need exempt schedules." }
      planCreationScheduleService.exemptSchedule(prisonNumber, PlanCreationScheduleStatus.EXEMPT_NO_NEED, prisonId = prisonId)
      reviewScheduleService.exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NO_NEED, prisonId = prisonId)
      return
    }

    // Has a need but not in education do nothing
    if (!educationService.hasActiveEducationEnrollment(prisonNumber)) {
      log.debug { "Prisoner $prisonNumber has a need but is not in education no action." }
      return
    } else {
      log.debug { "Prisoner $prisonNumber was in education." }
    }

    // Will always be in education at this point
    val educationStartDate = educationService.getNonPESEducationStartDate(prisonNumber)
    // if the educationStartDate is null at this point then it was a non PES course

    // if the doesn't have a plan or plan creation schedule:
    // does the person already have an ELSP?
    val plan = elspPlanRepository.findByPrisonNumber(prisonNumber)
    if (plan == null) {
      log.debug { "$prisonNumber doesn't have a plan - try to create a plan creation schedule." }
      planCreationScheduleService.createOrUpdateDueToNeedChange(prisonNumber = prisonNumber, educationStartDate = educationStartDate, alnAssessmentDate = alnAssessmentDate, prisonId = prisonId)
    } else {
      log.debug { "$prisonNumber did have a plan - try to create a review schedule." }
      reviewScheduleService.createOrUpdateDueToNeedChange(prisonNumber = prisonNumber, educationStartDate = educationStartDate, alnAssessmentDate = alnAssessmentDate, prisonId = prisonId)
    }
  }
}

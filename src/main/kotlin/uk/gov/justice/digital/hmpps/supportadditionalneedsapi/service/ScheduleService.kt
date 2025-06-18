package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerMergedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.ADMISSION
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.RETURN_FROM_COURT
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TEMPORARY_ABSENCE_RETURN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReceivedAdditionalInformation.Reason.TRANSFERRED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.PrisonerReleasedAdditionalInformation

private val log = KotlinLogging.logger {}

@Service
class ScheduleService(
  val planCreationScheduleService: PlanCreationScheduleService,
  val reviewScheduleService: ReviewScheduleService,
  val educationService: EducationService,
  val assessmentService: NeedService,
) {

  fun updateSchedules(additionalInformation: AdditionalInformation) {
    when (additionalInformation) {
      is PrisonerReceivedAdditionalInformation -> processReceived(additionalInformation)
      is PrisonerReleasedAdditionalInformation -> processReleased(additionalInformation)
      is PrisonerMergedAdditionalInformation -> processMerged(additionalInformation)
    }
  }

  private fun processReceived(info: PrisonerReceivedAdditionalInformation) {
    when (info.reason) {
      ADMISSION -> processNewAdmission(info.nomsNumber)
      TRANSFERRED -> processTransfer(info)
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
  private fun processReleased(info: PrisonerReleasedAdditionalInformation) {
    if (info.releaseTriggeredByPrisonerDeath) {
      planCreationScheduleService.exemptSchedule(info.nomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_DEATH)
      reviewScheduleService.exemptSchedule(info.nomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_DEATH)
    } else {
      planCreationScheduleService.exemptSchedule(info.nomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_RELEASE)
      reviewScheduleService.exemptSchedule(info.nomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_RELEASE)
    }
  }

  /**
   * Merge is a strange case, basically one noms number in the message is the one that has
   * been removed and the other is a new nomis number - need to exempt the removed nomis number and
   * process the new one as if it was a new admission,
   */
  private fun processMerged(info: PrisonerMergedAdditionalInformation) {
    // exempt any schedules for the removed person:
    planCreationScheduleService.exemptSchedule(info.removedNomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_MERGE)
    reviewScheduleService.exemptSchedule(info.removedNomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_MERGE)
    // process the new noms number as a new admission:
    processNewAdmission(info.nomsNumber)
    log.info("processed {${info.reason.name}} event for ${info.nomsNumber}")
  }

  private fun processNewAdmission(nomsNumber: String) {
    if (educationService.inEducation(nomsNumber) && assessmentService.hasNeed(nomsNumber)) {
      // create or update plan creation schedule or review schedule.
      // Check whether the person has a plan already, is in education and has a need.
      // depending on the outcome of those checks, create/update a planCreationSchedule
      // or reviewSchedule.
      TODO()
    }
  }

  private fun processTransfer(info: PrisonerReceivedAdditionalInformation) {
    // Curious also process transfer messages and will be sending us a message to say the
    // person has been un-enrolled in education. If this decision changes then we may want
    // to exempt the schedules here but for now only log that we received the message.
    log.info("{${info.reason.name}} event for ${info.nomsNumber} received")
  }
}

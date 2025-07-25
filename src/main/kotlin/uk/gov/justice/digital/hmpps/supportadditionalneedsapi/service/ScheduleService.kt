package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
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
  @Transactional
  fun processMerged(info: PrisonerMergedAdditionalInformation) {
    // exempt any schedules for the removed person:
    planCreationScheduleService.exemptSchedule(info.removedNomsNumber, PlanCreationScheduleStatus.EXEMPT_PRISONER_MERGE)
    reviewScheduleService.exemptSchedule(info.removedNomsNumber, ReviewScheduleStatus.EXEMPT_PRISONER_MERGE)
    log.info("processed {${info.reason.name}} event for ${info.nomsNumber}")
  }

  private fun processTransfer(info: PrisonerReceivedAdditionalInformation) {
    // Curious also process transfer messages and will be sending us a message to say the
    // person has been un-enrolled in education. If this decision changes then we may want
    // to exempt the schedules here but for now only log that we received the message.
    log.info("{${info.reason.name}} event for ${info.nomsNumber} received")
  }
}

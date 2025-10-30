package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.Hidden
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.DomainEventService
import java.time.Instant
import java.util.*
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation as EducationStatusUpdateAdditionalInformation1

private val log = KotlinLogging.logger {}

@RestController
@Hidden
@RequestMapping("/profile/{prisonNumber}")
class AdminToolsController(
  private val testDataService: DomainEventService,
) {

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/aln-trigger")
  @ResponseStatus(HttpStatus.CREATED)
  fun alnTriggerSimulation(
    @PathVariable prisonNumber: String,
  ) {
    log.info("Starting alnTriggerSimulation for $prisonNumber")
    val additionalInformation = EducationALNAssessmentUpdateAdditionalInformation(
      curiousExternalReference = UUID.randomUUID(),
    )
    val domainEvent = domainEventMessage(
      prisonNumber,
      EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
      additionalInformation = additionalInformation,
      description = "ASSESSMENT_COMPLETED",
    )
    testDataService.sendDomainEvent(domainEvent)
    log.info("Ended alnTriggerSimulation for $prisonNumber")
  }

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/education-trigger")
  @ResponseStatus(HttpStatus.CREATED)
  fun educationTriggerSimulation(
    @PathVariable prisonNumber: String,
    @RequestParam(name = "start", defaultValue = "true") start: Boolean = false,
  ) {
    log.info("Starting educationTriggerSimulation for $prisonNumber")
    val additionalInformation = EducationStatusUpdateAdditionalInformation1(
      curiousExternalReference = UUID.randomUUID(),
    )

    val domainEvent = domainEventMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = additionalInformation,
      description = if (start) "EDUCATION_STARTED" else "EDUCATION_STOPPED",
    )

    testDataService.sendDomainEvent(domainEvent)
    log.info("Ended educationTriggerSimulation for $prisonNumber")
  }

  fun domainEventMessage(
    prisonNumber: String,
    eventType: EventType = EventType.EDUCATION_STATUS_UPDATE,
    occurredAt: Instant = Instant.now().minusSeconds(10),
    publishedAt: Instant = Instant.now(),
    description: String = "EDUCATION_STOPPED",
    version: String = "1.0",
    additionalInformation: AdditionalInformation,
  ): SqsMessage = SqsMessage(
    Type = "Notification",
    Message = """
        {
          "eventType": "${eventType.eventType}",
          "personReference": { "identifiers": [ { "type": "NOMS", "value": "$prisonNumber" } ] },
          "occurredAt": "$occurredAt",
          "publishedAt": "$publishedAt",
          "description": "$description",
          "version": "$version",
          "additionalInformation": ${ObjectMapper().writeValueAsString(additionalInformation)}
        }        
    """.trimIndent(),
    MessageId = UUID.randomUUID(),
  )
}

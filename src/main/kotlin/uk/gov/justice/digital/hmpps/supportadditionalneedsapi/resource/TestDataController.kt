package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNStrength
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ScheduleService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.TestDataService
import java.time.Instant
import java.time.LocalDate
import java.util.*
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation as EducationStatusUpdateAdditionalInformation1
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource as IdentificationSourceModel

@RestController
@RequestMapping("/profile/{prisonNumber}")
@ConditionalOnProperty(name = ["ENABLE_TEST_ENDPOINTS"], havingValue = "true", matchIfMissing = false)
class TestDataController(
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val educationEnrolmentRepository: EducationEnrolmentRepository,
  private val educationService: EducationService,
  private val needService: NeedService,
  private val conditionService: ConditionService,
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val alnScreenerRepository: AlnScreenerRepository,
  private val scheduleService: ScheduleService,
  private val testDataService: TestDataService,
) {
  /**
   * Test only endpoint to set up a person with appropriate test data.
   *
   */
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/set-up-data")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPersonInEducationWithNeeds(
    @PathVariable prisonNumber: String,
    @RequestBody request: EducationNeedRequest,
  ): PlanCreationScheduleEntity? {
    val curiousRef = UUID.randomUUID()
    with(request) {
      educationService.recordEducationRecord(prisonNumber, inEducation, curiousRef)
      if (inEducation) {
        val educationEnrolmentEntity = EducationEnrolmentEntity(
          prisonNumber = prisonNumber,
          qualificationCode = "123",
          fundingType = "PES",
          endDate = null,
          learningStartDate = LocalDate.now(),
          establishmentId = "1234",
        )
        educationEnrolmentRepository.save(educationEnrolmentEntity)
      }
      if (alnNeed) {
        needService.recordAlnScreenerNeed(prisonNumber, true, curiousRef, LocalDate.now())
      }
      if (lddNeed) {
        needService.recordLddScreenerNeed(prisonNumber, true)
      }
      if (conditionSelfDeclared) {
        conditionService.createConditions(
          prisonNumber,
          CreateConditionsRequest(
            listOf(
              ConditionRequest(
                source = Source.SELF_DECLARED,
                prisonId = prisonId,
                conditionTypeCode = "ADHD",
                conditionDetails = "Added by test endpoint",
              ),
            ),
          ),
        )
      }
      if (conditionConfirmed) {
        conditionService.createConditions(
          prisonNumber,
          CreateConditionsRequest(
            listOf(
              ConditionRequest(
                source = Source.CONFIRMED_DIAGNOSIS,
                prisonId = prisonId,
                conditionTypeCode = "TOURETTES",
                conditionDetails = "Added by test endpoint",
              ),
            ),
          ),
        )
      }
      if (challengeNotALN) {
        challengeService.createChallenges(
          prisonNumber,
          CreateChallengesRequest(
            listOf(
              ChallengeRequest(
                prisonId = prisonId,
                challengeTypeCode = "FINISHING_TASKS",
                symptoms = "Challenge Symptoms by test endpoint",
                howIdentified = listOf(IdentificationSourceModel.WIDER_PRISON),
              ),
            ),
          ),
        )
      }
      if (strengthNotALN) {
        strengthService.createStrengths(
          prisonNumber,
          CreateStrengthsRequest(
            listOf(
              StrengthRequest(
                prisonId = prisonId,
                strengthTypeCode = "CALM",
                symptoms = "Calm Symptoms by test endpoint",
                howIdentified = listOf(IdentificationSourceModel.CONVERSATIONS),
              ),
            ),
          ),
        )
      }
      if (alnScreener) {
        // create an aln screener with a default challenge and strength
        val alnScreener = alnScreenerRepository.saveAndFlush(
          ALNScreenerEntity(
            prisonNumber = prisonNumber,
            createdAtPrison = prisonId,
            updatedAtPrison = prisonId,
            screeningDate = LocalDate.now(),
          ),
        )
        challengeService.createAlnChallenges(
          prisonNumber,
          listOf(ALNChallenge(challengeTypeCode = "WORD_BASED_PROBLEMS")),
          prisonId,
          alnScreener.id,
        )
        strengthService.createAlnStrengths(
          prisonNumber,
          listOf(ALNStrength(strengthTypeCode = "PEOPLE_PERSON")),
          prisonId,
          alnScreener.id,
        )
      }
    }

    scheduleService.processNeedChange(prisonNumber, needService.hasNeed(prisonNumber), prisonId = request.prisonId)

    return planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
  }

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/aln-trigger")
  @ResponseStatus(HttpStatus.CREATED)
  fun alnTriggerSimulation(
    @PathVariable prisonNumber: String,
  ) {
    val additionalInformation = EducationALNAssessmentUpdateAdditionalInformation(
      curiousExternalReference = UUID.randomUUID(),
    )
    val domainEvent = domainEventMessage(
      prisonNumber,
      EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
      additionalInformation = additionalInformation,
    )
    testDataService.sendDomainEvent(domainEvent)
  }

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/education-trigger")
  @ResponseStatus(HttpStatus.CREATED)
  fun educationTriggerSimulation(
    @PathVariable prisonNumber: String,
  ) {
    val additionalInformation = EducationStatusUpdateAdditionalInformation1(
      curiousExternalReference = UUID.randomUUID(),
    )
    val domainEvent = domainEventMessage(
      prisonNumber,
      EventType.EDUCATION_STATUS_UPDATE,
      additionalInformation = additionalInformation,
    )
    testDataService.sendDomainEvent(domainEvent)
  }
}

fun domainEventMessage(
  prisonNumber: String,
  eventType: EventType = EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
  occurredAt: Instant = Instant.now().minusSeconds(10),
  publishedAt: Instant = Instant.now(),
  description: String = "Test message from test end point",
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

data class EducationNeedRequest(
  val prisonId: String = "BXI",
  val alnNeed: Boolean = false,
  val lddNeed: Boolean = false,
  val conditionSelfDeclared: Boolean = false,
  val conditionConfirmed: Boolean = false,
  val challengeNotALN: Boolean = false,
  val strengthNotALN: Boolean = false,
  val alnScreener: Boolean = false,
  val inEducation: Boolean = false,
)

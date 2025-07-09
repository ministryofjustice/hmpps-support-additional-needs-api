package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.PlanCreationScheduleService
import java.time.LocalDate
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}")
@ConditionalOnProperty(name = ["ENABLE_TEST_ENDPOINTS"], havingValue = "true", matchIfMissing = false)
class TestDataController(
  private val planCreationScheduleRepository: PlanCreationScheduleRepository,
  private val educationService: EducationService,
  private val needService: NeedService,
  private val conditionService: ConditionService,
  private val challengeService: ChallengeService,
  private val planCreationScheduleService: PlanCreationScheduleService,
) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping("/plan-creation-schedule")
  @ResponseStatus(HttpStatus.CREATED)
  fun createPlanCreationSchedule(
    @PathVariable prisonNumber: String,
    @RequestBody request: Request,
  ): PlanCreationScheduleEntity = planCreationScheduleRepository.save(
    PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      deadlineDate = request.deadlineDate,
      status = PlanCreationScheduleStatus.SCHEDULED,
      createdAtPrison = request.prisonId,
      updatedAtPrison = request.prisonId,
    ),
  )

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
      if (alnNeed) {
        needService.recordAlnScreenerNeed(prisonNumber, true, curiousRef)
      }
      if (lddNeed) {
        needService.recordLddScreenerNeed(prisonNumber, true, curiousRef)
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
                detail = "Added by test endpoint",
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
                detail = "Added by test endpoint",
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
                symptoms = "Symptoms by test endpoint",
                howIdentified = "Identified by test endpoint",
              ),
            ),
          ),
        )
      }
    }

    planCreationScheduleService.attemptToCreate(prisonNumber, request.prisonId)
    planCreationScheduleService.attemptToUpdate(prisonNumber, request.prisonId)

    return planCreationScheduleRepository.findByPrisonNumber(prisonNumber)
  }
}

data class Request(val deadlineDate: LocalDate = LocalDate.now().plusDays(10), val prisonId: String = "BXI")
data class EducationNeedRequest(
  val prisonId: String = "BXI",
  val alnNeed: Boolean = false,
  val lddNeed: Boolean = false,
  val conditionSelfDeclared: Boolean = false,
  val conditionConfirmed: Boolean = false,
  val challengeNotALN: Boolean = false,
  val inEducation: Boolean = false,
)

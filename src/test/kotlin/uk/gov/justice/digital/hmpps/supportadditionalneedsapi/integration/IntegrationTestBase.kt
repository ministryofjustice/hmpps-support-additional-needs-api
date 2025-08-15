package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.awaitility.Awaitility
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import software.amazon.awssdk.services.sqs.model.PurgeQueueRequest
import software.amazon.awssdk.services.sqs.model.SendMessageRequest
import software.amazon.awssdk.services.sqs.model.SendMessageResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.LearnerNeurodivergenceDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.StrengthRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.SupportStrategyRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.TimelineRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.BankHolidaysApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.BankHolidaysApiExtension.Companion.bankHolidaysApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.CuriousApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.CuriousApiExtension.Companion.curiousApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsPrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsPrisonerSearchApiExtension.Companion.hmppsPrisonerSearchApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.LocalDate
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(HmppsAuthApiExtension::class, HmppsPrisonerSearchApiExtension::class, CuriousApiExtension::class, ManageUsersApiExtension::class, BankHolidaysApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT15M")
abstract class IntegrationTestBase {

  companion object {
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource.username", pgContainer::getUsername)
        registry.add("spring.datasource.password", pgContainer::getPassword)
      }

      System.setProperty("aws.region", "eu-west-2")

      localStackContainer?.also { setLocalStackProperties(it, registry) }
    }
  }

  init {
    // set awaitility defaults
    Awaitility.setDefaultPollInterval(500, MILLISECONDS)
    Awaitility.setDefaultTimeout(5, SECONDS)
  }

  @Autowired
  lateinit var educationRepository: EducationRepository

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var reviewScheduleRepository: ReviewScheduleRepository

  @Autowired
  protected lateinit var planCreationScheduleRepository: PlanCreationScheduleRepository

  @Autowired
  protected lateinit var reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository

  @Autowired
  protected lateinit var planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository

  @Autowired
  protected lateinit var referenceDataRepository: ReferenceDataRepository

  @Autowired
  protected lateinit var alnScreenerRepository: AlnScreenerRepository

  @Autowired
  protected lateinit var alnAssessmentRepository: AlnAssessmentRepository

  @Autowired
  protected lateinit var conditionRepository: ConditionRepository

  @Autowired
  protected lateinit var supportStrategyRepository: SupportStrategyRepository

  @Autowired
  protected lateinit var challengeRepository: ChallengeRepository

  @Autowired
  protected lateinit var strengthRepository: StrengthRepository

  @Autowired
  protected lateinit var elspPlanRepository: ElspPlanRepository

  @Autowired
  protected lateinit var timelineRepository: TimelineRepository

  @Autowired
  protected lateinit var educationEnrolmentRepository: EducationEnrolmentRepository

  @Autowired
  protected lateinit var elspPlanService: EducationSupportPlanService

  @Autowired
  protected lateinit var needService: NeedService

  @Autowired
  protected lateinit var workingDayService: WorkingDayService

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

  @Autowired
  protected lateinit var objectMapper: ObjectMapper

  @Autowired
  lateinit var hmppsQueueService: HmppsQueueService

  val domainEventQueue by lazy {
    hmppsQueueService.findByQueueId("supportadditionalneeds")
      ?: throw MissingQueueException("HmppsQueue supportadditionalneeds not found")
  }
  val domainEventQueueClient by lazy { domainEventQueue.sqsClient }
  val domainEventQueueDlqClient by lazy { domainEventQueue.sqsDlqClient }

  internal fun setAuthorisation(
    username: String? = "AUTH_ADM",
    roles: List<String> = listOf(),
    scopes: List<String> = listOf("read"),
  ): (HttpHeaders) -> Unit = jwtAuthHelper.setAuthorisationHeader(username = username, scope = scopes, roles = roles)

  protected fun stubGetTokenFromHmppsAuth() = hmppsAuth.stubGrantToken()

  protected fun stubPingWithResponse(status: Int) {
    hmppsAuth.stubHealthPing(status)
  }

  protected fun stubGetPrisonerFromPrisonerSearchApi(prisonNumber: String, response: Prisoner) = hmppsPrisonerSearchApi.stubGetPrisoner(prisonNumber, response)

  protected fun stubGetPrisonerNotFoundInPrisonerSearchApi(prisonNumber: String) = hmppsPrisonerSearchApi.stubGetPrisonerNotFound(prisonNumber)

  protected fun stubGetPrisonersInPrisonFromPrisonerSearchApi(prisonId: String, response: List<Prisoner>) = hmppsPrisonerSearchApi.stubPrisonersInAPrison(prisonId, response)

  protected fun stubGetPrisonersInPrisonFromPrisonerSearchApi(prisonId: String, response: String) = hmppsPrisonerSearchApi.stubPrisonersInAPrison(prisonId, response)

  protected fun stubGetCurious1PrisonerLddData(prisonId: String, response: LearnerNeurodivergenceDTO) = curiousApi.stubGetCurious1PrisonerLddData(prisonId, response)

  protected fun stubGetCurious1PrisonerLddDataNotFound(prisonId: String) = curiousApi.stubGetCurious1PrisonerLddDataNotFound(prisonId)

  protected fun stubGetDisplayName(username: String) = manageUsersApi.setUpGetUser(username)

  protected fun stubGetUserRepeatPass(username: String) = manageUsersApi.setUpManageUsersRepeatPass(username)

  protected fun stubGetUserRepeatFail(username: String) = manageUsersApi.setUpManageUsersRepeatFail(username)

  protected fun stubGetCurious2LearnerAssessments(prisonId: String, response: String) = curiousApi.stubGetCurious2LearnerAssessments(prisonId, response)

  protected fun stubGetCurious2InEducation(prisonId: String, response: String) = curiousApi.stubGetCurious2Education(prisonId, response)

  protected fun stubGetCurious2OutEducation(prisonId: String, response: String) = curiousApi.stubGetCurious2Education(prisonId, response)

  protected fun stubForBankHoliday() = bankHolidaysApi.stubBankHolidays()

  fun clearQueues() {
    // clear all the queues just in case there are any messages hanging around
    domainEventQueueClient.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.queueUrl).build()).get()
    domainEventQueueDlqClient!!.purgeQueue(PurgeQueueRequest.builder().queueUrl(domainEventQueue.dlqUrl).build())
      .get()
  }

  fun sendDomainEvent(
    message: SqsMessage,
    queueUrl: String = domainEventQueue.queueUrl,
  ): SendMessageResponse = domainEventQueueClient.sendMessage(
    SendMessageRequest.builder()
      .queueUrl(queueUrl)
      .messageBody(
        objectMapper.writeValueAsString(message),
      ).build(),
  ).get()

  fun aValidPlanCreationScheduleExists(
    prisonNumber: String,
    status: PlanCreationScheduleStatus = PlanCreationScheduleStatus.SCHEDULED,
    deadlineDate: LocalDate? = LocalDate.now().minusMonths(1),
  ) {
    val planCreationScheduleEntity =
      PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        deadlineDate = deadlineDate,
        status = status,
        exemptionReason = null,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        needSources = setOf(NeedSource.ALN_SCREENER, NeedSource.CONDITION_SELF_DECLARED),
        earliestStartDate = null,
      )
    planCreationScheduleRepository.saveAndFlush(planCreationScheduleEntity)
  }

  fun aValidReviewScheduleExists(
    prisonNumber: String,
    reference: UUID = UUID.randomUUID(),
    status: ReviewScheduleStatus = ReviewScheduleStatus.SCHEDULED,
    deadlineDate: LocalDate = LocalDate.now().minusMonths(1),
  ) {
    val reviewScheduleEntity =
      ReviewScheduleEntity(
        prisonNumber = prisonNumber,
        deadlineDate = deadlineDate,
        status = status,
        exemptionReason = null,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      )
    reviewScheduleRepository.saveAndFlush(reviewScheduleEntity)
  }

  fun prisonerHasNeed(prisonNumber: String) {
    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = ConditionEntity(
      prisonNumber = prisonNumber,
      source = Source.SELF_DECLARED,
      conditionType = adhd,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
    )

    conditionRepository.save(condition)
  }

  fun prisonerInEducation(prisonNumber: String) {
    val educationEntity = EducationEntity(prisonNumber = prisonNumber, inEducation = true)
    educationRepository.save(educationEntity)
  }
  fun anElSPExists(prisonNumber: String) {
    val elsp = ElspPlanEntity(prisonNumber = prisonNumber, individualSupport = "support", createdAtPrison = "BXI", updatedAtPrison = "BXI")

    elspPlanRepository.save(elsp)
  }

  fun aValidChallengeExists(prisonNumber: String) {
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    challengeRepository.saveAll(
      listOf(
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )
  }
}

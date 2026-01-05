package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration

import com.fasterxml.jackson.databind.ObjectMapper
import org.assertj.core.api.Assertions
import org.awaitility.Awaitility
import org.awaitility.kotlin.await
import org.awaitility.kotlin.matches
import org.awaitility.kotlin.untilCallTo
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.LegalStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidEducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.common.aValidHmppsDomainEventsSqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.ReviewConfig
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EhcpStatusEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EhcpStatusRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.LddAssessmentRepository
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.EhcpStatusMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspPlanMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.SqsMessage
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import uk.gov.justice.hmpps.sqs.HmppsQueueService
import uk.gov.justice.hmpps.sqs.MissingQueueException
import uk.gov.justice.hmpps.sqs.countMessagesOnQueue
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.*
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.SECONDS

@ExtendWith(
  HmppsAuthApiExtension::class,
  HmppsPrisonerSearchApiExtension::class,
  CuriousApiExtension::class,
  ManageUsersApiExtension::class,
  BankHolidaysApiExtension::class,
)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureWebTestClient(timeout = "PT15M")
abstract class IntegrationTestBase {

  companion object {
    val pesContractDate = LocalDate.of(2025, 10, 1)
    private val pgContainer = PostgresContainer.instance
    private val localStackContainer = LocalStackContainer.instance

    @JvmStatic
    @DynamicPropertySource
    fun properties(registry: DynamicPropertyRegistry) {
      pgContainer?.run {
        registry.add("spring.datasource-primary.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource-primary.username", pgContainer::getUsername)
        registry.add("spring.datasource-primary.password", pgContainer::getPassword)

        // Replica datasource uses the same database in tests for now (no actual replication)
        registry.add("spring.datasource-replica.url", pgContainer::getJdbcUrl)
        registry.add("spring.datasource-replica.username", pgContainer::getUsername)
        registry.add("spring.datasource-replica.password", pgContainer::getPassword)
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
  lateinit var ehcpStatusRepository: EhcpStatusRepository

  @Autowired
  private lateinit var ehcpStatusMapper: EhcpStatusMapper

  @Autowired
  private lateinit var elspPlanMapper: ElspPlanMapper

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
  protected lateinit var lddAssessmentRepository: LddAssessmentRepository

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
  protected lateinit var elspPlanHistoryRepository: ElspPlanHistoryRepository

  @Autowired
  protected lateinit var timelineRepository: TimelineRepository

  @Autowired
  protected lateinit var educationEnrolmentRepository: EducationEnrolmentRepository

  @Autowired
  protected lateinit var elspReviewRepository: ElspReviewRepository

  @Autowired
  protected lateinit var elspReviewHistoryRepository: ElspReviewHistoryRepository

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

  @Autowired
  lateinit var reviewConfig: ReviewConfig

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

  fun aPrisonerExists(prisonNumber: String, prisonId: String = "BXI") {
    stubGetPrisonerFromPrisonerSearchApi(
      prisonNumber,
      Prisoner(
        prisonerNumber = prisonNumber,
        legalStatus = LegalStatus.SENTENCED,
        releaseDate = LocalDate.now().plusYears(5),
        prisonId = prisonId,
        isIndeterminateSentence = false,
        isRecall = false,
        lastName = "smith",
        firstName = "bob",
        dateOfBirth = LocalDate.now().minusYears(22),
        cellLocation = "12b",
        releaseType = "",
      ),
    )
  }

  fun aValidPlanCreationScheduleExists(
    prisonNumber: String,
    status: PlanCreationScheduleStatus = PlanCreationScheduleStatus.SCHEDULED,
    deadlineDate: LocalDate = LocalDate.now().minusMonths(1),
  ) {
    val planCreationScheduleEntity =
      PlanCreationScheduleEntity(
        prisonNumber = prisonNumber,
        deadlineDate = deadlineDate,
        status = status,
        exemptionReason = null,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        needSources = sortedSetOf(NeedSource.ALN_SCREENER, NeedSource.CONDITION_SELF_DECLARED),
        earliestStartDate = null,
      )
    planCreationScheduleRepository.saveAndFlush(planCreationScheduleEntity)
  }

  fun aValidReviewScheduleExists(
    prisonNumber: String,
    reference: UUID = UUID.randomUUID(),
    status: ReviewScheduleStatus = ReviewScheduleStatus.SCHEDULED,
    deadlineDate: LocalDate = LocalDate.now().minusMonths(1),
  ): ReviewScheduleEntity {
    val reviewScheduleEntity =
      ReviewScheduleEntity(
        prisonNumber = prisonNumber,
        deadlineDate = deadlineDate,
        status = status,
        exemptionReason = null,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      )
    return reviewScheduleRepository.saveAndFlush(reviewScheduleEntity)
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

  fun prisonerInEducation(prisonNumber: String, learningStartDate: LocalDate = LocalDate.now(), establishmentId: String = "BXI") {
    val educationEntity = EducationEntity(prisonNumber = prisonNumber, inEducation = true)
    educationRepository.save(educationEntity)
    val educationEnrolmentEntity = EducationEnrolmentEntity(
      prisonNumber = prisonNumber,
      qualificationCode = "123",
      fundingType = "PES",
      endDate = null,
      learningStartDate = learningStartDate,
      establishmentId = establishmentId,
    )
    educationEnrolmentRepository.save(educationEnrolmentEntity)
  }

  fun anElSPExists(prisonNumber: String) {
    val elsp = elspPlanMapper.toEntity(
      prisonNumber,
      educationSupportPlanRequest = CreateEducationSupportPlanRequest(
        prisonId = "BXI",
        hasCurrentEhcp = true,
        reviewDate = LocalDate.now(),
        individualSupport = "support",
        planCreatedBy = PlanContributor("Tom Brown", jobRole = "Education coordinator"),
        otherContributors = listOf(PlanContributor(name = "Bob Smith", jobRole = "Teacher")),
        teachingAdjustments = "teachingAdjustments",
        specificTeachingSkills = "specificTeachingSkills",
        examAccessArrangements = "examAccessArrangements",
        lnspSupport = "lnspSupport",
        lnspSupportHours = 2,
        detail = "detail",
      ),
    )

    val ehcp = ehcpStatusMapper.toEntity(
      prisonNumber,
      educationSupportPlanRequest = CreateEducationSupportPlanRequest(
        prisonId = "BXI",
        hasCurrentEhcp = true,
        reviewDate = LocalDate.now(),
        individualSupport = "support",
        planCreatedBy = PlanContributor("Tom Brown", jobRole = "Education coordinator"),
        otherContributors = listOf(PlanContributor(name = "Bob Smith", jobRole = "Teacher")),
        teachingAdjustments = "teachingAdjustments",
        specificTeachingSkills = "specificTeachingSkills",
        examAccessArrangements = "examAccessArrangements",
        lnspSupport = "lnspSupport",
        lnspSupportHours = 2,
        detail = "detail",
      ),
    )

    elspPlanRepository.save(elsp)
    ehcpStatusRepository.save(ehcp)
  }

  fun anOldElSPExists(prisonNumber: String): ElspPlanEntity {
    val elsp = ElspPlanEntity(
      prisonNumber = prisonNumber,
      updatedAtPrison = "BXI",
      individualSupport = "support",
      planCreatedByJobRole = "Education coordinator",
      planCreatedByName = "Bob Smith",
      teachingAdjustments = "teachingAdjustments",
      specificTeachingSkills = "specificTeachingSkills",
      examAccessArrangements = "examAccessArrangements",
      lnspSupport = "lnspSupport",
      lnspSupportHours = 2,
      detail = "detail",
      createdAtPrison = "BXI",
      createdAt = Instant.now().minus(90, ChronoUnit.DAYS),
    )
    val ehcp = EhcpStatusEntity(prisonNumber = prisonNumber, hasCurrentEhcp = true, createdAtPrison = "BXI", updatedAtPrison = "BXI")
    ehcpStatusRepository.save(ehcp)
    return elspPlanRepository.saveAndFlush(elsp)
  }

  fun anElSPReviewExists(prisonNumber: String, reviewScheduleReference: UUID): ElspReviewEntity {
    val review = ElspReviewEntity(
      prisonNumber = prisonNumber,
      prisonerDeclinedFeedback = false,
      prisonerFeedback = "prisonerFeedback",
      reviewerFeedback = "reviewerFeedback",
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      reviewScheduleReference = reviewScheduleReference,
    )
    return elspReviewRepository.save(review)
  }

  fun aValidAlnScreenerExists(prisonNumber: String): ALNScreenerEntity = alnScreenerRepository.saveAndFlush(
    ALNScreenerEntity(
      prisonNumber,
      screeningDate = LocalDate.now(),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      hasChallenges = true,
      hasStrengths = true,
    ),
  )

  fun aValidChallengeExists(prisonNumber: String, screenerId: UUID? = null) {
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    challengeRepository.saveAll(
      listOf(
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          symptoms = "symptoms",
          howIdentified = sortedSetOf(IdentificationSource.COLLEAGUE_INFO, IdentificationSource.OTHER_SCREENING_TOOL),
          alnScreenerId = screenerId,
        ),
      ),
    )
  }

  fun aValidReviewExists(prisonNumber: String, reviewScheduleReference: UUID) {
    elspReviewRepository.saveAll(
      listOf(
        ElspReviewEntity(
          prisonNumber = prisonNumber,
          reviewCreatedByName = "Bob Smith",
          reviewCreatedByJobRole = "Role",
          prisonerDeclinedFeedback = false,
          prisonerFeedback = "prisoner feedback",
          reviewerFeedback = "reviewer feedback",
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          otherContributors = mutableListOf(),
          reviewScheduleReference = reviewScheduleReference,

        ),
      ),
    )
  }

  fun aValidConditionExists(prisonNumber: String) {
    val conditionType = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")
    conditionRepository.saveAll(
      listOf(
        ConditionEntity(
          prisonNumber = prisonNumber,
          conditionType = conditionType,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          source = Source.SELF_DECLARED,
        ),
      ),
    )
  }

  fun aValidStrengthExists(prisonNumber: String, screenerId: UUID? = null) {
    val strengthType = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    strengthRepository.saveAll(
      listOf(
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = strengthType,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          symptoms = "StrengthSymptoms",
          howIdentified = sortedSetOf(IdentificationSource.WIDER_PRISON, IdentificationSource.CONVERSATIONS),
          alnScreenerId = screenerId,
        ),
      ),
    )
  }

  fun conditionsExist(prisonNumber: String) {
    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")
    val dyslexia = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "DYSLEXIA"))
      ?: throw IllegalStateException("Reference data not found")
    val mentalHealth = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "MENTAL_HEALTH"))
      ?: throw IllegalStateException("Reference data not found")

    conditionRepository.saveAll(
      listOf(
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = adhd,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = dyslexia,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.CONFIRMED_DIAGNOSIS,
          conditionType = mentalHealth,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )
  }

  fun nonAlnChallengesExist(prisonNumber: String) {
    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
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
        ChallengeEntity(
          prisonNumber = prisonNumber,
          challengeType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )
  }

  fun nonAlnStrengthsExist(prisonNumber: String) {
    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    strengthRepository.saveAll(
      listOf(
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )
  }

  fun supportStrategiesExist(prisonNumber: String) {
    val processingSpeed = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "PROCESSING_SPEED"))
      ?: throw IllegalStateException("Reference data not found for PROCESSING_SPEED")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "SENSORY"))
      ?: throw IllegalStateException("Reference data not found for SENSORY")
    val general = referenceDataRepository.findByKey(ReferenceDataKey(Domain.SUPPORT_STRATEGY, "GENERAL"))
      ?: throw IllegalStateException("Reference data not found for GENERAL")

    supportStrategyRepository.saveAll(
      listOf(
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = processingSpeed,
          detail = "Needs quiet space to focus",
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
        SupportStrategyEntity(
          prisonNumber = prisonNumber,
          supportStrategyType = general,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          active = true,
        ),
      ),
    )
  }

  fun createALNAssessmentMessage(
    prisonNumber: String,
    curiousReference: UUID = UUID.randomUUID(),
    hasNeed: Boolean = true,
    assessmentDate: LocalDate = LocalDate.now(),
  ) {
    stubGetCurious2LearnerAssessments(
      prisonNumber,
      createTestALNAssessment(prisonNumber, hasNeed = hasNeed, assessmentDate = assessmentDate),
    )
    val sqsMessage = aValidHmppsDomainEventsSqsMessage(
      prisonNumber = prisonNumber,
      eventType = EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
      additionalInformation = aValidEducationALNAssessmentUpdateAdditionalInformation(curiousReference),
      description = "ASSESSMENT_COMPLETED",
    )
    sendCuriousALNMessage(sqsMessage)

    // Then
    // wait until the queue is drained / message is processed
    await untilCallTo {
      domainEventQueueClient.countMessagesOnQueue(domainEventQueue.queueUrl).get()
    } matches { it == 0 }
    await untilCallTo {
      val alnAssessment = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      if (hasNeed) {
        Assertions.assertThat(alnAssessment!!.hasNeed).isTrue()
      } else {
        Assertions.assertThat(alnAssessment!!.hasNeed).isFalse()
      }
      Assertions.assertThat(alnAssessment.curiousReference).isEqualTo(curiousReference)
      Assertions.assertThat(alnAssessment.screeningDate).isEqualTo(assessmentDate)
    } matches { it != null }
  }

  fun createTestALNAssessment(
    prisonNumber: String,
    hasNeed: Boolean = true,
    assessmentDate: LocalDate = LocalDate.now(),
  ): String = """{
  "v2": {
    "assessments": {
      "aln": [
        {
          "assessmentDate": "$assessmentDate",
          "assessmentOutcome": "${if (hasNeed) "Yes" else "No"}",
          "establishmentId": "KMI",
          "establishmentName": "WTI",
          "hasPrisonerConsent": "Yes",
          "stakeholderReferral": "yes"
        }
      ]
    },
    "prn": "$prisonNumber"
  }
}"""

  private fun sendCuriousALNMessage(sqsMessage: SqsMessage) {
    sendDomainEvent(sqsMessage)
  }

  fun aSmallPause(duration: Long = 100L) {
    Thread.sleep(duration)
  }
}

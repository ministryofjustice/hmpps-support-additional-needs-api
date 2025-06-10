package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration

import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT
import org.springframework.http.HttpHeaders
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.LearnerNeurodivergenceDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.LocalStackContainer
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.LocalStackContainer.setLocalStackProperties
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.container.PostgresContainer
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.CuriousApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.CuriousApiExtension.Companion.curiousApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsAuthApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsAuthApiExtension.Companion.hmppsAuth
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsPrisonerSearchApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsPrisonerSearchApiExtension.Companion.hmppsPrisonerSearchApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.ManageUsersApiExtension
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.hmpps.test.kotlin.auth.JwtAuthorisationHelper

@ExtendWith(HmppsAuthApiExtension::class, HmppsPrisonerSearchApiExtension::class, CuriousApiExtension::class, ManageUsersApiExtension::class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ActiveProfiles("test")
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

  @Autowired
  protected lateinit var webTestClient: WebTestClient

  @Autowired
  protected lateinit var reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository

  @Autowired
  protected lateinit var planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository

  @Autowired
  protected lateinit var referenceDataRepository: ReferenceDataRepository

  @Autowired
  protected lateinit var conditionRepository: ConditionRepository

  @Autowired
  protected lateinit var challengeRepository: ChallengeRepository

  @Autowired
  protected lateinit var elspPlanRepository: ElspPlanRepository

  @Autowired
  protected lateinit var elspPlanService: EducationSupportPlanService

  @Autowired
  protected lateinit var jwtAuthHelper: JwtAuthorisationHelper

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
}

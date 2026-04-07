package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.persistence.EntityManager
import org.junit.jupiter.api.BeforeEach
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.reactive.server.WebTestClient
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarApiDataTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarIntegrationTestHelper
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarJpaEntitiesTest
import uk.gov.justice.digital.hmpps.subjectaccessrequest.SarReportTest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import java.time.LocalDate
import javax.sql.DataSource

class SubjectAccessRequestTemplateTest :
  IntegrationTestBase(),
  SarApiDataTest,
  SarReportTest,
  SarJpaEntitiesTest {

  @Autowired
  lateinit var dataSource: DataSource

  @Autowired
  lateinit var entityManager: EntityManager

  override fun getSarHelper(): SarIntegrationTestHelper = sarIntegrationTestHelper

  override fun getWebTestClientInstance(): WebTestClient = webTestClient

  override fun getPrn(): String = "Z9999ZZ"

  override fun getEntityManagerInstance(): EntityManager = entityManager

  @BeforeEach
  fun cleanUpTestData() {
    val prisonNumber = getPrn()
    elspReviewRepository.deleteAll(elspReviewRepository.findAllByPrisonNumber(prisonNumber))
    elspPlanRepository.findByPrisonNumber(prisonNumber)?.let { elspPlanRepository.delete(it) }
    challengeRepository.deleteAll(challengeRepository.findAllByPrisonNumber(prisonNumber))
    strengthRepository.deleteAll(strengthRepository.findAllByPrisonNumber(prisonNumber))
    conditionRepository.deleteAll(conditionRepository.findAllByPrisonNumber(prisonNumber))
    planCreationScheduleRepository.findByPrisonNumber(prisonNumber)?.let { planCreationScheduleRepository.delete(it) }
    reviewScheduleRepository.deleteAll(reviewScheduleRepository.findAllByPrisonNumber(prisonNumber))
    alnScreenerRepository.deleteAll(alnScreenerRepository.findAllByPrisonNumber(prisonNumber))
    timelineRepository.deleteAll(timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber))
  }

  override fun setupTestData() {
    val prisonNumber = getPrn()
    stubGetDisplayName("testuser")
    anElSPExists(prisonNumber)
    aValidChallengeExists(prisonNumber)
    aValidStrengthExists(prisonNumber)
    aValidConditionExists(prisonNumber)
    aValidPlanCreationScheduleExists(prisonNumber, status = PlanCreationScheduleStatus.COMPLETED)
    val reviewScheduleEntity = aValidReviewScheduleExists(prisonNumber, deadlineDate = LocalDate.now().plusMonths(1))
    aValidReviewExists(prisonNumber, reviewScheduleEntity.reference)
    val screener = aValidAlnScreenerExists(prisonNumber)
    aValidChallengeExists(prisonNumber, screenerId = screener.id)
    aValidStrengthExists(prisonNumber, screenerId = screener.id)
  }
}

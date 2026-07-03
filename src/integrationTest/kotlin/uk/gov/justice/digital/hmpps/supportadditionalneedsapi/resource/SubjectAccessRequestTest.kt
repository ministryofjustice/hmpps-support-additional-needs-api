package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import com.fasterxml.jackson.module.kotlin.convertValue
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.IdentificationSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate
import java.util.UUID

class SubjectAccessRequestTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/subject-access-request"
  }

  @Test
  fun `should return unauthorized given no bearer token`() {
    webTestClient.get()
      .uri(URI_TEMPLATE, randomValidPrisonNumber())
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return 204 if no content found`() {
    stubGetTokenFromHmppsAuth()
    webTestClient.get()
      .uri { it.path(URI_TEMPLATE).queryParam("prn", randomValidPrisonNumber()).build() }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isNoContent
      .expectBody().isEmpty
  }

  @Test
  fun `should return 209 error if called with crn param`() {
    stubGetTokenFromHmppsAuth()
    webTestClient.get()
      .uri { it.path(URI_TEMPLATE).queryParam("crn", "A123456").build() }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isEqualTo(209)
      .expectBody().isEmpty
  }

  @Test
  fun `should return 400 error if called without prn or crn param`() {
    // Given
    stubGetTokenFromHmppsAuth()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .returnResult<ErrorResponse>()

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(400)
      .hasUserMessage("One of prn or crn must be supplied.")
      .hasDeveloperMessage("One of prn or crn must be supplied.")
  }

  @Test
  fun `should get subject-access-request data for specific prisoner without date filtering`() {
    // Given
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)
    aValidSupportStrategyExists(prisonNumber)
    aValidStrengthExists(prisonNumber)
    aValidChallengeExists(prisonNumber)
    aValidAlnScreenerExists(prisonNumber, screeningDate = LocalDate.parse("2026-02-15"))
      .also { aValidStrengthExists(prisonNumber, it.id) }
      .also { aValidChallengeExists(prisonNumber, it.id) }
    aValidAlnScreenerExists(prisonNumber, screeningDate = LocalDate.parse("2026-01-10"))
      .also { aValidStrengthExists(prisonNumber, it.id) }
      .also { aValidChallengeExists(prisonNumber, it.id) }
    aValidPlanCreationScheduleExists(prisonNumber, deadlineDate = LocalDate.parse("2026-03-15"))
    aValidReviewScheduleExists(prisonNumber, deadlineDate = LocalDate.parse("2026-04-15"))
    aValidConditionExists(prisonNumber)
    aValidAlnAssessmentExists(
      prisonNumber,
      screeningDate = LocalDate.parse("2026-01-05"),
      hasNeed = false,
      curiousReference = UUID.fromString("afcd7235-c13b-48c4-8424-61718b2255ba"),
    )
    aValidAlnAssessmentExists(
      prisonNumber,
      screeningDate = LocalDate.parse("2026-02-05"),
      hasNeed = true,
      curiousReference = UUID.fromString("46259b4d-7a9b-4fce-90c4-7c8c659b79b5"),
    )
    // Update the EHCP answer, creating a second EHCP version
    anEhcpStatusUpdateExists(prisonNumber, hasCurrentEhcp = false)

    // When
    val response = webTestClient.get()
      .uri { it.path(URI_TEMPLATE).queryParam("prn", prisonNumber).build() }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult<HmppsSubjectAccessRequestContent>()

    // Then
    val actual = objectMapper.convertValue<SubjectAccessRequestContent>(response.responseBody.blockFirst()!!.content)
    assertThat(actual)
      .originalEducationSupportPlan {
        it.hasIndividualSupport("support")
          .hasTeachingAdjustments("teachingAdjustments")
          .hasSpecificTeachingSkills("specificTeachingSkills")
          .hasExamAccessArrangements("examAccessArrangements")
          .hasLearningNeedsSupportPractitionerSupport("lnspSupport")
          .hasLearningNeedsSupportPractitionerHours(2)
          .hasOtherDetails("detail")
          .planWasCreatedByPlanContributor {
            it.hasName("Tom Brown")
              .hasJobRole("Education coordinator")
          }
          .planWasCreatedWithNumberOfOtherContributors(1)
          .planCreationContributor(1) {
            it.hasName("Bob Smith")
              .hasJobRole("Teacher")
          }
      }
      .hasNumberOfEhcpStatuses(2)
      .ehcpStatus(1) { it.hasCurrentEhcp() }
      .ehcpStatus(2) { it.doesNotHaveCurrentEhcp() }
      .hasNumberOfSupportStrategies(1)
      .supportStrategy(1) {
        it.isActive()
          .hasCode("PROCESSING_SPEED")
          .hasNoDetail()
      }
      .hasNumberOfNonAlnStrengths(1)
      .nonAlnStrength(1) {
        it.isActive()
          .hasCode("MEMORY")
          .hasSymptoms("StrengthSymptoms")
          .wasIdentifiedBy(setOf(IdentificationSource.WIDER_PRISON, IdentificationSource.CONVERSATIONS))
      }
      .hasNumberOfNonAlnChallenges(1)
      .nonAlnChallenge(1) {
        it.isActive()
          .hasCode("SENSORY_PROCESSING")
          .hasSymptoms("symptoms")
          .wasIdentifiedBy(setOf(IdentificationSource.COLLEAGUE_INFO, IdentificationSource.OTHER_SCREENING_TOOL))
      }
      .hasNumberOfAlnScreeners(2)
      .alnScreener(1) {
        it.hasScreenerDate(LocalDate.parse("2026-02-15"))
          .hasNumberOfStrengths(1)
          .strength(1) {
            it.isActive()
              .hasCode("MEMORY")
              .hasAlnScreenerDate(LocalDate.parse("2026-02-15"))
          }
          .hasNumberOfChallenges(1)
          .challenge(1) {
            it.isActive()
              .hasCode("SENSORY_PROCESSING")
              .hasAlnScreenerDate(LocalDate.parse("2026-02-15"))
          }
      }
      .alnScreener(2) {
        it.hasScreenerDate(LocalDate.parse("2026-01-10"))
          .hasNumberOfStrengths(1)
          .strength(1) {
            it.isActive()
              .hasCode("MEMORY")
              .hasAlnScreenerDate(LocalDate.parse("2026-01-10"))
          }
          .hasNumberOfChallenges(1)
          .challenge(1) {
            it.isActive()
              .hasCode("SENSORY_PROCESSING")
              .hasAlnScreenerDate(LocalDate.parse("2026-01-10"))
          }
      }
      .hasNumberOfPlanCreationSchedules(1)
      .planCreationSchedule(1) {
        it.hasDeadlineDate(LocalDate.parse("2026-03-15"))
          .hasStatus(PlanCreationStatus.SCHEDULED)
          .hasNoExemptionReason()
          .hasNoExemptionDetail()
          .hasVersion(1)
      }
      .hasNumberOfReviewSchedules(1)
      .reviewSchedule(1) {
        it.hasDeadlineDate(LocalDate.parse("2026-04-15"))
          .hasStatus(ReviewScheduleStatus.SCHEDULED)
          .hasNoExemptionReason()
          .hasVersion(1)
      }
      .alnScreener(1) { it.hasScreenerDate(LocalDate.parse("2026-02-15")) }
      .alnScreener(2) { it.hasScreenerDate(LocalDate.parse("2026-01-10")) }
      .hasNumberOfConditions(1)
      .condition(1) {
        it.isActive()
          .hasNoArchivedReason()
          .hasCode("ADHD")
          .hasNoConditionName()
          .hasSource(Source.SELF_DECLARED)
          .hasNoConditionDetails()
          .wasCreatedAtPrison("BXI")
          .wasUpdatedAtPrison("BXI")
        // TODO - add test to verify remaining fields:
        // createdAt/createdBy/updatedAt/updatedBy
      }
      .hasNumberOfAlnAssessments(2)
      .alnAssessment(1) {
        it.hasScreeningDate(LocalDate.parse("2026-02-05"))
          .hasHasNeed(true)
          .hasCuriousReference(UUID.fromString("46259b4d-7a9b-4fce-90c4-7c8c659b79b5"))
      }
      .alnAssessment(2) {
        it.hasScreeningDate(LocalDate.parse("2026-01-05"))
          .hasHasNeed(false)
          .hasCuriousReference(UUID.fromString("afcd7235-c13b-48c4-8424-61718b2255ba"))
      }
  }
}

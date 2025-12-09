package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar.SupportAdditionalNeedsContent
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent
import java.time.LocalDate

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
    stubGetTokenFromHmppsAuth()
    val response = webTestClient.get()
      .uri(URI_TEMPLATE)
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual!!.status).isEqualTo(400)
    assertThat(actual.userMessage).isEqualTo("One of prn or crn must be supplied.")
    assertThat(actual.developerMessage).isEqualTo("One of prn or crn must be supplied.")
  }

  @Test
  fun `should get induction and goals for specific prisoner without date filtering`() {
    // Given
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
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

    // Then
    val response = webTestClient.get()
      .uri { it.path(URI_TEMPLATE).queryParam("prn", prisonNumber).build() }
      .headers(setAuthorisation(roles = listOf("ROLE_SAR_DATA_ACCESS")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HmppsSubjectAccessRequestContent::class.java)

    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    val content = objectMapper.convertValue(actual!!.content, SupportAdditionalNeedsContent::class.java)

    assertThat(content.educationSupportPlans.size).isEqualTo(1)
    content.educationSupportPlans.first().let { p ->
      assertThat(p.individualSupport).isEqualTo("support")
      assertThat(p.teachingAdjustments).isEqualTo("teachingAdjustments")
      assertThat(p.specificTeachingSkills).isEqualTo("specificTeachingSkills")
      assertThat(p.examAccessArrangements).isEqualTo("examAccessArrangements")
      assertThat(p.lnspSupport).isEqualTo("lnspSupport")
      assertThat(p.lnspSupportHours).isEqualTo(2)
      assertThat(p.detail).isEqualTo("detail")
    }

    assertThat(content.challenges.size).isEqualTo(1)

    content.challenges.first().let { c ->
      assertThat(c.fromALNScreener).isEqualTo("No")
      assertThat(c.challengeType).isEqualTo("Sensory processing")
      assertThat(c.active).isEqualTo("Yes")
      assertThat(c.symptoms).isEqualTo("symptoms")
      assertThat(c.howIdentified).isEqualTo("colleague info, other screening tool")
    }

    assertThat(content.strengths.size).isEqualTo(1)

    content.strengths.first().let { c ->
      assertThat(c.fromALNScreener).isEqualTo("No")
      assertThat(c.strengthType).isEqualTo("Memory")
      assertThat(c.active).isEqualTo("Yes")
      assertThat(c.symptoms).isEqualTo("StrengthSymptoms")
      assertThat(c.howIdentified).isEqualTo("wider prison, conversations")
    }

    assertThat(content.conditions.size).isEqualTo(1)

    content.conditions.first().let { c ->
      assertThat(c.conditionType).isEqualTo("Attention Deficit Hyperactivity Disorder (ADHD / ADD)")
      assertThat(c.active).isEqualTo("Yes")
      assertThat(c.source).isEqualTo("self declared")
    }

    assertThat(content.planCreationSchedules.size).isEqualTo(1)

    content.planCreationSchedules.first().let { c ->
      assertThat(c.status).isEqualTo("completed")
      assertThat(c.deadlineDate).isEqualTo(LocalDate.now().minusMonths(1))
      assertThat(c.version).isEqualTo(0)
    }

    assertThat(content.reviewSchedules.size).isEqualTo(1)

    content.reviewSchedules.first().let { c ->
      assertThat(c.status).isEqualTo("scheduled")
      assertThat(c.deadlineDate).isEqualTo(LocalDate.now().plusMonths(1))
      assertThat(c.version).isEqualTo(0)
    }

    assertThat(content.reviews.size).isEqualTo(1)

    content.reviews.first().let { c ->
      assertThat(c.prisonerFeedback).isEqualTo("prisoner feedback")
      assertThat(c.reviewerFeedback).isEqualTo("reviewer feedback")
      assertThat(c.prisonerDeclinedFeedback).isEqualTo("No")
      assertThat(c.reviewCreatedByJobRole).isEqualTo("Role")
    }

    assertThat(content.alnScreeners.size).isEqualTo(1)
    content.alnScreeners.first().let { c ->
      assertThat(c.screeningDate).isEqualTo(LocalDate.now())
      assertThat(c.alnStrengths.size).isEqualTo(1)
      assertThat(c.alnChallenges.size).isEqualTo(1)

      c.alnChallenges.first().let { alnChallenge ->
        assertThat(alnChallenge.fromALNScreener).isEqualTo("Yes")
        assertThat(alnChallenge.challengeType).isEqualTo("Sensory processing")
        assertThat(alnChallenge.active).isEqualTo("Yes")
        assertThat(alnChallenge.symptoms).isEqualTo("symptoms")
        assertThat(alnChallenge.howIdentified).isEqualTo("colleague info, other screening tool")
      }

      c.alnStrengths.first().let { alnStrength ->
        assertThat(alnStrength.fromALNScreener).isEqualTo("Yes")
        assertThat(alnStrength.strengthType).isEqualTo("Memory")
        assertThat(alnStrength.active).isEqualTo("Yes")
        assertThat(alnStrength.symptoms).isEqualTo("StrengthSymptoms")
        assertThat(alnStrength.howIdentified).isEqualTo("wider prison, conversations")
      }
    }
  }
}

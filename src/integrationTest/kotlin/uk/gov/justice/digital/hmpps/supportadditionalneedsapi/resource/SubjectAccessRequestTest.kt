package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SubjectAccessRequestContent
import uk.gov.justice.hmpps.kotlin.sar.HmppsSubjectAccessRequestContent

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
  fun `should get original education support plan for specific prisoner without date filtering`() {
    // Given
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    anElSPExists(prisonNumber)
    aValidSupportStrategyExists(prisonNumber)
    aValidStrengthExists(prisonNumber)

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
    val content = objectMapper.convertValue(actual!!.content, SubjectAccessRequestContent::class.java)

    assertThat(content.originalEducationSupportPlan).isNotNull
    content.originalEducationSupportPlan!!.let { p ->
      assertThat(p.hasCurrentEhcp).isTrue()
      assertThat(p.individualSupport).isEqualTo("support")
      assertThat(p.teachingAdjustments).isEqualTo("teachingAdjustments")
      assertThat(p.specificTeachingSkills).isEqualTo("specificTeachingSkills")
      assertThat(p.examAccessArrangements).isEqualTo("examAccessArrangements")
      assertThat(p.lnspSupport).isEqualTo("lnspSupport")
      assertThat(p.lnspSupportHours).isEqualTo(2)
      assertThat(p.detail).isEqualTo("detail")
      assertThat(p.planCreatedBy?.name).isEqualTo("Tom Brown")
      assertThat(p.planCreatedBy?.jobRole).isEqualTo("Education coordinator")
      assertThat(p.otherContributors).hasSize(1)
      p.otherContributors!!.first().let { contributor ->
        assertThat(contributor.name).isEqualTo("Bob Smith")
        assertThat(contributor.jobRole).isEqualTo("Teacher")
      }
    }

    assertThat(content.supportStrategies).hasSize(1)
    content.supportStrategies!!.first().let { s ->
      assertThat(s.active).isTrue()
      assertThat(s.archiveReason).isNull()
      assertThat(s.supportStrategyType.categoryDescription).isEqualTo("Processing speed")
      assertThat(s.detail).isNull()
    }

    assertThat(content.strengths).hasSize(1)
    content.strengths!!.first().let { s ->
      assertThat(s.active).isTrue()
      assertThat(s.archiveReason).isNull()
      assertThat(s.strengthType.categoryDescription).isEqualTo("Memory")
      assertThat(s.strengthDescription).isEqualTo("StrengthSymptoms")
      assertThat(s.howIdentified).isEqualTo("wider prison, conversations")
      assertThat(s.howIdentifiedOther).isNull()
    }
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.LddAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.HasNeedResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.NeedSource
import java.time.LocalDate
import java.util.UUID

class HasNeedTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/has-need"
  }

  @Test
  fun `A prisoner has a condition, challenge and LDD assessment`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = sensory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    val alnAssessment = AlnAssessmentEntity(
      prisonNumber = prisonNumber,
      hasNeed = true,
      screeningDate = LocalDate.now(),
      curiousReference = UUID.randomUUID(),
    )
    alnAssessmentRepository.saveAndFlush(alnAssessment)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(3)
    assertThat(actual.needSources).containsExactlyInAnyOrder(NeedSource.CONDITION_SELF_DECLARED, NeedSource.CHALLENGE_NOT_ALN_SCREENER, NeedSource.ALN_SCREENER)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has a need that is a condition`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(1)
    assertThat(actual.needSources[0]).isEqualTo(NeedSource.CONDITION_SELF_DECLARED)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has a need that is a challenge`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CHALLENGE, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    challengeRepository.saveAndFlush(
      ChallengeEntity(
        prisonNumber = prisonNumber,
        challengeType = sensory,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(1)
    assertThat(actual.needSources[0]).isEqualTo(NeedSource.CHALLENGE_NOT_ALN_SCREENER)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has a need that is an aln assessment`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val alnAssessment = AlnAssessmentEntity(
      prisonNumber = prisonNumber,
      hasNeed = true,
      screeningDate = LocalDate.now(),
      curiousReference = UUID.randomUUID(),
    )
    alnAssessmentRepository.saveAndFlush(alnAssessment)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(1)
    assertThat(actual.needSources[0]).isEqualTo(NeedSource.ALN_SCREENER)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has an aln assessment and LDD assessment`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val alnAssessment = AlnAssessmentEntity(
      prisonNumber = prisonNumber,
      hasNeed = true,
      screeningDate = LocalDate.now(),
      curiousReference = UUID.randomUUID(),
    )
    alnAssessmentRepository.saveAndFlush(alnAssessment)

    val lddAssessment = LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true)
    lddAssessmentRepository.saveAndFlush(lddAssessment)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(1)
    assertThat(actual.needSources[0]).isEqualTo(NeedSource.ALN_SCREENER)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has an aln assessment with no need and LDD assessment`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val alnAssessment = AlnAssessmentEntity(
      prisonNumber = prisonNumber,
      hasNeed = false,
      screeningDate = LocalDate.now(),
      curiousReference = UUID.randomUUID(),
    )
    alnAssessmentRepository.saveAndFlush(alnAssessment)

    val lddAssessment = LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true)
    lddAssessmentRepository.saveAndFlush(lddAssessment)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.hasNeed).isFalse
    assertThat(actual.needSources.size).isZero
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has an LDD assessment`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val lddAssessment = LddAssessmentEntity(prisonNumber = prisonNumber, hasNeed = true)
    lddAssessmentRepository.saveAndFlush(lddAssessment)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    assertThat(actual!!.hasNeed).isTrue
    assertThat(actual.needSources.size).isEqualTo(1)
    assertThat(actual.needSources[0]).isEqualTo(NeedSource.LDD_SCREENER)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }

  @Test
  fun `A prisoner has a no need`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(HasNeedResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.hasNeed).isFalse
    assertThat(actual.needSources.size).isZero
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview/modal")
  }
}

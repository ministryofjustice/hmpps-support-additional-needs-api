package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreeners
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNStrength
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate
import java.util.UUID

class DeleteALNScreenerTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/aln-screener"
    private const val DEFAULT_QUERY = "?prisonId=BXI&reason=ENTERED_IN_ERROR"
  }

  @Test
  fun `delete the current ALN screener for a given prisoner with one screener`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    assertThat(alnScreenerRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
  }

  @Test
  fun `delete the current ALN screener when the prisoner has two screeners — previous becomes current and its strengths and challenges are unarchived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // Screener 1 (older — will become current after delete)
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "NWI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )

    aSmallPause()

    // Screener 2 (newer — current)
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-06-01"),
      challengeCodes = listOf("SPEED_OF_CALCULATION"),
      strengthCodes = listOf("SPATIAL_AWARENESS"),
    )

    // Sanity-check the pre-delete state via the public API
    val pre = getScreeners(prisonNumber)
    assertThat(pre.screeners).hasSize(2)
    val preCurrent = pre.screeners.first()
    assertThat(preCurrent.screenerDate).isEqualTo(LocalDate.parse("2020-06-01"))
    assertThat(preCurrent.challenges.map { it.active }).containsOnly(true)
    assertThat(preCurrent.strengths.map { it.active }).containsOnly(true)
    val prePrevious = pre.screeners.last()
    assertThat(prePrevious.challenges.map { it.active }).containsOnly(false)
    assertThat(prePrevious.strengths.map { it.active }).containsOnly(false)

    // When — delete the current screener
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then — only the older screener remains and is now current with re-activated children
    val post = getScreeners(prisonNumber)
    assertThat(post.screeners).hasSize(1)
    val current = post.screeners.first()
    assertThat(current.screenerDate).isEqualTo(LocalDate.parse("2020-01-01"))
    assertThat(current.createdAtPrison).isEqualTo("NWI")
    assertThat(current.challenges.map { it.challengeType.code }).containsExactly("MEMORY")
    assertThat(current.strengths.map { it.strengthType.code }).containsExactly("PEOPLE_PERSON")
    assertThat(current.challenges.map { it.active }).containsOnly(true)
    assertThat(current.strengths.map { it.active }).containsOnly(true)
    assertThat(current.challenges.map { it.archiveReason }).containsOnly(null)
    assertThat(current.strengths.map { it.archiveReason }).containsOnly(null)

    // Defensive — direct repository checks confirm screener 2's children were cascade-deleted
    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber).map { it.challengeType.key.code })
      .containsExactly("MEMORY")
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber).map { it.strengthType.key.code })
      .containsExactly("PEOPLE_PERSON")
  }

  @Test
  fun `delete with no screener for prisoner returns 404`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    val response = webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("ALN Screener not found for prisoner [$prisonNumber]")
  }

  @Test
  fun `delete the current ALN screener with no role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf(), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete the current ALN screener with read-only role`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When — read-only role cannot delete
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isForbidden
  }

  @Test
  fun `delete the current ALN screener without prisonId query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?reason=ENTERED_IN_ERROR", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete the current ALN screener without reason query param`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete the current ALN screener with an unknown reason value`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE?prisonId=BXI&reason=NOT_A_REAL_REASON", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isBadRequest
  }

  @Test
  fun `delete does not affect ALN screeners belonging to other prisoners`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonerA = randomValidPrisonNumber()
    val prisonerB = randomValidPrisonNumber()

    createScreener(
      prisonNumber = prisonerA,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )
    createScreener(
      prisonNumber = prisonerB,
      prisonId = "NWI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("SPEED_OF_CALCULATION"),
      strengthCodes = listOf("SPATIAL_AWARENESS"),
    )

    // When — delete prisonerA's screener
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonerA)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then — prisonerB is untouched
    val prisonerBScreeners = getScreeners(prisonerB)
    assertThat(prisonerBScreeners.screeners).hasSize(1)
    val prisonerBCurrent = prisonerBScreeners.screeners.first()
    assertThat(prisonerBCurrent.challenges.map { it.challengeType.code }).containsExactly("SPEED_OF_CALCULATION")
    assertThat(prisonerBCurrent.strengths.map { it.strengthType.code }).containsExactly("SPATIAL_AWARENESS")
    assertThat(challengeRepository.findAllByPrisonNumber(prisonerB)).hasSize(1)
    assertThat(strengthRepository.findAllByPrisonNumber(prisonerB)).hasSize(1)
  }

  @Test
  fun `delete does not touch AlnAssessmentEntity records`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val curiousReference = UUID.randomUUID()
    val assessmentDate = LocalDate.parse("2024-01-15")

    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )

    // A separate ALN assessment record (Curious-sourced) must NOT be touched
    alnAssessmentRepository.saveAndFlush(
      AlnAssessmentEntity(
        prisonNumber = prisonNumber,
        hasNeed = true,
        screeningDate = assessmentDate,
        curiousReference = curiousReference,
      ),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then — ALN screener is gone, but AlnAssessmentEntity is still present and unchanged
    assertThat(alnScreenerRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
    val assessment = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
    assertThat(assessment).isNotNull
    assertThat(assessment!!.curiousReference).isEqualTo(curiousReference)
    assertThat(assessment.screeningDate).isEqualTo(assessmentDate)
    assertThat(assessment.hasNeed).isTrue()
  }

  @Test
  fun `delete records an ALN_SCREENER_DELETED timeline event with reason in additionalInfo`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )

    // When
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    val deletionEntry = timelineEntries.firstOrNull { it.event == TimelineEventType.ALN_SCREENER_DELETED }
    assertThat(deletionEntry).isNotNull
    assertThat(deletionEntry!!.additionalInfo).isEqualTo("reason:ENTERED_IN_ERROR")
    assertThat(deletionEntry.createdAtPrison).isEqualTo("BXI")
  }

  @Test
  fun `delete the current ALN screener when the previous screener had no strengths and no challenges`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // Screener 1 — empty (no strengths, no challenges)
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "NWI",
      screenerDate = LocalDate.parse("2020-01-01"),
      challengeCodes = emptyList(),
      strengthCodes = emptyList(),
    )

    aSmallPause()

    // Screener 2 — with strengths and challenges (current)
    createScreener(
      prisonNumber = prisonNumber,
      prisonId = "BXI",
      screenerDate = LocalDate.parse("2020-06-01"),
      challengeCodes = listOf("MEMORY"),
      strengthCodes = listOf("PEOPLE_PERSON"),
    )

    // When — delete current; the now-current screener (empty) must not blow up the unarchive loop
    webTestClient.delete()
      .uri("$URI_TEMPLATE$DEFAULT_QUERY", prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val post = getScreeners(prisonNumber)
    assertThat(post.screeners).hasSize(1)
    val current = post.screeners.first()
    assertThat(current.screenerDate).isEqualTo(LocalDate.parse("2020-01-01"))
    assertThat(current.challenges).isEmpty()
    assertThat(current.strengths).isEmpty()
    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber)).isEmpty()
  }

  private fun createScreener(
    prisonNumber: String,
    prisonId: String,
    screenerDate: LocalDate,
    challengeCodes: List<String>,
    strengthCodes: List<String>,
  ) {
    val request = ALNScreenerRequest(
      prisonId = prisonId,
      screenerDate = screenerDate,
      challenges = challengeCodes.map { ALNChallenge(challengeTypeCode = it) },
      strengths = strengthCodes.map { ALNStrength(strengthTypeCode = it) },
    )
    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isCreated
  }

  private fun getScreeners(prisonNumber: String): ALNScreeners = webTestClient.get()
    .uri(URI_TEMPLATE, prisonNumber)
    .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
    .exchange()
    .expectStatus()
    .isOk
    .expectBody<ALNScreeners>()
    .returnResult()
    .responseBody!!
}

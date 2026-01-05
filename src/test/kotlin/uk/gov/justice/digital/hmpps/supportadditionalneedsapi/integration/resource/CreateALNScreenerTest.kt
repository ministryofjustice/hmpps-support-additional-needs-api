package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.test.web.reactive.server.expectBody
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreeners
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNStrength
import java.time.LocalDate

class CreateALNScreenerTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/aln-screener"
  }

  @Test
  fun `Create an aln screener for a given prisoner with needs`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    val strengthsList = createStrengthsList()
    val alnScreener = ALNScreenerRequest(prisonId = "BXI", strengths = strengthsList, challenges = challengesList, screenerDate = LocalDate.parse("2020-01-01"))

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener)
      .exchange()
      .expectStatus()
      .isCreated

    // Then

    val alnScreenerEntity = alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
    assertThat(alnScreenerEntity).isNotNull
    assertThat(alnScreenerEntity!!.needsIdentified).isTrue()
    assertThat(alnScreenerEntity.screeningDate).isEqualTo(LocalDate.parse("2020-01-01"))
    assertThat(alnScreenerEntity.hasStrengths).isTrue()
    assertThat(alnScreenerEntity.hasChallenges).isTrue()

    val savedChallenges = alnScreenerEntity.challenges
    assertThat(savedChallenges.size).isEqualTo(2)
    val memoryChallenge = savedChallenges.find { it.challengeType.key.code == "MEMORY" }
    assertThat(memoryChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(memoryChallenge.challengeType.key.code).isEqualTo("MEMORY")
    assertThat(memoryChallenge.fromALNScreener).isTrue()
    assertThat(memoryChallenge.createdAtPrison).isEqualTo("BXI")

    val processingSpeedChallenge = savedChallenges.find { it.challengeType.key.code == "SPEED_OF_CALCULATION" }
    assertThat(processingSpeedChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(processingSpeedChallenge.challengeType.key.code).isEqualTo("SPEED_OF_CALCULATION")
    assertThat(processingSpeedChallenge.fromALNScreener).isTrue()
    assertThat(processingSpeedChallenge.createdAtPrison).isEqualTo("BXI")

    val savedStrengths = alnScreenerEntity.strengths
    assertThat(savedStrengths.size).isEqualTo(2)
    val peoplePersonStrength = savedStrengths.find { it.strengthType.key.code == "PEOPLE_PERSON" }
    assertThat(peoplePersonStrength!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(peoplePersonStrength.strengthType.key.code).isEqualTo("PEOPLE_PERSON")
    assertThat(peoplePersonStrength.fromALNScreener).isTrue()
    assertThat(peoplePersonStrength.createdAtPrison).isEqualTo("BXI")

    val spacialAwarenessChallenge = savedStrengths.find { it.strengthType.key.code == "SPATIAL_AWARENESS" }
    assertThat(spacialAwarenessChallenge!!.prisonNumber).isEqualTo(prisonNumber)
    assertThat(spacialAwarenessChallenge.strengthType.key.code).isEqualTo("SPATIAL_AWARENESS")
    assertThat(spacialAwarenessChallenge.fromALNScreener).isTrue()
    assertThat(spacialAwarenessChallenge.createdAtPrison).isEqualTo("BXI")

    val timelineEntries = timelineRepository.findAllByPrisonNumberOrderByCreatedAt(prisonNumber)
    assertThat(timelineEntries[0].event).isEqualTo(TimelineEventType.ALN_CHALLENGE_ADDED)
    assertThat(timelineEntries[0].additionalInfo).isEqualTo("ChallengeType:MEMORY")
    assertThat(timelineEntries[1].event).isEqualTo(TimelineEventType.ALN_CHALLENGE_ADDED)
    assertThat(timelineEntries[1].additionalInfo).isEqualTo("ChallengeType:SPEED_OF_CALCULATION")
    assertThat(timelineEntries[2].event).isEqualTo(TimelineEventType.ALN_STRENGTH_ADDED)
    assertThat(timelineEntries[2].additionalInfo).isEqualTo("StrengthType:PEOPLE_PERSON")
    assertThat(timelineEntries[3].event).isEqualTo(TimelineEventType.ALN_STRENGTH_ADDED)
    assertThat(timelineEntries[3].additionalInfo).isEqualTo("StrengthType:SPATIAL_AWARENESS")
  }

  @Test
  fun `Create an aln screener for a given prisoner with no needs`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList: List<ALNChallenge> = listOf()
    val strengthsList: List<ALNStrength> = listOf()
    val alnScreener = ALNScreenerRequest(prisonId = "BXI", strengths = strengthsList, challenges = challengesList, screenerDate = LocalDate.parse("2020-01-01"))

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener)
      .exchange()
      .expectStatus()
      .isCreated

    // Then
    val alnScreenerEntity = alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
    assertThat(alnScreenerEntity).isNotNull
    assertThat(alnScreenerEntity!!.needsIdentified).isFalse()
    assertThat(alnScreenerEntity.hasStrengths).isFalse()
    assertThat(alnScreenerEntity.hasChallenges).isFalse()
    assertThat(alnScreenerEntity.screeningDate).isEqualTo(LocalDate.parse("2020-01-01"))

    val savedChallenges = alnScreenerEntity.challenges
    assertThat(savedChallenges.size).isEqualTo(0)

    val savedStrengths = alnScreenerEntity.strengths
    assertThat(savedStrengths.size).isEqualTo(0)
  }

  @Test
  fun `Get all ALN screeners for a prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    val strengthsList = createStrengthsList()
    val screenerDate = LocalDate.parse("2020-01-01")
    val alnScreener = ALNScreenerRequest(
      prisonId = "BXI",
      strengths = strengthsList,
      challenges = challengesList,
      screenerDate = screenerDate,
    )

    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener)
      .exchange()
      .expectStatus()
      .isCreated

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody(ALNScreeners::class.java)
      .returnResult()
      .responseBody!!

    // Then
    val screener = response.screeners.first()
    assertThat(screener.screenerDate).isEqualTo(screenerDate)
    assertThat(screener.createdAtPrison).isEqualTo("BXI")
    assertThat(screener.challenges.map { it.challengeType.code }).containsExactlyInAnyOrder("MEMORY", "SPEED_OF_CALCULATION")
    assertThat(screener.strengths.map { it.strengthType.code }).containsExactlyInAnyOrder("PEOPLE_PERSON", "SPATIAL_AWARENESS")
    assertThat(screener.createdBy).isEqualTo("testuser")
    assertThat(screener.createdByDisplayName).isEqualTo("Test User")
    assertThat(screener.challenges.map { it.alnScreenerDate }).containsOnly(screenerDate)
    assertThat(screener.strengths.map { it.alnScreenerDate }).containsOnly(screenerDate)
  }

  @Test
  fun `test cascade delete of challenges and strengths`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    val strengthsList = createStrengthsList()
    val alnScreener = ALNScreenerRequest(prisonId = "BXI", strengths = strengthsList, challenges = challengesList, screenerDate = LocalDate.parse("2020-01-01"))

    // When
    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener)
      .exchange()
      .expectStatus()
      .isCreated

    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber).size).isGreaterThan(0)
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber).size).isGreaterThan(0)

    // Then
    val screener = alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
    alnScreenerRepository.deleteById(screener!!.id)
    alnScreenerRepository.flush()

    assertThat(challengeRepository.findAllByPrisonNumber(prisonNumber).size).isZero()
    assertThat(strengthRepository.findAllByPrisonNumber(prisonNumber).size).isZero()
  }

  @Test
  fun `Create two ALN screeners for a prisoner with the same screening date`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val challengesList = createChallengesList()
    val strengthsList = createStrengthsList()
    val screenerDate = LocalDate.parse("2020-01-01")
    val alnScreener1 = ALNScreenerRequest(
      prisonId = "NWI",
      strengths = strengthsList,
      challenges = challengesList,
      screenerDate = screenerDate,
    )
    val alnScreener2 = ALNScreenerRequest(
      prisonId = "BXI",
      strengths = strengthsList,
      challenges = challengesList,
      screenerDate = screenerDate,
    )

    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener1)
      .exchange()
      .expectStatus()
      .isCreated

    aSmallPause()

    webTestClient.post()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(alnScreener2)
      .exchange()
      .expectStatus()
      .isCreated

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .expectBody<ALNScreeners>()
      .returnResult()
      .responseBody!!

    // Then
    val latestScreener = response.screeners.first()
    assertThat(latestScreener.screenerDate).isEqualTo(screenerDate)
    assertThat(latestScreener.createdAtPrison).isEqualTo("BXI")
    assertThat(latestScreener.challenges.map { it.challengeType.code }).containsExactlyInAnyOrder("MEMORY", "SPEED_OF_CALCULATION")
    assertThat(latestScreener.strengths.map { it.strengthType.code }).containsExactlyInAnyOrder("PEOPLE_PERSON", "SPATIAL_AWARENESS")
    assertThat(latestScreener.createdBy).isEqualTo("testuser")
    assertThat(latestScreener.createdByDisplayName).isEqualTo("Test User")
    assertThat(latestScreener.challenges.map { it.alnScreenerDate }).containsOnly(screenerDate)
    assertThat(latestScreener.strengths.map { it.alnScreenerDate }).containsOnly(screenerDate)
    assertThat(latestScreener.challenges.map { it.active }).containsOnly(true)
    assertThat(latestScreener.strengths.map { it.active }).containsOnly(true)

    val oldScreener = response.screeners.last()
    assertThat(oldScreener.screenerDate).isEqualTo(screenerDate)
    assertThat(oldScreener.createdAtPrison).isEqualTo("NWI")
    assertThat(oldScreener.challenges.map { it.challengeType.code }).containsExactlyInAnyOrder("MEMORY", "SPEED_OF_CALCULATION")
    assertThat(oldScreener.strengths.map { it.strengthType.code }).containsExactlyInAnyOrder("PEOPLE_PERSON", "SPATIAL_AWARENESS")
    assertThat(oldScreener.createdBy).isEqualTo("testuser")
    assertThat(oldScreener.createdByDisplayName).isEqualTo("Test User")
    assertThat(oldScreener.challenges.map { it.alnScreenerDate }).containsOnly(screenerDate)
    assertThat(oldScreener.strengths.map { it.alnScreenerDate }).containsOnly(screenerDate)

    assertThat(oldScreener.challenges.map { it.active }).containsOnly(false)
    assertThat(oldScreener.strengths.map { it.active }).containsOnly(false)
  }

  private fun createChallengesList(): List<ALNChallenge> = listOf(
    ALNChallenge(
      challengeTypeCode = "MEMORY",
    ),
    ALNChallenge(
      challengeTypeCode = "SPEED_OF_CALCULATION",
    ),
  )

  private fun createStrengthsList(): List<ALNStrength> = listOf(
    ALNStrength(
      strengthTypeCode = "PEOPLE_PERSON",
    ),
    ALNStrength(
      strengthTypeCode = "SPATIAL_AWARENESS",
    ),
  )
}

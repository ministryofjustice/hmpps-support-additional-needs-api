package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNChallenge
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest
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

    val alnScreenerEntity = alnScreenerRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
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
    val alnScreenerEntity = alnScreenerRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
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

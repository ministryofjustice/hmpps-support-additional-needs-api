package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import java.time.LocalDate

class GetStrengthsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/strengths"
  }

  @Test
  fun `Get list of non screener strengths for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    nonAlnStrengthsExist(prisonNumber)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.strengths).hasSize(2)

    val strengthCodes = actual.strengths.map { it.strengthType.code }
    assertThat(strengthCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.strengths.forEach {
      assertThat(it.fromALNScreener).isFalse()
    }
  }

  @Test
  fun `Get list of non screener strengths for a given prisoner where one is archived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

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
          archiveReason = "archive reason",
          active = false,
        ),
      ),
    )

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.strengths).hasSize(2)

    val strengthCodes = actual.strengths.map { it.strengthType.code }
    assertThat(strengthCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.strengths.forEach {
      assertThat(it.fromALNScreener).isFalse()
    }
    val archivedStrength = actual.strengths.first { it.strengthType.code == "MEMORY" }
    assertThat(archivedStrength.active).isFalse()
    assertThat(archivedStrength.archiveReason).isEqualTo("archive reason")
  }

  @Test
  fun `Get list of screener strengths for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    val alnScreener = alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2020-01-01")))
    strengthRepository.saveAll(
      listOf(
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
      ),
    )

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.strengths).hasSize(2)

    val strengthCodes = actual.strengths.map { it.strengthType.code }
    assertThat(strengthCodes).containsExactlyInAnyOrder("MEMORY", "SENSORY_PROCESSING")
    actual.strengths.forEach {
      assertThat(it.fromALNScreener).isTrue()
    }
  }

  @Test
  fun `Return no strengths for a given prisoner when the latest screener has none`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val memory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "MEMORY"))
      ?: throw IllegalStateException("Reference data not found")
    val sensory = referenceDataRepository.findByKey(ReferenceDataKey(Domain.STRENGTH, "SENSORY_PROCESSING"))
      ?: throw IllegalStateException("Reference data not found")
    // screener 1 has strengths
    val alnScreener = alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2020-01-01")))
    strengthRepository.saveAll(
      listOf(
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = sensory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
        StrengthEntity(
          prisonNumber = prisonNumber,
          strengthType = memory,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
          alnScreenerId = alnScreener.id,
        ),
      ),
    )
    // screener2 - has no strengths needs to be a small delay or they will both have the same timestamp
    Thread.sleep(100)
    alnScreenerRepository.saveAndFlush(ALNScreenerEntity(prisonNumber = prisonNumber, createdAtPrison = "BXI", updatedAtPrison = "BXI", screeningDate = LocalDate.parse("2022-01-01")))

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.strengths).hasSize(0)
  }

  @Test
  fun `Return empty list when no strengths exist for prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    // No strengths are created or saved for this prison number

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(StrengthListResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()
    assertThat(actual!!.strengths).isEmpty()
  }
}

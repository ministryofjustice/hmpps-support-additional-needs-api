package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class ArchiveConditionTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/conditions/{reference}/archive"
  }

  @Test
  fun `archive a condition for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
      ),
    )

    val request = ArchiveConditionRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNoContent

    // Then
    val conditionEntities = conditionRepository.findAllByPrisonNumber(prisonNumber)

    assertThat(conditionEntities).hasSize(1)
    assertThat(conditionEntities.first().active).isEqualTo(false)
    assertThat(conditionEntities.first().archiveReason).isEqualTo("archive reason")
  }

  @Test
  fun `attempt to archive a condition that is already archived`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")

    val condition = conditionRepository.saveAndFlush(
      ConditionEntity(
        prisonNumber = prisonNumber,
        source = Source.SELF_DECLARED,
        conditionType = adhd,
        createdAtPrison = "BXI",
        updatedAtPrison = "BXI",
        active = false,
        archiveReason = "archived condition",
      ),
    )

    val request = ArchiveChallengeRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .is4xxClientError
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.CONFLICT.value())
      .hasUserMessage("Condition with reference [${condition.reference}] has been archived for prisoner [$prisonNumber]")
  }

  @Test
  fun `attempt to archive a condition that does not exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()
    val ref = UUID.randomUUID().toString()
    val request = ArchiveConditionRequest(prisonId = "BXI", archiveReason = "archive reason")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(request)
      .exchange()
      .expectStatus()
      .isNotFound
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.NOT_FOUND.value())
      .hasUserMessage("Condition with reference [$ref] not found for prisoner [$prisonNumber]")
  }
}

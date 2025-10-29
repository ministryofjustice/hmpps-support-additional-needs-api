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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Source.CONFIRMED_DIAGNOSIS
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.util.*

class UpdateConditionsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/conditions/{reference}"
  }

  @Test
  fun `update a condition for a given prisoner`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val adhd = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "ADHD"))
      ?: throw IllegalStateException("Reference data not found")
    val dyslexia = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "DYSLEXIA"))
      ?: throw IllegalStateException("Reference data not found")
    val mentalHealth = referenceDataRepository.findByKey(ReferenceDataKey(Domain.CONDITION, "MENTAL_HEALTH"))
      ?: throw IllegalStateException("Reference data not found")

    val conditions = conditionRepository.saveAll(
      listOf(
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = adhd,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.SELF_DECLARED,
          conditionType = dyslexia,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
        ConditionEntity(
          prisonNumber = prisonNumber,
          source = Source.CONFIRMED_DIAGNOSIS,
          conditionType = mentalHealth,
          createdAtPrison = "BXI",
          updatedAtPrison = "BXI",
        ),
      ),
    )

    val condition = conditions.find { it.conditionType.code == "ADHD" } ?: throw IllegalStateException("condition not found")
    val updateConditionRequest = UpdateConditionRequest(source = CONFIRMED_DIAGNOSIS, conditionDetails = "updated condition details", prisonId = "FKL")

    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, condition.reference)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateConditionRequest)
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(ConditionResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull()

    val updatedCondition =
      conditionRepository.findAllByPrisonNumber(prisonNumber).find { it.conditionType.code == "ADHD" }
        ?: throw IllegalStateException("condition not found")

    assertThat(updatedCondition.conditionDetails).isEqualTo("updated condition details")
    assertThat(updatedCondition.source).isEqualTo(Source.CONFIRMED_DIAGNOSIS)
    assertThat(updatedCondition.updatedAtPrison).isEqualTo("FKL")
  }

  @Test
  fun `attempt to update a condition where a condition doesnt exist`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    val updateConditionRequest = UpdateConditionRequest(source = CONFIRMED_DIAGNOSIS, conditionDetails = "updated condition details", prisonId = "FKL")

    val ref = UUID.randomUUID().toString()
    // When
    val response = webTestClient.put()
      .uri(URI_TEMPLATE, prisonNumber, ref)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW"), username = "testuser"))
      .bodyValue(updateConditionRequest)
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

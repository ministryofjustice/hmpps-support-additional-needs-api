package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.customPlanStatusOrder
import java.time.LocalDate

class SearchSanResultsTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/search/prisons/{prisonId}/people"
    private const val PRISON_ID = "BXI"

    private val PRISONER_1 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_2 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_3 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_4 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_5 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_6 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_7 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_8 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    private val PRISONER_9 = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())

    private val PRISONERS_IN_PRISON =
      listOf(PRISONER_1, PRISONER_2, PRISONER_3, PRISONER_4, PRISONER_5, PRISONER_6, PRISONER_7, PRISONER_8, PRISONER_9)
  }

  @Test
  fun `should return results for each plan status`() {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetPrisonersInPrisonFromPrisonerSearchApi(PRISON_ID, PRISONERS_IN_PRISON)
    stubForBankHoliday()
    // set up each person to have a specific status
    // needsPlan
    // PRISONER_1
    prisonerInEducation(PRISONER_1.prisonerNumber)
    prisonerHasNeed(PRISONER_1.prisonerNumber)

    // PlanDue
    // PRISONER_2
    prisonerInEducation(PRISONER_2.prisonerNumber)
    prisonerHasNeed(PRISONER_2.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_2.prisonerNumber, deadlineDate = LocalDate.now().plusDays(1))

    // ReviewDue
    // PRISONER_3
    prisonerInEducation(PRISONER_3.prisonerNumber)
    prisonerHasNeed(PRISONER_3.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_3.prisonerNumber, deadlineDate = LocalDate.now().plusDays(1))

    // ActivePlan
    // PRISONER_4
    prisonerInEducation(PRISONER_4.prisonerNumber)
    prisonerHasNeed(PRISONER_4.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_4.prisonerNumber, deadlineDate = LocalDate.now().plusWeeks(2))
    anElSPExists(PRISONER_4.prisonerNumber)

    // PlanOverDue
    // PRISONER_5
    prisonerInEducation(PRISONER_5.prisonerNumber)
    prisonerHasNeed(PRISONER_5.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_5.prisonerNumber, deadlineDate = LocalDate.now().minusDays(5))

    // ReviewOverDue
    // PRISONER_6
    prisonerInEducation(PRISONER_6.prisonerNumber)
    prisonerHasNeed(PRISONER_6.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_6.prisonerNumber, deadlineDate = LocalDate.now().minusDays(5))

    // Inactive plan
    // PRISONER_7
    prisonerHasNeed(PRISONER_7.prisonerNumber)
    anElSPExists(PRISONER_7.prisonerNumber)

    // Declined plan
    // PRISONER_8
    prisonerHasNeed(PRISONER_8.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_8.prisonerNumber, deadlineDate = LocalDate.now().minusDays(5), status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY)

    // No plan
    // PRISONER_9
    // no data in san

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SearchByPrisonResponse::class.java)

    // Then
    val body = response.responseBody.blockFirst()!!

    val prisoner1PlanStatus = body.people.first { it.prisonNumber == PRISONER_1.prisonerNumber }.planStatus
    assertEquals(PlanStatus.NEEDS_PLAN, prisoner1PlanStatus)

    val prisoner2PlanStatus = body.people.first { it.prisonNumber == PRISONER_2.prisonerNumber }.planStatus
    assertEquals(PlanStatus.PLAN_DUE, prisoner2PlanStatus)

    val prisoner3PlanStatus = body.people.first { it.prisonNumber == PRISONER_3.prisonerNumber }.planStatus
    assertEquals(PlanStatus.REVIEW_DUE, prisoner3PlanStatus)

    val prisoner4PlanStatus = body.people.first { it.prisonNumber == PRISONER_4.prisonerNumber }.planStatus
    assertEquals(PlanStatus.ACTIVE_PLAN, prisoner4PlanStatus)

    val prisoner5PlanStatus = body.people.first { it.prisonNumber == PRISONER_5.prisonerNumber }.planStatus
    assertEquals(PlanStatus.PLAN_OVERDUE, prisoner5PlanStatus)

    val prisoner6PlanStatus = body.people.first { it.prisonNumber == PRISONER_6.prisonerNumber }.planStatus
    assertEquals(PlanStatus.REVIEW_OVERDUE, prisoner6PlanStatus)

    val prisoner7PlanStatus = body.people.first { it.prisonNumber == PRISONER_7.prisonerNumber }.planStatus
    assertEquals(PlanStatus.INACTIVE_PLAN, prisoner7PlanStatus)

    val prisoner8PlanStatus = body.people.first { it.prisonNumber == PRISONER_8.prisonerNumber }.planStatus
    assertEquals(PlanStatus.PLAN_DECLINED, prisoner8PlanStatus)

    val prisoner9PlanStatus = body.people.first { it.prisonNumber == PRISONER_9.prisonerNumber }.planStatus
    assertEquals(PlanStatus.NO_PLAN, prisoner9PlanStatus)

    val expectedOrder = body.people.sortedBy { customPlanStatusOrder[it.planStatus] }.map { it.prisonNumber }
    val actualOrder = body.people.map { it.prisonNumber }

    assertEquals(expectedOrder, actualOrder, "Results are not ordered by custom planStatus order")
  }
}

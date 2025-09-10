package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import java.time.LocalDate
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("GET /profile/{prisonNumber}/plan-action-status")
class PlanActionStatusTest : IntegrationTestBase() {

  private companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/plan-action-status"

    val PRISONER_1: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_2: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_3: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_4: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_5: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_6: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_7: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_8: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_9: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_10: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
    val PRISONER_11: Prisoner = aValidPrisoner(prisonerNumber = randomValidPrisonNumber())
  }

  @BeforeAll
  fun beforeAll() {
    // common stubs
    stubGetTokenFromHmppsAuth()
    stubForBankHoliday()
    stubGetDisplayName("testuser")

    // data
    setUpData()
  }

  @ParameterizedTest(name = "{index} → {0} should have status {1}")
  @MethodSource("prisonerStatusCases")
  fun `returns expected plan action status`(prisoner: Prisoner, expected: PlanStatus) {
    val body = fetch(prisoner)
    assertEquals(expected, body.status)
  }

  private fun prisonerStatusCases(): Stream<Arguments> = Stream.of(
    Arguments.of(PRISONER_1, PlanStatus.NEEDS_PLAN),
    Arguments.of(PRISONER_2, PlanStatus.PLAN_DUE),
    Arguments.of(PRISONER_3, PlanStatus.REVIEW_DUE),
    Arguments.of(PRISONER_4, PlanStatus.ACTIVE_PLAN),
    Arguments.of(PRISONER_5, PlanStatus.PLAN_OVERDUE),
    Arguments.of(PRISONER_6, PlanStatus.REVIEW_OVERDUE),
    Arguments.of(PRISONER_7, PlanStatus.INACTIVE_PLAN),
    Arguments.of(PRISONER_8, PlanStatus.PLAN_DECLINED),
    Arguments.of(PRISONER_9, PlanStatus.NO_PLAN),
    Arguments.of(PRISONER_10, PlanStatus.NO_PLAN),
    Arguments.of(PRISONER_11, PlanStatus.NO_PLAN),
  )

  // --- Helpers ----------------------------------------------------------------------------------

  private fun fetch(prisoner: Prisoner): PlanActionStatus = webTestClient.get()
    .uri(URI_TEMPLATE, prisoner.prisonerNumber)
    .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
    .exchange()
    .expectStatus().isOk
    .expectBody(PlanActionStatus::class.java)
    .returnResult()
    .responseBody!!

  private fun setUpData() {
    val today = LocalDate.now()

    // needsPlan -> PRISONER_1
    prisonerInEducation(PRISONER_1.prisonerNumber)
    prisonerHasNeed(PRISONER_1.prisonerNumber)
    aValidPlanCreationScheduleExists(
      prisonNumber = PRISONER_1.prisonerNumber,
      deadlineDate = IN_THE_FUTURE_DATE,
    )

    // planDue -> PRISONER_2
    prisonerInEducation(PRISONER_2.prisonerNumber)
    prisonerHasNeed(PRISONER_2.prisonerNumber)
    aValidPlanCreationScheduleExists(
      prisonNumber = PRISONER_2.prisonerNumber,
      deadlineDate = today.plusDays(1),
    )

    // reviewDue -> PRISONER_3
    prisonerInEducation(PRISONER_3.prisonerNumber)
    prisonerHasNeed(PRISONER_3.prisonerNumber)
    aValidReviewScheduleExists(
      prisonNumber = PRISONER_3.prisonerNumber,
      deadlineDate = today.plusDays(1),
    )

    // activePlan -> PRISONER_4
    prisonerInEducation(PRISONER_4.prisonerNumber)
    prisonerHasNeed(PRISONER_4.prisonerNumber)
    aValidReviewScheduleExists(
      prisonNumber = PRISONER_4.prisonerNumber,
      deadlineDate = today.plusWeeks(2),
    )
    anElSPExists(PRISONER_4.prisonerNumber)

    // planOverDue -> PRISONER_5
    prisonerInEducation(PRISONER_5.prisonerNumber)
    prisonerHasNeed(PRISONER_5.prisonerNumber)
    aValidPlanCreationScheduleExists(
      prisonNumber = PRISONER_5.prisonerNumber,
      deadlineDate = today.minusDays(5),
    )

    // reviewOverDue -> PRISONER_6
    prisonerInEducation(PRISONER_6.prisonerNumber)
    prisonerHasNeed(PRISONER_6.prisonerNumber)
    aValidReviewScheduleExists(
      prisonNumber = PRISONER_6.prisonerNumber,
      deadlineDate = today.minusDays(5),
    )

    // inactive plan -> PRISONER_7
    prisonerHasNeed(PRISONER_7.prisonerNumber)
    anElSPExists(PRISONER_7.prisonerNumber)

    // declined plan -> PRISONER_8
    prisonerHasNeed(PRISONER_8.prisonerNumber)
    aValidPlanCreationScheduleExists(
      prisonNumber = PRISONER_8.prisonerNumber,
      deadlineDate = today.minusDays(5),
      status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY,
    )

    // PRISONER_9 -> no data in SAN

    // PRISONER_10 -> no need but in education.
    prisonerInEducation(PRISONER_10.prisonerNumber)

    // PRISONER_11 -> has need but not in education.
    prisonerHasNeed(PRISONER_11.prisonerNumber)
  }
}

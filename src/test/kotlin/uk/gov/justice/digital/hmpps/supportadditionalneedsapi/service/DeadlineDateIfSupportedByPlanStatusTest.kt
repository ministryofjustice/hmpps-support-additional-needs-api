package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import java.time.LocalDate

class DeadlineDateIfSupportedByPlanStatusTest {

  @ParameterizedTest
  @CsvSource(
    value = ["NO_PLAN", "INACTIVE_PLAN", "PLAN_DECLINED", "NEEDS_PLAN"],
  )
  fun `should return null given plan status does not support deadline dates`(planStatus: PlanStatus) {
    // Given
    val deadlineDate = LocalDate.now()

    // When
    val actual = deadlineDateIfSupportedByPlanStatus(planStatus, deadlineDate)

    // Then
    assertThat(actual).isNull()
  }

  @ParameterizedTest
  @CsvSource(
    value = ["PLAN_OVERDUE", "PLAN_DUE", "REVIEW_OVERDUE", "REVIEW_DUE", "ACTIVE_PLAN"],
  )
  fun `should return deadlineDate given plan status supports deadline dates`(planStatus: PlanStatus) {
    // Given
    val deadlineDate = LocalDate.now()

    // When
    val actual = deadlineDateIfSupportedByPlanStatus(planStatus, deadlineDate)

    // Then
    assertThat(actual).isEqualTo(deadlineDate)
  }

  @ParameterizedTest
  @CsvSource(
    value = ["PLAN_OVERDUE", "PLAN_DUE", "REVIEW_OVERDUE", "REVIEW_DUE", "ACTIVE_PLAN"],
  )
  fun `should return null given plan status supports deadline dates but deadline date is 2099-12-31`(planStatus: PlanStatus) {
    // Given
    val deadlineDate = LocalDate.parse("2099-12-31")

    // When
    val actual = deadlineDateIfSupportedByPlanStatus(planStatus, deadlineDate)

    // Then
    assertThat(actual).isNull()
  }
}

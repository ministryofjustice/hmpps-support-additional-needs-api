package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate

fun assertThat(actual: PlanActionStatus?) = PlanActionStatusAssert(actual)

/**
 * AssertJ custom assertion for a single [PlanActionStatus].
 */
class PlanActionStatusAssert(actual: PlanActionStatus?) :
  AbstractObjectAssert<PlanActionStatusAssert, PlanActionStatus?>(
    actual,
    PlanActionStatusAssert::class.java,
  ) {

  fun hasStatus(expectedStatus: PlanStatus) = apply {
    isNotNull
    with(actual!!) {
      if (status != expectedStatus) {
        failWithMessage("Expected status to be <$expectedStatus> but was <$status>")
      }
    }
  }

  fun hasNoPlanCreationDeadlineDate() = apply {
    isNotNull
    with(actual!!) {
      if (planCreationDeadlineDate != null) {
        failWithMessage("Expected planCreationDeadlineDate to be <null> but was <$planCreationDeadlineDate>")
      }
    }
  }

  fun hasPlanCreationDeadlineDate(expectedPlanCreationDeadlineDate: LocalDate) = apply {
    isNotNull
    with(actual!!) {
      if (planCreationDeadlineDate != expectedPlanCreationDeadlineDate) {
        failWithMessage("Expected planCreationDeadlineDate to be <$expectedPlanCreationDeadlineDate> but was <$planCreationDeadlineDate>")
      }
    }
  }

  fun hasNoReviewDeadlineDate() = apply {
    isNotNull
    with(actual!!) {
      if (reviewDeadlineDate != null) {
        failWithMessage("Expected reviewDeadlineDate to be <null> but was <$reviewDeadlineDate>")
      }
    }
  }
}

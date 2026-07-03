package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.util.UUID

fun assertThat(actual: ReviewScheduleResponse?) = ReviewScheduleResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [ReviewScheduleResponse].
 */
class ReviewScheduleResponseAssert(actual: ReviewScheduleResponse?) :
  AbstractObjectAssert<ReviewScheduleResponseAssert, ReviewScheduleResponse?>(
    actual,
    ReviewScheduleResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun hasDeadlineDate(expected: LocalDate): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (deadlineDate != expected) {
        failWithMessage("Expected deadlineDate to be $expected, but was $deadlineDate")
      }
    }
    return this
  }

  fun hasStatus(expected: ReviewScheduleStatus): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (status != expected) {
        failWithMessage("Expected status to be $expected, but was $status")
      }
    }
    return this
  }

  fun hasExemptionReason(expected: String): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionReason != expected) {
        failWithMessage("Expected exemptionReason to be $expected, but was $exemptionReason")
      }
    }
    return this
  }

  fun hasNoExemptionReason(): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionReason != null) {
        failWithMessage("Expected no exemptionReason, but was $exemptionReason")
      }
    }
    return this
  }

  fun hasVersion(expected: Int): ReviewScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (version != expected) {
        failWithMessage("Expected version to be $expected, but was $version")
      }
    }
    return this
  }
}

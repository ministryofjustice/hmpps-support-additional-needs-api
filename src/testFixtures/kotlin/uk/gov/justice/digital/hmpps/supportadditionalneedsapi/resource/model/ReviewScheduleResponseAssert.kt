package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: ReviewScheduleResponse?) = ReviewScheduleResponseAssert(actual)
fun assertThat(actual: ReviewSchedulesResponse?) = ReviewSchedulesResponseAssert(actual)

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

class ReviewSchedulesResponseAssert(actual: ReviewSchedulesResponse?) :
  AbstractObjectAssert<ReviewSchedulesResponseAssert, ReviewSchedulesResponse?>(
    actual,
    ReviewSchedulesResponseAssert::class.java,
  ) {

  fun hasNumberOfReviewSchedules(expected: Int): ReviewSchedulesResponseAssert {
    isNotNull
    with(actual!!) {
      if (reviewSchedules.size != expected) {
        failWithMessage("Expected ReviewSchedulesResponse to be have $expected review schedules, but has ${reviewSchedules.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ReviewScheduleResponse]. Takes a lambda as the method
   * argument to call assertion methods provided by [ReviewScheduleResponseAssert].
   * Returns this [ReviewSchedulesResponseAssert] to allow further chained assertions on the parent [ReviewSchedulesResponse]
   *
   * The `reviewScheduleNumber` parameter is not zero indexed to make for better readability in tests. IE. the first
   * review schedule should be referenced as `.reviewSchedule(1) { .... }`
   */
  fun reviewSchedule(reviewScheduleNumber: Int, consumer: Consumer<ReviewScheduleResponseAssert>): ReviewSchedulesResponseAssert {
    isNotNull
    with(actual!!) {
      val reviewSchedule = reviewSchedules[reviewScheduleNumber - 1]
      consumer.accept(assertThat(reviewSchedule))
    }
    return this
  }
}

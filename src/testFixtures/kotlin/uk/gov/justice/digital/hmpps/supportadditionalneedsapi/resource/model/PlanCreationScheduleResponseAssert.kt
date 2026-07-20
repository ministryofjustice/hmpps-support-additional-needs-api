package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: PlanCreationScheduleResponse?) = PlanCreationScheduleResponseAssert(actual)
fun assertThat(actual: PlanCreationSchedulesResponse?) = PlanCreationSchedulesResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [PlanCreationScheduleResponse].
 */
class PlanCreationScheduleResponseAssert(actual: PlanCreationScheduleResponse?) :
  AbstractObjectAssert<PlanCreationScheduleResponseAssert, PlanCreationScheduleResponse?>(
    actual,
    PlanCreationScheduleResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun hasDeadlineDate(expected: LocalDate): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (deadlineDate != expected) {
        failWithMessage("Expected deadlineDate to be $expected, but was $deadlineDate")
      }
    }
    return this
  }

  fun hasStatus(expected: PlanCreationStatus): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (status != expected) {
        failWithMessage("Expected status to be $expected, but was $status")
      }
    }
    return this
  }

  fun hasExemptionReason(expected: PlanCreationScheduleExemptionReason): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionReason != expected) {
        failWithMessage("Expected exemptionReason to be $expected, but was $exemptionReason")
      }
    }
    return this
  }

  fun hasNoExemptionReason(): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionReason != null) {
        failWithMessage("Expected no exemptionReason, but was $exemptionReason")
      }
    }
    return this
  }

  fun hasExemptionDetail(expected: String): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionDetail != expected) {
        failWithMessage("Expected exemptionDetail to be $expected, but was $exemptionDetail")
      }
    }
    return this
  }

  fun hasNoExemptionDetail(): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (exemptionDetail != null) {
        failWithMessage("Expected no exemptionDetail, but was $exemptionDetail")
      }
    }
    return this
  }

  fun hasVersion(expected: Int): PlanCreationScheduleResponseAssert {
    isNotNull
    with(actual!!) {
      if (version != expected) {
        failWithMessage("Expected version to be $expected, but was $version")
      }
    }
    return this
  }
}

class PlanCreationSchedulesResponseAssert(actual: PlanCreationSchedulesResponse?) :
  AbstractObjectAssert<PlanCreationSchedulesResponseAssert, PlanCreationSchedulesResponse?>(
    actual,
    PlanCreationSchedulesResponseAssert::class.java,
  ) {

  fun hasNumberOfPlanCreationSchedules(expected: Int): PlanCreationSchedulesResponseAssert {
    isNotNull
    with(actual!!) {
      if (planCreationSchedules.size != expected) {
        failWithMessage("Expected PlanCreationSchedulesResponse to be have $expected plan creation schedules, but has ${planCreationSchedules.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [PlanCreationScheduleResponse]. Takes a lambda as the method
   * argument to call assertion methods provided by [PlanCreationScheduleResponseAssert].
   * Returns this [PlanCreationSchedulesResponseAssert] to allow further chained assertions on the parent [PlanCreationSchedulesResponse]
   *
   * The `planCreationScheduleNumber` parameter is not zero indexed to make for better readability in tests. IE. the first
   * plan creation schedule should be referenced as `.planCreationSchedule(1) { .... }`
   */
  fun planCreationSchedule(planCreationScheduleNumber: Int, consumer: Consumer<PlanCreationScheduleResponseAssert>): PlanCreationSchedulesResponseAssert {
    isNotNull
    with(actual!!) {
      val planCreationSchedule = planCreationSchedules[planCreationScheduleNumber - 1]
      consumer.accept(assertThat(planCreationSchedule))
    }
    return this
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractAssert
import java.time.LocalDate

fun assertThat(actual: Person?) = PersonAssert(actual)

class PersonAssert(actual: Person?) : AbstractAssert<PersonAssert, Person?>(actual, PersonAssert::class.java) {
  fun hasPrisonNumber(expected: String): PersonAssert {
    isNotNull
    with(actual!!) {
      if (prisonNumber != expected) {
        failWithMessage("Expected prisonNumber to be $expected, but was $prisonNumber")
      }
    }
    return this
  }

  fun hasForename(expected: String): PersonAssert {
    isNotNull
    with(actual!!) {
      if (forename != expected) {
        failWithMessage("Expected forename to be $expected, but was $forename")
      }
    }
    return this
  }

  fun hasSurname(expected: String): PersonAssert {
    isNotNull
    with(actual!!) {
      if (surname != expected) {
        failWithMessage("Expected surname to be $expected, but was $surname")
      }
    }
    return this
  }

  fun hasPlanStatus(expected: PlanStatus): PersonAssert {
    isNotNull
    with(actual!!) {
      if (planStatus != expected) {
        failWithMessage("Expected planStatus to be $expected, but was $planStatus")
      }
    }
    return this
  }

  fun hasReleaseDate(expected: LocalDate): PersonAssert {
    isNotNull
    with(actual!!) {
      if (releaseDate == null || !releaseDate.isEqual(expected)) {
        failWithMessage("Expected releaseDate to be $expected, but was $releaseDate")
      }
    }
    return this
  }

  fun hasNoReleaseDate(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (releaseDate != null) {
        failWithMessage("Expected releaseDate to be null, but was $releaseDate")
      }
    }
    return this
  }

  fun hasDeadlineDate(expected: LocalDate): PersonAssert {
    isNotNull
    with(actual!!) {
      if (deadlineDate == null || !deadlineDate.isEqual(expected)) {
        failWithMessage("Expected deadlineDate to be $expected, but was $deadlineDate")
      }
    }
    return this
  }

  fun hasNoDeadlineDate(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (deadlineDate != null) {
        failWithMessage("Expected deadlineDate to be null, but was $deadlineDate")
      }
    }
    return this
  }

  fun hasCellLocation(expected: String): PersonAssert {
    isNotNull
    with(actual!!) {
      if (!cellLocation.equals(expected)) {
        failWithMessage("Expected cellLocation to be $expected, but was $cellLocation")
      }
    }
    return this
  }

  fun hasNoCellLocation(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (cellLocation != null) {
        failWithMessage("Expected cellLocation to be null, but was $cellLocation")
      }
    }
    return this
  }

  fun isInEducation(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (!inEducation) {
        failWithMessage("Expected inEducation to be true, but was false")
      }
    }
    return this
  }

  fun isNotInEducation(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (inEducation) {
        failWithMessage("Expected inEducation to be false, but was true")
      }
    }
    return this
  }

  fun hasAdditionalNeed(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (!hasAdditionalNeed) {
        failWithMessage("Expected hasAdditionalNeed to be true, but was false")
      }
    }
    return this
  }

  fun doesNotHaveAdditionalNeed(): PersonAssert {
    isNotNull
    with(actual!!) {
      if (hasAdditionalNeed) {
        failWithMessage("Expected hasAdditionalNeed to be false, but was true")
      }
    }
    return this
  }
}

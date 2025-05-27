package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractAssert

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
}

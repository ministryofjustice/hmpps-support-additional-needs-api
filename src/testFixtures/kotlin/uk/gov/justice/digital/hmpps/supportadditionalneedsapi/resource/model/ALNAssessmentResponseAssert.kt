package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun assertThat(actual: ALNAssessmentResponse?) = ALNAssessmentResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [ALNAssessmentResponse].
 */
class ALNAssessmentResponseAssert(actual: ALNAssessmentResponse?) : AbstractObjectAssert<ALNAssessmentResponseAssert, ALNAssessmentResponse?>(actual, ALNAssessmentResponseAssert::class.java) {

  fun hasReference(expected: UUID): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun hasHasNeed(expected: Boolean): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (hasNeed != expected) {
        failWithMessage("Expected hasNeed to be $expected, but was $hasNeed")
      }
    }
    return this
  }

  fun hasScreeningDate(expected: LocalDate): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (screeningDate != expected) {
        failWithMessage("Expected screening date to be $expected, but was $screeningDate")
      }
    }
    return this
  }

  fun hasCuriousReference(expected: UUID): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (curiousReference != expected) {
        failWithMessage("Expected curious reference to be $expected, but was $curiousReference")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): ALNAssessmentResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }
}

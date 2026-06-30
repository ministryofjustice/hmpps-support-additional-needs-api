package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.OffsetDateTime
import java.util.UUID

fun assertThat(actual: SupportStrategyResponse?) = SupportStrategyResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [SupportStrategyResponse].
 */
class SupportStrategyResponseAssert(actual: SupportStrategyResponse?) :
  AbstractObjectAssert<SupportStrategyResponseAssert, SupportStrategyResponse?>(
    actual,
    SupportStrategyResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun isActive(): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (!active) {
        failWithMessage("Expected support strategy to be active but it was not")
      }
    }
    return this
  }

  fun isNotActive(): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (active) {
        failWithMessage("Expected support strategy not to be active but it was")
      }
    }
    return this
  }

  fun isArchived(): SupportStrategyResponseAssert = isNotActive()

  fun hasArchivedReason(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (archiveReason != expected) {
        failWithMessage("Expected support strategy to have archive reason $expected but it was $archiveReason")
      }
    }
    return this
  }

  fun hasCode(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!.supportStrategyType) {
      if (code != expected) {
        failWithMessage("Expected support strategy code to be $expected, but was $code")
      }
    }
    return this
  }

  fun hasCategory(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!.supportStrategyType) {
      if (categoryCode != expected) {
        failWithMessage("Expected support strategy category to be $expected, but was $categoryCode")
      }
    }
    return this
  }

  fun hasArea(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!.supportStrategyType) {
      if (areaCode != expected) {
        failWithMessage("Expected support strategy area to be $expected, but was $areaCode")
      }
    }
    return this
  }

  fun hasNoDetail(): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (!detail.isNullOrEmpty()) {
        failWithMessage("Expected support strategy to not have detail, but was $detail")
      }
    }
    return this
  }

  fun hasDetail(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (detail != expected) {
        failWithMessage("Expected support strategy detail to be $expected, but was $detail")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): SupportStrategyResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAtPrison != expected) {
        failWithMessage("Expected updatedAtPrison to be $expected, but was $updatedAtPrison")
      }
    }
    return this
  }
}

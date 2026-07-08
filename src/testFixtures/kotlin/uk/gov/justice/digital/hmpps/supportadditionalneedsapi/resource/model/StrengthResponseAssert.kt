package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: StrengthResponse?) = StrengthResponseAssert(actual)
fun assertThat(actual: StrengthListResponse?) = StrengthListResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [StrengthResponse].
 */
class StrengthResponseAssert(actual: StrengthResponse?) :
  AbstractObjectAssert<StrengthResponseAssert, StrengthResponse?>(
    actual,
    StrengthResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun isActive(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (!active) {
        failWithMessage("Expected strength to be active but it was not")
      }
    }
    return this
  }

  fun isNotActive(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (active) {
        failWithMessage("Expected strength not to be active but it was")
      }
    }
    return this
  }

  fun isArchived(): StrengthResponseAssert = isNotActive()

  fun hasArchivedReason(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (archiveReason != expected) {
        failWithMessage("Expected strength to have archive reason $expected but it was $archiveReason")
      }
    }
    return this
  }

  fun hasNoArchivedReason(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (!archiveReason.isNullOrEmpty()) {
        failWithMessage("Expected strength to not have an archive reason but it was $archiveReason")
      }
    }
    return this
  }

  fun isFromAlnScreener(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (!fromALNScreener) {
        failWithMessage("Expected strength to be from an ALN Screener but it was not")
      }
    }
    return this
  }

  fun isNotFromAlnScreener(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (!fromALNScreener) {
        failWithMessage("Expected strength to not be from an ALN Screener but it was")
      }
    }
    return this
  }

  fun hasAlnScreenerDate(expected: LocalDate): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (alnScreenerDate != expected) {
        failWithMessage("Expected ALN Screener date to be $expected, but was $alnScreenerDate")
      }
    }
    return this
  }

  fun hasCode(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!.strengthType) {
      if (code != expected) {
        failWithMessage("Expected strength code to be $expected, but was $code")
      }
    }
    return this
  }

  fun hasCategory(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!.strengthType) {
      if (categoryCode != expected) {
        failWithMessage("Expected strength category to be $expected, but was $categoryCode")
      }
    }
    return this
  }

  fun hasArea(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!.strengthType) {
      if (areaCode != expected) {
        failWithMessage("Expected strength area to be $expected, but was $areaCode")
      }
    }
    return this
  }

  fun hasNoSymptoms(): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (!symptoms.isNullOrEmpty()) {
        failWithMessage("Expected no symptoms but has $symptoms")
      }
    }
    return this
  }

  fun hasSymptoms(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (symptoms != expected) {
        failWithMessage("Expected symptoms to be $expected but was $symptoms")
      }
    }
    return this
  }

  fun wasIdentifiedBy(expected: Set<IdentificationSource>): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if ((howIdentified?.toSet() ?: emptySet()) != expected.toSet()) {
        failWithMessage("Expected identification sources to be $expected but was $howIdentified")
      }
    }
    return this
  }

  fun wasIdentifiedByOther(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (howIdentifiedOther != expected) {
        failWithMessage("Expected identified by other to be $expected but was $howIdentifiedOther")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): StrengthResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAtPrison != expected) {
        failWithMessage("Expected updatedAtPrison to be $expected, but was $updatedAtPrison")
      }
    }
    return this
  }
}

/**
 * AssertJ custom assertion for a [StrengthListResponse].
 */
class StrengthListResponseAssert(actual: StrengthListResponse?) :
  AbstractObjectAssert<StrengthListResponseAssert, StrengthListResponse?>(
    actual,
    StrengthListResponseAssert::class.java,
  ) {

  fun hasNoStrengths(): StrengthListResponseAssert {
    isNotNull
    with(actual!!) {
      if (!strengths.isEmpty()) {
        failWithMessage("Expected there to be no strengths, but there was: ${strengths.size}")
      }
    }
    return this
  }

  fun hasNumberOfStrengths(expected: Int): StrengthListResponseAssert {
    isNotNull
    with(actual!!) {
      if (strengths.size != expected) {
        failWithMessage("Expected there to be $expected strengths, but has ${strengths.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [StrengthResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [StrengthResponseAssert].
   * Returns this [StrengthListResponseAssert] to allow further chained assertions on the parent [StrengthListResponse]
   *
   * The `strengthNumber` parameter is not zero indexed to make for better readability in tests. IE. the first strength
   * should be referenced as `.strength(1) { .... }`
   */
  fun strength(strengthNumber: Int, consumer: Consumer<StrengthResponseAssert>): StrengthListResponseAssert {
    isNotNull
    with(actual!!) {
      val strength = strengths[strengthNumber - 1]
      consumer.accept(assertThat(strength))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [StrengthResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [StrengthResponseAssert].
   * Returns this [StrengthListResponseAssert] to allow further chained assertions on the parent [StrengthListResponse]
   * The assertions on all [StrengthResponse]s must pass as true.
   */
  fun allStrengths(consumer: Consumer<StrengthResponseAssert>): StrengthListResponseAssert {
    isNotNull
    with(actual!!) {
      strengths.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: ConditionResponse?) = ConditionResponseAssert(actual)
fun assertThat(actual: ConditionListResponse?) = ConditionListResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [ConditionResponse].
 */
class ConditionResponseAssert(actual: ConditionResponse?) :
  AbstractObjectAssert<ConditionResponseAssert, ConditionResponse?>(
    actual,
    ConditionResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun isActive(): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (!active) {
        failWithMessage("Expected condition to be active but it was not")
      }
    }
    return this
  }

  fun isNotActive(): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (active) {
        failWithMessage("Expected condition not to be active but it was")
      }
    }
    return this
  }

  fun isArchived(): ConditionResponseAssert = isNotActive()

  fun hasArchivedReason(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (archiveReason != expected) {
        failWithMessage("Expected condition to have archive reason $expected but it was $archiveReason")
      }
    }
    return this
  }

  fun hasNoArchivedReason(): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (archiveReason != null) {
        failWithMessage("Expected condition to have no archive reason but it was $archiveReason")
      }
    }
    return this
  }

  fun hasSource(expected: Source): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (source != expected) {
        failWithMessage("Expected source to be $expected but was $source")
      }
    }
    return this
  }

  fun hasConditionName(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (conditionName != expected) {
        failWithMessage("Expected condition name to be $expected but was $conditionName")
      }
    }
    return this
  }

  fun hasNoConditionName(): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (conditionName != null) {
        failWithMessage("Expected condition name to be null but was $conditionName")
      }
    }
    return this
  }

  fun hasConditionDetails(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (conditionDetails != expected) {
        failWithMessage("Expected condition details to be $expected but was $conditionDetails")
      }
    }
    return this
  }

  fun hasNoConditionDetails(): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (conditionDetails != null) {
        failWithMessage("Expected condition details to be null but was $conditionDetails")
      }
    }
    return this
  }

  fun hasCode(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!.conditionType) {
      if (code != expected) {
        failWithMessage("Expected condition code to be $expected, but was $code")
      }
    }
    return this
  }

  fun hasCategory(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!.conditionType) {
      if (categoryCode != expected) {
        failWithMessage("Expected condition category to be $expected, but was $categoryCode")
      }
    }
    return this
  }

  fun hasArea(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!.conditionType) {
      if (areaCode != expected) {
        failWithMessage("Expected condition area to be $expected, but was $areaCode")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): ConditionResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): ConditionResponseAssert {
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
 * AssertJ custom assertion for a [ConditionListResponse].
 */
class ConditionListResponseAssert(actual: ConditionListResponse?) :
  AbstractObjectAssert<ConditionListResponseAssert, ConditionListResponse?>(
    actual,
    ConditionListResponseAssert::class.java,
  ) {

  fun hasNoConditions(): ConditionListResponseAssert {
    isNotNull
    with(actual!!) {
      if (!conditions.isEmpty()) {
        failWithMessage("Expected there to be no conditions, but there was: ${conditions.size}")
      }
    }
    return this
  }

  fun hasNumberOfConditions(expected: Int): ConditionListResponseAssert {
    isNotNull
    with(actual!!) {
      if (conditions.size != expected) {
        failWithMessage("Expected there to be $expected conditions, but has ${conditions.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ConditionResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ConditionResponseAssert].
   * Returns this [ConditionListResponseAssert] to allow further chained assertions on the parent [ConditionListResponse]
   *
   * The `conditionNumber` parameter is not zero indexed to make for better readability in tests. IE. the first condition
   * should be referenced as `.condition(1) { .... }`
   */
  fun condition(conditionNumber: Int, consumer: Consumer<ConditionResponseAssert>): ConditionListResponseAssert {
    isNotNull
    with(actual!!) {
      val condition = conditions[conditionNumber - 1]
      consumer.accept(assertThat(condition))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ConditionResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ConditionResponseAssert].
   * Returns this [ConditionListResponseAssert] to allow further chained assertions on the parent [ConditionListResponse]
   * The assertions on all [ConditionResponse]s must pass as true.
   */
  fun allConditions(consumer: Consumer<ConditionResponseAssert>): ConditionListResponseAssert {
    isNotNull
    with(actual!!) {
      conditions.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: SupportStrategyResponse?) = SupportStrategyResponseAssert(actual)
fun assertThat(actual: SupportStrategyListResponse?) = SupportStrategyListResponseAssert(actual)

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

/**
 * AssertJ custom assertion for a [SupportStrategyListResponse].
 */
class SupportStrategyListResponseAssert(actual: SupportStrategyListResponse?) :
  AbstractObjectAssert<SupportStrategyListResponseAssert, SupportStrategyListResponse?>(
    actual,
    SupportStrategyListResponseAssert::class.java,
  ) {

  fun hasNoSupportStrategies(): SupportStrategyListResponseAssert {
    isNotNull
    with(actual!!) {
      if (!supportStrategies.isEmpty()) {
        failWithMessage("Expected there to be no support strategies, but there was: ${supportStrategies.size}")
      }
    }
    return this
  }

  fun hasNumberOfSupportStrategies(expected: Int): SupportStrategyListResponseAssert {
    isNotNull
    with(actual!!) {
      if (supportStrategies.size != expected) {
        failWithMessage("Expected there to be $expected support strategies, but has ${supportStrategies.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [SupportStrategyResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [SupportStrategyResponseAssert].
   * Returns this [SupportStrategyListResponseAssert] to allow further chained assertions on the parent [SupportStrategyListResponse]
   *
   * The `supportStrategyNumber` parameter is not zero indexed to make for better readability in tests. IE. the first support strategy
   * should be referenced as `.supportStrategy(1) { .... }`
   */
  fun supportStrategy(supportStrategyNumber: Int, consumer: Consumer<SupportStrategyResponseAssert>): SupportStrategyListResponseAssert {
    isNotNull
    with(actual!!) {
      val supportStrategy = supportStrategies[supportStrategyNumber - 1]
      consumer.accept(assertThat(supportStrategy))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [SupportStrategyResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [SupportStrategyResponseAssert].
   * Returns this [SupportStrategyListResponseAssert] to allow further chained assertions on the parent [SupportStrategyListResponse]
   * The assertions on all [SupportStrategyResponse]s must pass as true.
   */
  fun allSupportStrategies(consumer: Consumer<SupportStrategyResponseAssert>): SupportStrategyListResponseAssert {
    isNotNull
    with(actual!!) {
      supportStrategies.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }
}

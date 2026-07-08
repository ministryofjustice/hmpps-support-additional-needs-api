package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.function.Consumer

fun assertThat(actual: ALNScreenerResponse?) = ALNScreenerResponseAssert(actual)
fun assertThat(actual: ALNScreeners?) = ALNScreenersAssert(actual)

/**
 * AssertJ custom assertion for a single [ALNScreenerResponse]
 */
class ALNScreenerResponseAssert(actual: ALNScreenerResponse?) : AbstractObjectAssert<ALNScreenerResponseAssert, ALNScreenerResponse?>(actual, ALNScreenerResponseAssert::class.java) {

  fun hasReference(expected: UUID): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun hasScreenerDate(expected: LocalDate): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (screenerDate != expected) {
        failWithMessage("Expected screener date to be $expected, but was $screenerDate")
      }
    }
    return this
  }

  fun hasNoStrengths(): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (!strengths.isEmpty()) {
        failWithMessage("Expected no strengths but has ${strengths.size} strengths")
      }
    }
    return this
  }

  fun hasNumberOfStrengths(expected: Int): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (strengths.size != expected) {
        failWithMessage("Expected ALNScreenerResponse to be have $expected strengths, but has ${strengths.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [StrengthResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [StrengthResponseAssert].
   * Returns this [ALNScreenerResponseAssert] to allow further chained assertions on the parent [ALNScreenerResponse]
   *
   * The `strengthNumber` parameter is not zero indexed to make for better readability in tests. IE. the first strength
   * should be referenced as `.strength(1) { .... }`
   */
  fun strength(strengthNumber: Int, consumer: Consumer<StrengthResponseAssert>): ALNScreenerResponseAssert {
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
   * Returns this [ALNScreenerResponseAssert] to allow further chained assertions on the parent [ALNScreenerResponse]
   * The assertions on all [StrengthResponse]s must pass as true.
   */
  fun allStrengths(consumer: Consumer<StrengthResponseAssert>): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      strengths.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoChallenges(): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (!challenges.isEmpty()) {
        failWithMessage("Expected no challenges but has ${challenges.size} challenges")
      }
    }
    return this
  }

  fun hasNumberOfChallenges(expected: Int): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (challenges.size != expected) {
        failWithMessage("Expected ALNScreenerResponse to be have $expected challenges, but has ${challenges.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ChallengeResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ChallengeResponseAssert].
   * Returns this [ALNScreenerResponseAssert] to allow further chained assertions on the parent [ALNScreenerResponse]
   *
   * The `challengeNumber` parameter is not zero indexed to make for better readability in tests. IE. the first strength
   * should be referenced as `.challenge(1) { .... }`
   */
  fun challenge(challengeNumber: Int, consumer: Consumer<ChallengeResponseAssert>): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      val challenge = challenges[challengeNumber - 1]
      consumer.accept(assertThat(challenge))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ChallengeResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ChallengeResponseAssert].
   * Returns this [ALNScreenerResponseAssert] to allow further chained assertions on the parent [ALNScreenerResponse]
   * The assertions on all [ChallengeResponse]s must pass as true.
   */
  fun allChallenges(consumer: Consumer<ChallengeResponseAssert>): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      challenges.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): ALNScreenerResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): ALNScreenerResponseAssert {
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
 * AssertJ custom assertion for a [ALNScreeners].
 */
class ALNScreenersAssert(actual: ALNScreeners?) :
  AbstractObjectAssert<ALNScreenersAssert, ALNScreeners?>(
    actual,
    ALNScreenersAssert::class.java,
  ) {

  fun hasNoScreeners(): ALNScreenersAssert {
    isNotNull
    with(actual!!) {
      if (!screeners.isEmpty()) {
        failWithMessage("Expected there to be no screeners, but there was: ${screeners.size}")
      }
    }
    return this
  }

  fun hasNumberOfScreeners(expected: Int): ALNScreenersAssert {
    isNotNull
    with(actual!!) {
      if (screeners.size != expected) {
        failWithMessage("Expected there to be $expected screeners, but has ${screeners.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ALNScreenerResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNScreenerResponseAssert].
   * Returns this [ALNScreenersAssert] to allow further chained assertions on the parent [ALNScreeners]
   *
   * The `screenerNumber` parameter is not zero indexed to make for better readability in tests. IE. the first screener
   * should be referenced as `.screener(1) { .... }`
   */
  fun screener(screenerNumber: Int, consumer: Consumer<ALNScreenerResponseAssert>): ALNScreenersAssert {
    isNotNull
    with(actual!!) {
      val screener = screeners[screenerNumber - 1]
      consumer.accept(assertThat(screener))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ALNScreenerResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNScreenerResponseAssert].
   * Returns this [ALNScreenersAssert] to allow further chained assertions on the parent [ALNScreeners]
   * The assertions on all [ALNScreenerResponse]s must pass as true.
   */
  fun allScreeners(consumer: Consumer<ALNScreenerResponseAssert>): ALNScreenersAssert {
    isNotNull
    with(actual!!) {
      screeners.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }
}

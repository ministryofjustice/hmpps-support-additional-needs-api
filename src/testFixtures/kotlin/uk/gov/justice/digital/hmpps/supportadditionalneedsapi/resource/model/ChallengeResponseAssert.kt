package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

fun assertThat(actual: ChallengeResponse?) = ChallengeResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [ChallengeResponse].
 */
class ChallengeResponseAssert(actual: ChallengeResponse?) :
  AbstractObjectAssert<ChallengeResponseAssert, ChallengeResponse?>(
    actual,
    ChallengeResponseAssert::class.java,
  ) {

  fun hasReference(expected: UUID): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (reference != expected) {
        failWithMessage("Expected reference to be $expected, but was $reference")
      }
    }
    return this
  }

  fun isActive(): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (!active) {
        failWithMessage("Expected challenge to be active but it was not")
      }
    }
    return this
  }

  fun isNotActive(): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (active) {
        failWithMessage("Expected challenge not to be active but it was")
      }
    }
    return this
  }

  fun isArchived(): ChallengeResponseAssert = isNotActive()

  fun hasArchivedReason(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (archiveReason != expected) {
        failWithMessage("Expected challenge to have archive reason $expected but it was $archiveReason")
      }
    }
    return this
  }

  fun isFromAlnScreener(): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (!fromALNScreener) {
        failWithMessage("Expected challenge to be from an ALN Screener but it was not")
      }
    }
    return this
  }

  fun isNotFromAlnScreener(): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (!fromALNScreener) {
        failWithMessage("Expected challenge to not be from an ALN Screener but it was")
      }
    }
    return this
  }

  fun hasAlnScreenerDate(expected: LocalDate): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (alnScreenerDate != expected) {
        failWithMessage("Expected ALN Screener date to be $expected, but was $alnScreenerDate")
      }
    }
    return this
  }

  fun hasCode(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!.challengeType) {
      if (code != expected) {
        failWithMessage("Expected challenge code to be $expected, but was $code")
      }
    }
    return this
  }

  fun hasCategory(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!.challengeType) {
      if (categoryCode != expected) {
        failWithMessage("Expected challenge category to be $expected, but was $categoryCode")
      }
    }
    return this
  }

  fun hasArea(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!.challengeType) {
      if (areaCode != expected) {
        failWithMessage("Expected challenge area to be $expected, but was $areaCode")
      }
    }
    return this
  }

  fun hasNoSymptoms(): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (!symptoms.isNullOrEmpty()) {
        failWithMessage("Expected no symptoms but has $symptoms")
      }
    }
    return this
  }

  fun hasSymptoms(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (symptoms != expected) {
        failWithMessage("Expected symptoms to be $expected but was $symptoms")
      }
    }
    return this
  }

  fun wasIdentifiedBy(expected: Set<IdentificationSource>): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if ((howIdentified?.toSet() ?: emptySet()) != expected.toSet()) {
        failWithMessage("Expected identification sources to be $expected but was $howIdentified")
      }
    }
    return this
  }

  fun wasIdentifiedByOther(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (howIdentifiedOther != expected) {
        failWithMessage("Expected identified by other to be $expected but was $howIdentifiedOther")
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): ChallengeResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAtPrison != expected) {
        failWithMessage("Expected updatedAtPrison to be $expected, but was $updatedAtPrison")
      }
    }
    return this
  }
}

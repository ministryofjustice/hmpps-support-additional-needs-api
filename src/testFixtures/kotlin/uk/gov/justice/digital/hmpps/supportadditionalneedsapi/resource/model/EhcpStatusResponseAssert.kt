package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert

fun assertThat(actual: EhcpStatusResponse?) = EhcpStatusResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [EhcpStatusResponse].
 */
class EhcpStatusResponseAssert(actual: EhcpStatusResponse?) :
  AbstractObjectAssert<EhcpStatusResponseAssert, EhcpStatusResponse?>(
    actual,
    EhcpStatusResponseAssert::class.java,
  ) {

  fun hasCurrentEhcp(): EhcpStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (!hasCurrentEhcp) {
        failWithMessage("Expected hasCurrentEhcp to be true but was false")
      }
    }
    return this
  }

  fun doesNotHaveCurrentEhcp(): EhcpStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (hasCurrentEhcp) {
        failWithMessage("Expected hasCurrentEhcp to be false but was true")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): EhcpStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): EhcpStatusResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }
}

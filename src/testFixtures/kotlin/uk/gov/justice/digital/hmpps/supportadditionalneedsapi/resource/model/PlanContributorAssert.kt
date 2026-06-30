package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert

fun assertThat(actual: PlanContributor?) = PlanContributorAssert(actual)

/**
 * AssertJ custom assertion for a single [PlanContributor].
 */
class PlanContributorAssert(actual: PlanContributor?) :
  AbstractObjectAssert<PlanContributorAssert, PlanContributor?>(
    actual,
    PlanContributorAssert::class.java,
  ) {

  fun hasName(expected: String): PlanContributorAssert {
    isNotNull
    with(actual!!) {
      if (name != expected) {
        failWithMessage("Expected plan contributor name $expected, but was $name")
      }
    }
    return this
  }

  fun hasJobRole(expected: String): PlanContributorAssert {
    isNotNull
    with(actual!!) {
      if (jobRole != expected) {
        failWithMessage("Expected plan contributor job role $expected, but was $jobRole")
      }
    }
    return this
  }
}

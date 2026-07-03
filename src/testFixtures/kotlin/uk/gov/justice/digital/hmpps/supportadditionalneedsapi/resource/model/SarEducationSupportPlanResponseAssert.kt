package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.time.OffsetDateTime
import java.util.function.Consumer

fun assertThat(actual: SarEducationSupportPlanResponse?) = SarEducationSupportPlanResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [SarEducationSupportPlanResponse].
 */
class SarEducationSupportPlanResponseAssert(actual: SarEducationSupportPlanResponse?) :
  AbstractObjectAssert<SarEducationSupportPlanResponseAssert, SarEducationSupportPlanResponse?>(
    actual,
    SarEducationSupportPlanResponseAssert::class.java,
  ) {

  fun hasIndividualSupport(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (individualSupport != expected) {
        failWithMessage("Expected to have individual support $expected but has $individualSupport")
      }
    }
    return this
  }

  fun hasSpecificTeachingSkills(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (specificTeachingSkills != expected) {
        failWithMessage("Expected to have specific teaching skills $expected but has $specificTeachingSkills")
      }
    }
    return this
  }

  fun hasTeachingAdjustments(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (teachingAdjustments != expected) {
        failWithMessage("Expected to have teaching adjustments $expected but has $teachingAdjustments")
      }
    }
    return this
  }

  fun hasExamAccessArrangements(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (examAccessArrangements != expected) {
        failWithMessage("Expected to have exam access arrangements $expected but has $examAccessArrangements")
      }
    }
    return this
  }

  fun hasLearningNeedsSupportPractitionerSupport(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (lnspSupport != expected) {
        failWithMessage("Expected to have Learning Needs Support Practitioner support $expected but has $lnspSupport")
      }
    }
    return this
  }

  fun hasLearningNeedsSupportPractitionerHours(expected: Int): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (lnspSupportHours != expected) {
        failWithMessage("Expected to have Learning Needs Support Practitioner hours $expected but has $lnspSupportHours")
      }
    }
    return this
  }

  fun hasOtherDetails(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (otherDetails != expected) {
        failWithMessage("Expected to have other details $expected but has $otherDetails")
      }
    }
    return this
  }

  fun planWasNotCreatedByPlanContributor(): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (planCreatedBy != null) {
        failWithMessage("Expected not to have been created by other contributor, but was created by $planCreatedBy")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the child [PlanContributor]. Takes a lambda as the method argument
   * to call assertion methods provided by [PlanContributorAssert].
   * Returns this [SarEducationSupportPlanResponseAssert] to allow further chained assertions on the parent [SarEducationSupportPlanResponse]
   */
  fun planWasCreatedByPlanContributor(consumer: Consumer<PlanContributorAssert>): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      consumer.accept(assertThat(planCreatedBy))
    }
    return this
  }

  fun planWasNotCreatedWithOtherContributors(): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (!otherContributors.isNullOrEmpty()) {
        failWithMessage("Expected plan not to have been created with other contributors but is was: $otherContributors")
      }
    }
    return this
  }

  fun planWasCreatedWithNumberOfOtherContributors(expected: Int): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (otherContributors?.size != expected) {
        failWithMessage("Expected plan to be have been created with $expected other contributors, but has ${otherContributors?.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [PlanContributor]. Takes a lambda as the method argument
   * to call assertion methods provided by [PlanContributorAssert].
   * Returns this [SarEducationSupportPlanResponseAssert] to allow further chained assertions on the parent [SarEducationSupportPlanResponse]
   *
   * The `contributorNumber` parameter is not zero indexed to make for better readability in tests. IE. the first contributor
   * should be referenced as `.planCreationContributor(1) { .... }`
   */
  fun planCreationContributor(contributorNumber: Int, consumer: Consumer<PlanContributorAssert>): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      val planContributor = otherContributors?.get(contributorNumber - 1)
      consumer.accept(assertThat(planContributor))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [PlanContributor]s. Takes a lambda as the method argument
   * to call assertion methods provided by [PlanContributorAssert].
   * Returns this [SarEducationSupportPlanResponseAssert] to allow further chained assertions on the parent [SarEducationSupportPlanResponse]
   * The assertions on all [PlanContributor]s must pass as true.
   */
  fun allPlanCreationContributors(consumer: Consumer<PlanContributorAssert>): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      otherContributors?.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun wasCreatedAt(expected: OffsetDateTime): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAt != expected) {
        failWithMessage("Expected createdAt to be $expected, but was $createdAt")
      }
    }
    return this
  }

  fun wasUpdatedAt(expected: OffsetDateTime): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAt != expected) {
        failWithMessage("Expected updatedAt to be $expected, but was $updatedAt")
      }
    }
    return this
  }

  fun wasCreatedBy(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdBy != expected) {
        failWithMessage("Expected createdBy to be $expected, but was $createdBy")
      }
    }
    return this
  }

  fun wasCreatedByDisplayName(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdByDisplayName != expected) {
        failWithMessage("Expected createdByDisplayName to be $expected, but was $createdByDisplayName")
      }
    }
    return this
  }

  fun wasUpdatedBy(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedBy != expected) {
        failWithMessage("Expected updatedBy to be $expected, but was $updatedBy")
      }
    }
    return this
  }

  fun wasUpdatedByDisplayName(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedByDisplayName != expected) {
        failWithMessage("Expected updatedByDisplayName to be $expected, but was $updatedByDisplayName")
      }
    }
    return this
  }

  fun wasCreatedAtPrison(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (createdAtPrison != expected) {
        failWithMessage("Expected createdAtPrison to be $expected, but was $createdAtPrison")
      }
    }
    return this
  }

  fun wasUpdatedAtPrison(expected: String): SarEducationSupportPlanResponseAssert {
    isNotNull
    with(actual!!) {
      if (updatedAtPrison != expected) {
        failWithMessage("Expected updatedAtPrison to be $expected, but was $updatedAtPrison")
      }
    }
    return this
  }
}

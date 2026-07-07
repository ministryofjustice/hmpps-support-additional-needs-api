package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert

fun assertThat(actual: SarEducationSupportPlanReviewResponse?) = SarEducationSupportPlanReviewResponseAssert(actual)

/**
 * AssertJ custom assertion for a single [SarEducationSupportPlanReviewResponse].
 */
class SarEducationSupportPlanReviewResponseAssert(actual: SarEducationSupportPlanReviewResponse?) :
  AbstractObjectAssert<SarEducationSupportPlanReviewResponseAssert, SarEducationSupportPlanReviewResponse?>(
    actual,
    SarEducationSupportPlanReviewResponseAssert::class.java,
  ) {

  fun hasIndividualSupport(expected: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (individualSupport != expected) {
        failWithMessage("Expected individualSupport to be $expected, but was $individualSupport")
      }
    }
    return this
  }

  fun hasTeachingAdjustments(expected: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (teachingAdjustments != expected) {
        failWithMessage("Expected teachingAdjustments to be $expected, but was $teachingAdjustments")
      }
    }
    return this
  }

  fun hasOtherDetails(expected: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (otherDetails != expected) {
        failWithMessage("Expected otherDetails to be $expected, but was $otherDetails")
      }
    }
    return this
  }

  fun hasReviewerFeedback(expected: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (reviewerFeedback != expected) {
        failWithMessage("Expected reviewerFeedback to be $expected, but was $reviewerFeedback")
      }
    }
    return this
  }

  fun hasPrisonerFeedback(expected: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (prisonerFeedback != expected) {
        failWithMessage("Expected prisonerFeedback to be $expected, but was $prisonerFeedback")
      }
    }
    return this
  }

  fun prisonerDeclinedReview(): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (!prisonerDeclinedFeedback) {
        failWithMessage("Expected prisonerDeclinedFeedback to be true but was false")
      }
    }
    return this
  }

  fun prisonerDidNotDeclineReview(): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (prisonerDeclinedFeedback) {
        failWithMessage("Expected prisonerDeclinedFeedback to be false but was true")
      }
    }
    return this
  }

  fun hasReviewCreatedBy(name: String, jobRole: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (reviewCreatedBy?.name != name || reviewCreatedBy?.jobRole != jobRole) {
        failWithMessage("Expected reviewCreatedBy to be ($name, $jobRole), but was $reviewCreatedBy")
      }
    }
    return this
  }

  fun hasNoOtherContributors(): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (!otherContributors.isNullOrEmpty()) {
        failWithMessage("Expected no other contributors but has ${otherContributors?.size}")
      }
    }
    return this
  }

  fun hasNumberOfOtherContributors(expected: Int): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      if (otherContributors?.size != expected) {
        failWithMessage("Expected $expected other contributors, but has ${otherContributors?.size}")
      }
    }
    return this
  }

  /**
   * Asserts on a single other contributor. The `contributorNumber` is not zero indexed for readability, IE. the first
   * contributor is referenced as `.hasOtherContributor(1, ...)`.
   */
  fun hasOtherContributor(contributorNumber: Int, name: String, jobRole: String): SarEducationSupportPlanReviewResponseAssert {
    isNotNull
    with(actual!!) {
      val contributor = otherContributors!![contributorNumber - 1]
      if (contributor.name != name || contributor.jobRole != jobRole) {
        failWithMessage("Expected other contributor $contributorNumber to be ($name, $jobRole), but was $contributor")
      }
    }
    return this
  }
}

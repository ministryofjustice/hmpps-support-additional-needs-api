package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import org.assertj.core.api.AbstractObjectAssert
import java.util.function.Consumer

fun assertThat(actual: SubjectAccessRequestContent?) = SubjectAccessRequestContentAssert(actual)

/**
 * AssertJ custom assertion for a single [SubjectAccessRequestContent]
 */
class SubjectAccessRequestContentAssert(actual: SubjectAccessRequestContent?) : AbstractObjectAssert<SubjectAccessRequestContentAssert, SubjectAccessRequestContent?>(actual, SubjectAccessRequestContentAssert::class.java) {

  fun hasNoOriginalEducationSupportPlan(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (originalEducationSupportPlan != null) {
        failWithMessage("Expected original education support plan to be null but was: $originalEducationSupportPlan")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the child [SarEducationSupportPlanResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [SarEducationSupportPlanResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   */
  fun originalEducationSupportPlan(consumer: Consumer<SarEducationSupportPlanResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      consumer.accept(assertThat(originalEducationSupportPlan))
    }
    return this
  }

  fun hasNoSupportStrategies(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (supportStrategies.isNotEmpty()) {
        failWithMessage("Expected no support strategies but has ${supportStrategies.size} support strategies")
      }
    }
    return this
  }

  fun hasNumberOfSupportStrategies(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (supportStrategies.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected support strategies, but has ${supportStrategies.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [SupportStrategyResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [SupportStrategyResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `supportStrategyNumber` parameter is not zero indexed to make for better readability in tests. IE. the first support strategy
   * should be referenced as `.supportStrategy(1) { .... }`
   */
  fun supportStrategy(supportStrategyNumber: Int, consumer: Consumer<SupportStrategyResponseAssert>): SubjectAccessRequestContentAssert {
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
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [SupportStrategyResponse]s must pass as true.
   */
  fun allSupportStrategies(consumer: Consumer<SupportStrategyResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      supportStrategies.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoNonAlnStrengths(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (!nonAlnStrengths.isEmpty()) {
        failWithMessage("Expected no strengths but has ${nonAlnStrengths.size} strengths")
      }
    }
    return this
  }

  fun hasNumberOfNonAlnStrengths(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (nonAlnStrengths.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected strengths, but has ${nonAlnStrengths.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [StrengthResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [StrengthResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `strengthNumber` parameter is not zero indexed to make for better readability in tests. IE. the first strength
   * should be referenced as `.nonAlnStrength(1) { .... }`
   */
  fun nonAlnStrength(strengthNumber: Int, consumer: Consumer<StrengthResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val nonAlnStrength = nonAlnStrengths[strengthNumber - 1]
      consumer.accept(assertThat(nonAlnStrength))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [StrengthResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [StrengthResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [StrengthResponse]s must pass as true.
   */
  fun allNonAlnStrengths(consumer: Consumer<StrengthResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      nonAlnStrengths.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoNonAlnChallenges(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (!nonAlnChallenges.isEmpty()) {
        failWithMessage("Expected no challenges but has ${nonAlnChallenges.size} challenges")
      }
    }
    return this
  }

  fun hasNumberOfNonAlnChallenges(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (nonAlnChallenges.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected challenges, but has ${nonAlnChallenges.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ChallengeResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ChallengeResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `challengeNumber` parameter is not zero indexed to make for better readability in tests. IE. the first challenge
   * should be referenced as `.nonAlnChallenge(1) { .... }`
   */
  fun nonAlnChallenge(challengeNumber: Int, consumer: Consumer<ChallengeResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val nonAlnChallenge = nonAlnChallenges[challengeNumber - 1]
      consumer.accept(assertThat(nonAlnChallenge))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ChallengeResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ChallengeResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [ChallengeResponse]s must pass as true.
   */
  fun allNonAlnChallenges(consumer: Consumer<ChallengeResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      nonAlnChallenges.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoAlnScreeners(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (!alnScreeners.isEmpty()) {
        failWithMessage("Expected no ALN Screeners but has ${alnScreeners.size} screeners")
      }
    }
    return this
  }

  fun hasNumberOfAlnScreeners(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (alnScreeners.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected ALN Screeners, but has ${alnScreeners.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ConditionResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ConditionResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `conditionNumber` parameter is not zero indexed to make for better readability in tests. IE. the first condition
   * should be referenced as `.condition(1) { .... }`
   */
  fun condition(conditionNumber: Int, consumer: Consumer<ConditionResponseAssert>): SubjectAccessRequestContentAssert {
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
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [ConditionResponse]s must pass as true.
   */
  fun allConditions(consumer: Consumer<ConditionResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      conditions.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoConditions(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (!conditions.isEmpty()) {
        failWithMessage("Expected no ALN Screeners but has ${conditions.size} conditions")
      }
    }
    return this
  }

  fun hasNumberOfConditions(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (conditions.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected conditions, but has ${conditions.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ALNScreenerResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNScreenerResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `screenerNumber` parameter is not zero indexed to make for better readability in tests. IE. the first screener
   * should be referenced as `.alnScreener(1) { .... }`
   */
  fun alnScreener(screenerNumber: Int, consumer: Consumer<ALNScreenerResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val screener = alnScreeners[screenerNumber - 1]
      consumer.accept(assertThat(screener))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ALNScreenerResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNScreenerResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [ALNScreenerResponse]s must pass as true.
   */
  fun allAlnScreeners(consumer: Consumer<ALNScreenerResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      alnScreeners.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoPlanCreationSchedules(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (planCreationSchedules.isNotEmpty()) {
        failWithMessage("Expected no plan creation schedules but has ${planCreationSchedules.size} plan creation schedules")
      }
    }
    return this
  }

  fun hasNumberOfPlanCreationSchedules(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (planCreationSchedules.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected plan creation schedules, but has ${planCreationSchedules.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [PlanCreationScheduleResponse]. Takes a lambda as the method
   * argument to call assertion methods provided by [PlanCreationScheduleResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `planCreationScheduleNumber` parameter is not zero indexed to make for better readability in tests. IE. the first
   * plan creation schedule should be referenced as `.planCreationSchedule(1) { .... }`
   */
  fun planCreationSchedule(planCreationScheduleNumber: Int, consumer: Consumer<PlanCreationScheduleResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val planCreationSchedule = planCreationSchedules[planCreationScheduleNumber - 1]
      consumer.accept(assertThat(planCreationSchedule))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [PlanCreationScheduleResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [PlanCreationScheduleResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [PlanCreationScheduleResponse]s must pass as true.
   */
  fun allPlanCreationSchedules(consumer: Consumer<PlanCreationScheduleResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      planCreationSchedules.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoReviewSchedules(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (reviewSchedules.isNotEmpty()) {
        failWithMessage("Expected no plan creation schedules but has ${reviewSchedules.size} review schedules")
      }
    }
    return this
  }

  fun hasNumberOfReviewSchedules(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (reviewSchedules.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected review schedules, but has ${reviewSchedules.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ReviewScheduleResponse]. Takes a lambda as the method
   * argument to call assertion methods provided by [ReviewScheduleResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `reviewScheduleNumber` parameter is not zero indexed to make for better readability in tests. IE. the first
   * plan creation schedule should be referenced as `.reviewSchedule(1) { .... }`
   */
  fun reviewSchedule(reviewScheduleNumber: Int, consumer: Consumer<ReviewScheduleResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val reviewSchedule = reviewSchedules[reviewScheduleNumber - 1]
      consumer.accept(assertThat(reviewSchedule))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ReviewScheduleResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ReviewScheduleResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [ReviewScheduleResponse]s must pass as true.
   */
  fun allReviewSchedules(consumer: Consumer<ReviewScheduleResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      reviewSchedules.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoAlnAssessments(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (!alnAssessments.isEmpty()) {
        failWithMessage("Expected no ALN Screener needs but has ${alnAssessments.size} screener needs")
      }
    }
    return this
  }

  fun hasNumberOfAlnAssessments(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (alnAssessments.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected ALN Screener needs, but has ${alnAssessments.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [ALNAssessmentResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNAssessmentResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `screenerNumber` parameter is not zero indexed to make for better readability in tests. IE. the first screener
   * should be referenced as `.alnScreener(1) { .... }`
   */
  fun alnAssessment(screenerNumber: Int, consumer: Consumer<ALNAssessmentResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val alnAssessment = alnAssessments[screenerNumber - 1]
      consumer.accept(assertThat(alnAssessment))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [ALNAssessmentResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [ALNAssessmentResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [ALNAssessmentResponse]s must pass as true.
   */
  fun allAlnAssessments(consumer: Consumer<ALNAssessmentResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      alnAssessments.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoEhcpStatuses(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (ehcpStatuses.isNotEmpty()) {
        failWithMessage("Expected no EHCP statuses but has ${ehcpStatuses.size} EHCP statuses")
      }
    }
    return this
  }

  fun hasNumberOfEhcpStatuses(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (ehcpStatuses.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected EHCP statuses, but has ${ehcpStatuses.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [EhcpStatusResponse]. Takes a lambda as the method argument
   * to call assertion methods provided by [EhcpStatusResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `ehcpStatusNumber` parameter is not zero indexed to make for better readability in tests. IE. the first EHCP
   * status should be referenced as `.ehcpStatus(1) { .... }`
   */
  fun ehcpStatus(ehcpStatusNumber: Int, consumer: Consumer<EhcpStatusResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val ehcpStatus = ehcpStatuses[ehcpStatusNumber - 1]
      consumer.accept(assertThat(ehcpStatus))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [EhcpStatusResponse]s. Takes a lambda as the method argument
   * to call assertion methods provided by [EhcpStatusResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [EhcpStatusResponse]s must pass as true.
   */
  fun allEhcpStatuses(consumer: Consumer<EhcpStatusResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      ehcpStatuses.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }

  fun hasNoReviews(): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (reviews.isNotEmpty()) {
        failWithMessage("Expected no reviews but has ${reviews.size} reviews")
      }
    }
    return this
  }

  fun hasNumberOfReviews(expected: Int): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      if (reviews.size != expected) {
        failWithMessage("Expected SubjectAccessRequestContent to be have $expected reviews, but has ${reviews.size}")
      }
    }
    return this
  }

  /**
   * Allows for assertion chaining into the specified child [SarEducationSupportPlanReviewResponse]. Takes a lambda as
   * the method argument to call assertion methods provided by [SarEducationSupportPlanReviewResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   *
   * The `reviewNumber` parameter is not zero indexed to make for better readability in tests. IE. the first review
   * should be referenced as `.review(1) { .... }`
   */
  fun review(reviewNumber: Int, consumer: Consumer<SarEducationSupportPlanReviewResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      val review = reviews[reviewNumber - 1]
      consumer.accept(assertThat(review))
    }
    return this
  }

  /**
   * Allows for assertion chaining into all child [SarEducationSupportPlanReviewResponse]s. Takes a lambda as the method
   * argument to call assertion methods provided by [SarEducationSupportPlanReviewResponseAssert].
   * Returns this [SubjectAccessRequestContentAssert] to allow further chained assertions on the parent [SubjectAccessRequestContent]
   * The assertions on all [SarEducationSupportPlanReviewResponse]s must pass as true.
   */
  fun allReviews(consumer: Consumer<SarEducationSupportPlanReviewResponseAssert>): SubjectAccessRequestContentAssert {
    isNotNull
    with(actual!!) {
      reviews.onEach {
        consumer.accept(assertThat(it))
      }
    }
    return this
  }
}

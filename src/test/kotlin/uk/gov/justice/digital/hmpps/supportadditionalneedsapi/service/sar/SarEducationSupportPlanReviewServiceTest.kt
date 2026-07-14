package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanHistoryEntityKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SarEducationSupportPlanReviewResponseMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.Instant
import java.util.UUID

/**
 * Tests how each review is paired with the version of the plan as it was after that review.
 *
 * The pairing is asserted by verifying which (review, plan version) pairs are handed to the mapper.
 *
 * Timeline note: completing a review creates the review record and THEN updates the plan, so a plan version produced by
 * a review is always timestamped shortly after that review's createdAt.
 */
@ExtendWith(MockitoExtension::class)
class SarEducationSupportPlanReviewServiceTest {

  @InjectMocks
  private lateinit var service: SarEducationSupportPlanReviewService

  @Mock
  private lateinit var elspReviewRepository: ElspReviewRepository

  @Mock
  private lateinit var elspPlanHistoryRepository: ElspPlanHistoryRepository

  @Mock
  private lateinit var sarEducationSupportPlanReviewResponseMapper: SarEducationSupportPlanReviewResponseMapper

  private val prisonNumber = randomValidPrisonNumber()

  @Test
  fun `should pair each review with the plan version that review produced given every review changed the plan`() {
    // Given
    val originalPlan = aPlanVersion(updatedAt = "2026-01-01T09:00:00Z")
    val planAfterFirstReview = aPlanVersion(updatedAt = "2026-02-01T09:00:01Z")
    val planAfterSecondReview = aPlanVersion(updatedAt = "2026-03-01T09:00:01Z")
    val firstReview = aReview(createdAt = "2026-02-01T09:00:00Z")
    val secondReview = aReview(createdAt = "2026-03-01T09:00:00Z")
    givenReviews(firstReview, secondReview)
    givenPlanVersions(originalPlan, planAfterFirstReview, planAfterSecondReview)

    // When
    service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(firstReview, planAfterFirstReview)
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(secondReview, planAfterSecondReview)
  }

  @Test
  fun `should show the plan unchanged given a review that changed none of the plan answers`() {
    // Given
    // The second review wrote no plan version, so there are 3 reviews but only 3 plan versions (not 4).
    val originalPlan = aPlanVersion(updatedAt = "2026-01-01T09:00:00Z")
    val planAfterFirstReview = aPlanVersion(updatedAt = "2026-02-01T09:00:01Z")
    val planAfterThirdReview = aPlanVersion(updatedAt = "2026-04-01T09:00:01Z")
    val firstReview = aReview(createdAt = "2026-02-01T09:00:00Z")
    val secondReviewThatChangedNothing = aReview(createdAt = "2026-03-01T09:00:00Z")
    val thirdReview = aReview(createdAt = "2026-04-01T09:00:00Z")
    givenReviews(firstReview, secondReviewThatChangedNothing, thirdReview)
    givenPlanVersions(originalPlan, planAfterFirstReview, planAfterThirdReview)

    // When
    service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(firstReview, planAfterFirstReview)
    // The review that changed nothing shows the plan as it stood - ie. as the first review left it.
    then(sarEducationSupportPlanReviewResponseMapper).should()
      .toModel(secondReviewThatChangedNothing, planAfterFirstReview)
    // The most recent review must still be reported, against the version it produced.
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(thirdReview, planAfterThirdReview)
  }

  @Test
  fun `should show the original plan given the first review changed none of the plan answers`() {
    // Given
    val originalPlan = aPlanVersion(updatedAt = "2026-01-01T09:00:00Z")
    val reviewThatChangedNothing = aReview(createdAt = "2026-02-01T09:00:00Z")
    givenReviews(reviewThatChangedNothing)
    givenPlanVersions(originalPlan)

    // When
    service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(reviewThatChangedNothing, originalPlan)
  }

  @Test
  fun `should show the plan unchanged given the most recent review changed none of the plan answers`() {
    // Given
    val originalPlan = aPlanVersion(updatedAt = "2026-01-01T09:00:00Z")
    val planAfterFirstReview = aPlanVersion(updatedAt = "2026-02-01T09:00:01Z")
    val firstReview = aReview(createdAt = "2026-02-01T09:00:00Z")
    val mostRecentReviewThatChangedNothing = aReview(createdAt = "2026-03-01T09:00:00Z")
    givenReviews(firstReview, mostRecentReviewThatChangedNothing)
    givenPlanVersions(originalPlan, planAfterFirstReview)

    // When
    service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(firstReview, planAfterFirstReview)
    then(sarEducationSupportPlanReviewResponseMapper).should()
      .toModel(mostRecentReviewThatChangedNothing, planAfterFirstReview)
  }

  @Test
  fun `should show the plan unchanged given consecutive reviews that changed none of the plan answers`() {
    // Given
    val originalPlan = aPlanVersion(updatedAt = "2026-01-01T09:00:00Z")
    val firstReviewThatChangedNothing = aReview(createdAt = "2026-02-01T09:00:00Z")
    val secondReviewThatChangedNothing = aReview(createdAt = "2026-03-01T09:00:00Z")
    givenReviews(firstReviewThatChangedNothing, secondReviewThatChangedNothing)
    givenPlanVersions(originalPlan)

    // When
    service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(firstReviewThatChangedNothing, originalPlan)
    then(sarEducationSupportPlanReviewResponseMapper).should().toModel(secondReviewThatChangedNothing, originalPlan)
  }

  @Test
  fun `should return no reviews given the person has no reviews`() {
    // Given
    givenReviews()
    givenPlanVersions(aPlanVersion(updatedAt = "2026-01-01T09:00:00Z"))

    // When
    val actual = service.getReviewsWithTheirAssociatedPlanVersion(prisonNumber)

    // Then
    assertThat(actual).isEmpty()
    then(sarEducationSupportPlanReviewResponseMapper).shouldHaveNoInteractions()
  }

  private fun givenReviews(vararg reviews: ElspReviewEntity) {
    // Returned in an arbitrary order to prove the service does its own ordering.
    given(elspReviewRepository.findAllByPrisonNumber(prisonNumber)).willReturn(reviews.toList().reversed())
  }

  private fun givenPlanVersions(vararg planVersions: ElspPlanHistoryEntity) {
    given(elspPlanHistoryRepository.findAllByPrisonNumber(prisonNumber)).willReturn(planVersions.toList().reversed())
  }

  private fun aReview(createdAt: String): ElspReviewEntity = ElspReviewEntity(
    prisonNumber = prisonNumber,
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    reviewScheduleReference = UUID.randomUUID(),
    createdAt = Instant.parse(createdAt),
  )

  private fun aPlanVersion(updatedAt: String): ElspPlanHistoryEntity = ElspPlanHistoryEntity(
    prisonNumber = prisonNumber,
    individualSupport = "support",
    createdBy = "testuser",
    createdAt = Instant.parse("2026-01-01T09:00:00Z"),
    updatedBy = "testuser",
    updatedAt = Instant.parse(updatedAt),
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    id = ElspPlanHistoryEntityKey(revisionNumber = 1, id = UUID.randomUUID()),
  )
}

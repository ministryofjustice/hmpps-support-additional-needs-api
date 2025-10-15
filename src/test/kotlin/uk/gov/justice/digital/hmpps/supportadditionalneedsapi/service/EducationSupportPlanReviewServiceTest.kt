package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.CannotCompleteReviewWithNoSchedule
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspReviewMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewContributor
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportPlanReviewRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEducationSupportPlanRequest
import java.time.LocalDate
import java.util.*

@ExtendWith(MockitoExtension::class)
class EducationSupportPlanReviewServiceTest {

  @InjectMocks
  private lateinit var service: EducationSupportPlanReviewService

  @Mock
  private lateinit var educationSupportPlanService: EducationSupportPlanService

  @Mock
  private lateinit var reviewScheduleRepository: ReviewScheduleRepository

  @Mock
  private lateinit var reviewScheduleService: ReviewScheduleService

  @Mock
  private lateinit var elspReviewRepository: ElspReviewRepository

  @Mock
  private lateinit var elspReviewMapper: ElspReviewMapper

  @Test
  fun `should throw PlanNotFoundException if prisoner has no plan`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val request = aValidSupportPlanReviewRequest()
    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(false)

    // When / Then
    assertThatThrownBy { service.processReview(prisonNumber, request) }
      .isInstanceOf(PlanNotFoundException::class.java)
  }

  @Test
  fun `should throw CannotCompleteReviewWithNoSchedule if no schedule exists`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val request = aValidSupportPlanReviewRequest()
    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(true)
    given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

    // When / Then
    assertThatThrownBy { service.processReview(prisonNumber, request) }
      .isInstanceOf(CannotCompleteReviewWithNoSchedule::class.java)
  }

  @Test
  fun `should throw CannotCompleteReviewWithNoSchedule if schedule status is not SCHEDULED`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val request = aValidSupportPlanReviewRequest()
    val reviewSchedule = ReviewScheduleEntity(reference = UUID.randomUUID(), prisonNumber = prisonNumber, status = ReviewScheduleStatus.COMPLETED, createdAtPrison = "BXI", updatedAtPrison = "BXI", deadlineDate = LocalDate.now())

    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(true)
    given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(reviewSchedule)

    // When / Then
    assertThatThrownBy { service.processReview(prisonNumber, request) }
      .isInstanceOf(CannotCompleteReviewWithNoSchedule::class.java)
  }

  @Test
  fun `should create review, update plan and complete schedule when valid request`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    val request = aValidSupportPlanReviewRequest(nextReviewDate = LocalDate.now().plusMonths(6))
    val reviewSchedule = ReviewScheduleEntity(reference = UUID.randomUUID(), prisonNumber = prisonNumber, status = ReviewScheduleStatus.SCHEDULED, createdAtPrison = "BXI", updatedAtPrison = "BXI", deadlineDate = LocalDate.now())

    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(true)
    given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(reviewSchedule)

    // When
    service.processReview(prisonNumber, request)

    // Then
    then(elspReviewRepository).should().save(any())
    then(educationSupportPlanService).should().updatePlan(prisonNumber, request.updateEducationSupportPlan)
    then(reviewScheduleService).should().completeExistingAndCreateNextReviewSchedule(
      eq(prisonNumber),
      eq(request.nextReviewDate),
      eq(request.prisonId),
      eq(reviewSchedule),
    )
  }

  @Test
  fun `getReviews should throw PlanNotFoundException if prisoner has no plan`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(false)

    // When / Then
    assertThatThrownBy { service.getReviews(prisonNumber) }
      .isInstanceOf(PlanNotFoundException::class.java)
  }

  @Test
  fun `getReviews should return an empty list if there is no reviews`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(true)
    given(elspReviewRepository.findAllByPrisonNumber(prisonNumber)).willReturn(listOf())

    // When / Then
    val reviewsResponse = service.getReviews(prisonNumber)
    // Then
    assertThat(reviewsResponse.reviews).isEmpty()
  }

  @Test
  fun `getReviews should return an reviews if there are reviews`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(educationSupportPlanService.hasPlan(prisonNumber)).willReturn(true)

    given(elspReviewRepository.findAllByPrisonNumber(prisonNumber)).willReturn(listOf(aValidReviewEntity(prisonNumber), aValidReviewEntity(prisonNumber)))

    // When / Then
    val reviewsResponse = service.getReviews(prisonNumber)
    // Then
    assertThat(reviewsResponse.reviews).hasSize(2)
  }

  private fun aValidReviewEntity(prisonNumber: String): ElspReviewEntity = ElspReviewEntity(
    reference = UUID.randomUUID(),
    prisonNumber = prisonNumber,
    prisonerDeclinedFeedback = false,
    prisonerFeedback = "prisonerFeedback",
    reviewerFeedback = "reviewerFeedback",
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
    reviewScheduleReference = UUID.randomUUID(),
  )

  private fun aValidSupportPlanReviewRequest(nextReviewDate: LocalDate = LocalDate.now().plusMonths(1)): SupportPlanReviewRequest = SupportPlanReviewRequest(
    nextReviewDate = nextReviewDate,
    prisonId = "BXI",
    prisonerDeclinedFeedback = false,
    reviewerFeedback = "reviewerFeedback",
    updateEducationSupportPlan = UpdateEducationSupportPlanRequest(anyChanges = false),
    reviewCreatedBy = ReviewContributor(name = "John Smith", jobRole = "teacher"),
    otherContributors = listOf(),
    prisonerFeedback = "prisonerFeedback",
  )
}

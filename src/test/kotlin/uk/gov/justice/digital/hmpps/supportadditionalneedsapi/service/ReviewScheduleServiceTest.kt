package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.ReviewConfig
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspReviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspReviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ReviewScheduleServiceTest {

  @Mock
  private lateinit var reviewScheduleRepository: ReviewScheduleRepository

  @Mock
  private lateinit var reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository

  @Mock
  private lateinit var reviewScheduleHistoryMapper: ReviewScheduleHistoryMapper

  @Mock
  private lateinit var eventPublisher: EventPublisher

  @Mock
  private lateinit var workingDayService: WorkingDayService

  @Mock
  private lateinit var reviewRepository: ElspReviewRepository

  @Mock
  private lateinit var elspPlanRepository: ElspPlanRepository

  @Mock
  private lateinit var elspReviewRepository: ElspReviewRepository

  private val prisonNumber = randomValidPrisonNumber()
  private val prisonId = "AAA"
  private val pesContractDate: LocalDate = LocalDate.parse("2025-10-01")
  private val daysToAdd = 5L

  private lateinit var reviewConfig: ReviewConfig
  private lateinit var service: ReviewScheduleService

  @BeforeEach
  fun setUp() {
    reviewConfig = ReviewConfig(reviewDeadlineDaysToAdd = daysToAdd, additionalReviewDateLogic = true)
    service = ReviewScheduleService(
      reviewScheduleRepository = reviewScheduleRepository,
      reviewScheduleHistoryRepository = reviewScheduleHistoryRepository,
      reviewScheduleHistoryMapper = reviewScheduleHistoryMapper,
      eventPublisher = eventPublisher,
      pesContractDate = pesContractDate,
      workingDayService = workingDayService,
      reviewRepository = reviewRepository,
      elspPlanRepository = elspPlanRepository,
      reviewConfig = reviewConfig,
      elspReviewRepository = elspReviewRepository,
    )
  }

  @Nested
  inner class CreateOrUpdateDueToEducationUpdate {
    @Test
    fun `should create new schedule given no existing review schedule`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      givenWorkingDayServiceAddsCalendarDays()
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.prisonNumber).isEqualTo(prisonNumber)
      assertThat(saved.deadlineDate).isEqualTo(startDate.plusDays(daysToAdd))
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      assertThat(saved.createdAtPrison).isEqualTo(prisonId)
      assertThat(saved.updatedAtPrison).isEqualTo(prisonId)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @ParameterizedTest
    @EnumSource(value = ReviewScheduleStatus::class, names = ["SCHEDULED"], mode = EnumSource.Mode.EXCLUDE)
    fun `should create new schedule given existing review schedule is not SCHEDULED`(status: ReviewScheduleStatus) {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = status, deadlineDate = LocalDate.parse("2025-12-01"))
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved).isNotSameAs(existing)
      assertThat(saved.deadlineDate).isEqualTo(startDate.plusDays(daysToAdd))
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      assertThat(existing.status).isEqualTo(status)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @Test
    fun `should base new schedule deadline on PES contract date given education start date is before PES contract date`() {
      // Given
      val startDate = pesContractDate.minusMonths(6)
      givenWorkingDayServiceAddsCalendarDays()
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(pesContractDate.plusDays(daysToAdd))
      verify(workingDayService).getNextWorkingDayNDaysFromDate(daysToAdd, startDate)
      verify(workingDayService).getNextWorkingDayNDaysFromDate(daysToAdd, pesContractDate)
    }

    @Test
    fun `should bring deadline forward without checking plan or reviews given additional review date logic is disabled`() {
      // Given
      reviewConfig.additionalReviewDateLogic = false
      val startDate = LocalDate.parse("2026-01-10")
      val proposedDeadline = startDate.plusDays(daysToAdd)
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = LocalDate.parse("2026-03-01"))
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved).isSameAs(existing)
      assertThat(saved.deadlineDate).isEqualTo(proposedDeadline)
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
      verifyNoInteractions(elspPlanRepository, elspReviewRepository)
    }

    @Test
    fun `should not change deadline or check plan and reviews given SCHEDULED review exists and proposed deadline is later`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val currentDeadline = LocalDate.parse("2026-01-12")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      assertThat(existing.deadlineDate).isEqualTo(currentDeadline)
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(elspPlanRepository, elspReviewRepository, eventPublisher)
    }

    @Test
    fun `should not change deadline given SCHEDULED review exists and proposed deadline is the same`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val currentDeadline = startDate.plusDays(daysToAdd)
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      assertThat(existing.deadlineDate).isEqualTo(currentDeadline)
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `should keep current deadline given education start date is before plan creation date`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val currentDeadline = LocalDate.parse("2026-03-01")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)
      given(elspPlanRepository.findByPrisonNumber(prisonNumber)).willReturn(anElspPlan(createdOn = startDate.plusDays(1)))

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      assertThat(existing.deadlineDate).isEqualTo(currentDeadline)
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(elspReviewRepository, eventPublisher)
    }

    @Test
    fun `should keep current deadline given education start date is the same as plan creation date`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val currentDeadline = LocalDate.parse("2026-03-01")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)
      given(elspPlanRepository.findByPrisonNumber(prisonNumber)).willReturn(anElspPlan(createdOn = startDate))

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      assertThat(existing.deadlineDate).isEqualTo(currentDeadline)
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(elspReviewRepository, eventPublisher)
    }

    @Test
    fun `should keep current deadline given education start date is on or before last review creation date`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val currentDeadline = LocalDate.parse("2026-03-01")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)
      given(elspPlanRepository.findByPrisonNumber(prisonNumber)).willReturn(anElspPlan(createdOn = startDate.minusMonths(2)))
      given(elspReviewRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
        .willReturn(anElspReview(createdOn = startDate))

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      assertThat(existing.deadlineDate).isEqualTo(currentDeadline)
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(eventPublisher)
    }

    @Test
    fun `should bring deadline forward given education start date is after plan and last review creation dates`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val proposedDeadline = startDate.plusDays(daysToAdd)
      val currentDeadline = LocalDate.parse("2026-03-01")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)
      given(elspPlanRepository.findByPrisonNumber(prisonNumber)).willReturn(anElspPlan(createdOn = startDate.minusMonths(2)))
      given(elspReviewRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber))
        .willReturn(anElspReview(createdOn = startDate.minusDays(1)))

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved).isSameAs(existing)
      assertThat(saved.deadlineDate).isEqualTo(proposedDeadline)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @Test
    fun `should bring deadline forward given no plan and no previous review exist`() {
      // Given
      val startDate = LocalDate.parse("2026-01-10")
      val proposedDeadline = startDate.plusDays(daysToAdd)
      val currentDeadline = LocalDate.parse("2026-03-01")
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = currentDeadline)
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)
      given(elspPlanRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
      given(elspReviewRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToEducationUpdate(prisonNumber, startDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(proposedDeadline)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }
  }

  @Nested
  inner class CreateOrUpdateDueToNeedChange {

    private val educationStartDate = LocalDate.parse("2026-01-10")

    @Test
    fun `should do nothing given a SCHEDULED review schedule already exists`() {
      // Given
      val existing = aReviewSchedule(status = ReviewScheduleStatus.SCHEDULED, deadlineDate = LocalDate.parse("2026-03-01"))
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, educationStartDate.minusDays(1), prisonId)

      // Then
      verify(reviewScheduleRepository, never()).save(any())
      verifyNoInteractions(eventPublisher, workingDayService)
    }

    @Test
    fun `should create schedule with deadline based on education start date given ALN assessment is before education start`() {
      // Given
      givenWorkingDayServiceAddsCalendarDays()
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, educationStartDate.minusDays(1), prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.prisonNumber).isEqualTo(prisonNumber)
      assertThat(saved.deadlineDate).isEqualTo(educationStartDate.plusDays(daysToAdd))
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      assertThat(saved.createdAtPrison).isEqualTo(prisonId)
      assertThat(saved.updatedAtPrison).isEqualTo(prisonId)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @Test
    fun `should create schedule with deadline based on education start date given ALN assessment is on education start date`() {
      // Given
      givenWorkingDayServiceAddsCalendarDays()
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, educationStartDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(educationStartDate.plusDays(daysToAdd))
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @Test
    fun `should create schedule with deadline based on PES contract date given education started before PES contract date`() {
      // Given
      val earlyStartDate = pesContractDate.minusMonths(3)
      givenWorkingDayServiceAddsCalendarDays()
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, earlyStartDate, earlyStartDate.minusDays(1), prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(pesContractDate.plusDays(daysToAdd))
    }

    @Test
    fun `should create schedule with future deadline given ALN assessment is after education start`() {
      // Given
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, educationStartDate.plusDays(1), prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
      verifyNoInteractions(workingDayService)
    }

    @Test
    fun `should create schedule with future deadline given no ALN assessment date`() {
      // Given
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, null, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
      verifyNoInteractions(workingDayService)
    }

    @Test
    fun `should create schedule with future deadline given no education start date`() {
      // Given
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, null, educationStartDate, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
      verifyNoInteractions(workingDayService)
    }

    @Test
    fun `should create schedule with future deadline given neither education start nor ALN assessment date`() {
      // Given
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(null)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, null, null, prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved.deadlineDate).isEqualTo(IN_THE_FUTURE_DATE)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }

    @ParameterizedTest
    @EnumSource(value = ReviewScheduleStatus::class, names = ["SCHEDULED"], mode = EnumSource.Mode.EXCLUDE)
    fun `should create new schedule given existing review schedule is not SCHEDULED`(status: ReviewScheduleStatus) {
      // Given
      givenWorkingDayServiceAddsCalendarDays()
      val existing = aReviewSchedule(status = status, deadlineDate = LocalDate.parse("2025-12-01"))
      given(reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)).willReturn(existing)

      // When
      service.createOrUpdateDueToNeedChange(prisonNumber, educationStartDate, educationStartDate.minusDays(1), prisonId)

      // Then
      val saved = captureSavedSchedule()
      assertThat(saved).isNotSameAs(existing)
      assertThat(saved.deadlineDate).isEqualTo(educationStartDate.plusDays(daysToAdd))
      assertThat(saved.status).isEqualTo(ReviewScheduleStatus.SCHEDULED)
      assertThat(existing.status).isEqualTo(status)
      verify(eventPublisher).createAndPublishReviewScheduleEvent(prisonNumber)
    }
  }

  private fun givenWorkingDayServiceAddsCalendarDays() {
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willAnswer {
      (it.arguments[1] as LocalDate).plusDays(it.arguments[0] as Long)
    }
  }

  private fun captureSavedSchedule(): ReviewScheduleEntity {
    val captor = argumentCaptor<ReviewScheduleEntity>()
    verify(reviewScheduleRepository).save(captor.capture())
    return captor.firstValue
  }

  private fun aReviewSchedule(
    status: ReviewScheduleStatus,
    deadlineDate: LocalDate,
  ) = ReviewScheduleEntity(
    prisonNumber = prisonNumber,
    deadlineDate = deadlineDate,
    status = status,
    createdAtPrison = prisonId,
    updatedAtPrison = prisonId,
  )

  private fun anElspPlan(createdOn: LocalDate) = ElspPlanEntity(
    prisonNumber = prisonNumber,
    individualSupport = "Some support required",
    createdAtPrison = prisonId,
    updatedAtPrison = prisonId,
  ).apply { createdAt = createdOn.atStartOfDay(ZoneId.systemDefault()).toInstant() }

  private fun anElspReview(createdOn: LocalDate) = ElspReviewEntity(
    prisonNumber = prisonNumber,
    createdAtPrison = prisonId,
    updatedAtPrison = prisonId,
    reviewScheduleReference = UUID.randomUUID(),
  ).apply { createdAt = createdOn.atStartOfDay(ZoneId.systemDefault()).toInstant() }
}

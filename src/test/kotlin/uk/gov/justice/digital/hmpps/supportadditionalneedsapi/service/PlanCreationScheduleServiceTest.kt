package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource.ALN_SCREENER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.Clock
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PlanCreationScheduleServiceTest {

  @InjectMocks
  private lateinit var service: PlanCreationScheduleService

  @Mock
  private lateinit var planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository

  @Mock
  private lateinit var planCreationScheduleRepository: PlanCreationScheduleRepository

  @Mock
  private lateinit var planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper

  @Mock
  private lateinit var needService: NeedService

  @Mock
  private lateinit var eventPublisher: EventPublisher

  @Mock
  private lateinit var pesContractDate: LocalDate

  @Mock
  private lateinit var elspPlanRepository: ElspPlanRepository

  @Mock
  private lateinit var workingDayService: WorkingDayService

  @Mock
  private lateinit var clock: Clock

  private val prisonNumber = randomValidPrisonNumber()

  @Test
  fun `createSchedule creates when in education and has need`() {
    // Given
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))

    val deadline = LocalDate.now().plusDays(5)
    val earliest = LocalDate.now()

    // When
    service.createSchedule(prisonNumber = prisonNumber, prisonId = "MDI", deadlineDate = deadline, earliestStartDate = earliest)

    // Then
    verify(planCreationScheduleRepository).saveAndFlush(
      check<PlanCreationScheduleEntity> {
        assertEquals(prisonNumber, it.prisonNumber)
        assertEquals(PlanCreationScheduleStatus.SCHEDULED, it.status)
        assertEquals("MDI", it.createdAtPrison)
        assertEquals("MDI", it.updatedAtPrison)
        assertEquals(deadline, it.deadlineDate)
        assertEquals(earliest, it.earliestStartDate)
        assertEquals(setOf(ALN_SCREENER), it.needSources)
      },
    )
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createSchedule does nothing if not in education or no need`() {
    // Given

    // When
    service.createSchedule(prisonNumber, deadlineDate = IN_THE_FUTURE_DATE, earliestStartDate = null, prisonId = "BXI")

    // Then
    verify(needService).hasNeed(prisonNumber)
    verify(needService, never()).getNeedSources(any())
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(eventPublisher, never()).createAndPublishPlanCreationSchedule(any())
  }

  @Test
  fun `should return deadline date based on PES contract date given PES contract date plus DEADLINE_DAYS_TO_ADD is greater than education start date plus DEADLINE_DAYS_TO_ADD`() {
    // Given
    val expectedNumberOfDaysToAdd = 5L // PES rules adds 5 days to dates

    val pesContractDatePlus5Days = LocalDate.parse("2025-10-06") // PES contract date 01/10/2025 + 5 days

    val educationStartDate = LocalDate.parse("2024-09-01")
    val educationStartDatePlus5Days = LocalDate.parse("2024-09-06") // education start date plus 5 days

    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any()))
      .willReturn(
        educationStartDatePlus5Days,
        pesContractDatePlus5Days,
      )

    // When
    val result = service.getDeadlineDate(educationStartDate)

    // Then
    assertThat(result).isEqualTo(pesContractDatePlus5Days)
    verify(workingDayService).getNextWorkingDayNDaysFromDate(expectedNumberOfDaysToAdd, educationStartDate)
    verify(workingDayService).getNextWorkingDayNDaysFromDate(expectedNumberOfDaysToAdd, pesContractDate)
  }

  @Test
  fun `should return deadline date based on education start date given education start date plus DEADLINE_DAYS_TO_ADD is greater than PES contract date plus DEADLINE_DAYS_TO_ADD`() {
    // Given
    val expectedNumberOfDaysToAdd = 5L // PES rules adds 5 days to dates

    val pesContractDatePlus5Days = LocalDate.parse("2025-10-06") // PES contract date 01/10/2025 + 5 days

    val educationStartDate = LocalDate.parse("2025-10-02")
    val educationStartDatePlus5Days = LocalDate.parse("2025-10-07") // education start date plus 5 days

    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any()))
      .willReturn(
        educationStartDatePlus5Days,
        pesContractDatePlus5Days,
      )

    // When
    val result = service.getDeadlineDate(educationStartDate)

    // Then
    assertThat(result).isEqualTo(educationStartDatePlus5Days)
    verify(workingDayService).getNextWorkingDayNDaysFromDate(expectedNumberOfDaysToAdd, educationStartDate)
    verify(workingDayService).getNextWorkingDayNDaysFromDate(expectedNumberOfDaysToAdd, pesContractDate)
  }

  @Test
  fun `completeSchedule marks completed when scheduled`() {
    // Given
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = LocalDate.now(),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = null,
    )
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(existing)

    // When
    service.completeSchedule(prisonNumber, "MDI")

    // Then
    assertEquals(PlanCreationScheduleStatus.COMPLETED, existing.status)
    assertEquals("MDI", existing.updatedAtPrison)
    verify(planCreationScheduleRepository).save(existing)
  }

  private fun verifyNoDeadlineForPlanCreation() {
    verify(planCreationScheduleRepository).saveAndFlush(
      check<PlanCreationScheduleEntity> {
        assertEquals(PlanCreationScheduleStatus.SCHEDULED, it.status)
        assertEquals(IN_THE_FUTURE_DATE, it.deadlineDate)
        assertEquals(null, it.earliestStartDate)
      },
    )
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate saves plan creation schedule with deadline when no existing schedule and person has need and enrolled in PES course`() {
    // Given
    val startDate = LocalDate.parse("2025-10-02")
    val expectedDeadline = LocalDate.parse("2025-10-09")
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willReturn(expectedDeadline)

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = startDate,
      isPesCourse = true,
      subjectToKPIRules = true,
      prisonId = "MDI",
    )

    // Then
    verify(planCreationScheduleRepository).saveAndFlush(
      check<PlanCreationScheduleEntity> {
        assertEquals(PlanCreationScheduleStatus.SCHEDULED, it.status)
        assertEquals(expectedDeadline, it.deadlineDate)
        assertEquals(startDate, it.earliestStartDate)
        assertEquals(setOf(ALN_SCREENER), it.needSources)
      },
    )
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate saves plan creation schedule with no deadline when no existing schedule and person has need and person isn't subject to KPI rules and enrolled in PES course`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = LocalDate.parse("2025-10-02"),
      isPesCourse = true,
      subjectToKPIRules = false,
      prisonId = "MDI",
    )

    // Then
    verifyNoDeadlineForPlanCreation()
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate saves plan creation schedule with no deadline when no existing schedule and person has need and enrolled in non-PES course and subject to KPI rules`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = LocalDate.parse("2025-10-02"),
      isPesCourse = false,
      subjectToKPIRules = true,
      prisonId = "MDI",
    )

    // Then
    verifyNoDeadlineForPlanCreation()
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate does not save plan creation schedule when no existing schedule and person has no need`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(false)

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = LocalDate.parse("2025-10-02"),
      isPesCourse = false,
      subjectToKPIRules = false,
      prisonId = "MDI",
    )

    // Then
    verify(needService).hasNeed(prisonNumber)
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(eventPublisher, never()).createAndPublishPlanCreationSchedule(any())
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate does not check need when schedule already exists`() {
    // Given
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.EXEMPT_PRISONER_TRANSFER,
      deadlineDate = LocalDate.parse("2025-01-01"),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = null,
    )
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(existing)

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = LocalDate.parse("2025-10-02"),
      isPesCourse = false,
      subjectToKPIRules = false,
      prisonId = "MDI",
    )

    // Then
    verify(needService, never()).hasNeed(any())
    verify(planCreationScheduleRepository).saveAndFlush(existing)
    assertEquals(PlanCreationScheduleStatus.SCHEDULED, existing.status)
  }

  @Test
  fun `createOrUpdateDueToEducationUpdate does not update plan creation schedule when existing schedule is already scheduled with the same deadline and earliest start date`() {
    // Given
    val startDate = LocalDate.parse("2025-10-02")
    val expectedDeadline = LocalDate.parse("2025-10-09")
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = expectedDeadline,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = startDate,
    )
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(existing)
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willReturn(expectedDeadline)

    // When
    service.createOrUpdateDueToEducationUpdate(
      prisonNumber = prisonNumber,
      startDate = startDate,
      isPesCourse = true,
      subjectToKPIRules = true,
      prisonId = "MDI",
    )

    // Then
    verify(needService, never()).hasNeed(any())
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(eventPublisher, never()).createAndPublishPlanCreationSchedule(any())
    assertEquals(PlanCreationScheduleStatus.SCHEDULED, existing.status)
    assertEquals(expectedDeadline, existing.deadlineDate)
    assertEquals(startDate, existing.earliestStartDate)
    assertEquals("BXI", existing.updatedAtPrison)
  }

  @Test
  fun `createOrUpdateDueToNeedChange saves plan creation schedule with deadline when no existing schedule, person has need and education started on or after ALN assessment`() {
    // Given
    val educationStartDate = LocalDate.parse("2025-10-02")
    val alnAssessmentDate = LocalDate.parse("2025-10-01")
    val expectedDeadline = LocalDate.parse("2025-10-09")
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willReturn(expectedDeadline)

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = educationStartDate,
      alnAssessmentDate = alnAssessmentDate,
      prisonId = "MDI",
    )

    // Then
    verify(planCreationScheduleRepository).saveAndFlush(
      check<PlanCreationScheduleEntity> {
        assertEquals(PlanCreationScheduleStatus.SCHEDULED, it.status)
        assertEquals(expectedDeadline, it.deadlineDate)
        assertEquals(educationStartDate, it.earliestStartDate)
        assertEquals(setOf(ALN_SCREENER), it.needSources)
      },
    )
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToNeedChange saves plan creation schedule with no deadline when no existing schedule, person has need and ALN assessment was after education start`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = LocalDate.parse("2025-10-01"),
      alnAssessmentDate = LocalDate.parse("2025-10-02"),
      prisonId = "MDI",
    )

    // Then
    verifyNoDeadlineForPlanCreation()
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToNeedChange saves plan creation schedule with no deadline when no existing schedule, person has need and no dates provided`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(true)
    given(needService.getNeedSources(prisonNumber)).willReturn(sortedSetOf(ALN_SCREENER))

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = null,
      alnAssessmentDate = null,
      prisonId = "MDI",
    )

    // Then
    verifyNoDeadlineForPlanCreation()
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
  }

  @Test
  fun `createOrUpdateDueToNeedChange does not save plan creation schedule when no existing schedule, person has no need and education started on or after ALN assessment`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(false)
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willReturn(LocalDate.parse("2025-10-09"))

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = LocalDate.parse("2025-10-02"),
      alnAssessmentDate = LocalDate.parse("2025-10-01"),
      prisonId = "MDI",
    )

    // Then
    verify(needService).hasNeed(prisonNumber)
    verify(needService, never()).getNeedSources(any())
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(eventPublisher, never()).createAndPublishPlanCreationSchedule(any())
  }

  @Test
  fun `createOrUpdateDueToNeedChange does not save plan creation schedule when no existing schedule, person has no need and prisoner is on non-PES course`() {
    // Given
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(null)
    given(needService.hasNeed(prisonNumber)).willReturn(false)

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      // A null educationStartDate indicates a non-PES course
      educationStartDate = null,
      alnAssessmentDate = null,
      prisonId = "MDI",
    )

    // Then
    verify(needService).hasNeed(prisonNumber)
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(eventPublisher, never()).createAndPublishPlanCreationSchedule(any())
  }

  @Test
  fun `createOrUpdateDueToNeedChange does not check need and reschedules when existing schedule is EXEMPT_NO_NEED`() {
    // Given
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.EXEMPT_NO_NEED,
      deadlineDate = LocalDate.parse("2025-01-01"),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = null,
    )
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(existing)

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = null,
      alnAssessmentDate = null,
      prisonId = "MDI",
    )

    // Then
    verify(needService, never()).hasNeed(any())
    verify(planCreationScheduleRepository).saveAndFlush(existing)
    verify(eventPublisher).createAndPublishPlanCreationSchedule(prisonNumber)
    assertEquals(PlanCreationScheduleStatus.SCHEDULED, existing.status)
    assertEquals(IN_THE_FUTURE_DATE, existing.deadlineDate)
    assertEquals("MDI", existing.updatedAtPrison)
  }

  /**
   * This logic is incorrect and will be fixed in ESWE-2439.
   * 
   * When an existing schedule is EXEMPT_NO_NEED and education started on or after the ALN assessment, both update
   * branches in createOrUpdateDueToNeedChange run: the first sets the education based (KPI) deadline, then the
   * EXEMPT_NO_NEED branch overwrites it with IN_THE_FUTURE_DATE, saving and publishing an event twice.
   *
   * Expected behaviour:
   *  - status is SCHEDULED
   *  - deadlineDate is the education based deadline (getDeadlineDate(educationStartDate)), not IN_THE_FUTURE_DATE
   *  - earliestStartDate is educationStartDate
   *  - the schedule is saved once and a single event is published
   */
  @Test
  fun `createOrUpdateDueToNeedChange overwrites education based deadline and saves twice when existing schedule is EXEMPT_NO_NEED and education started on or after ALN assessment`() {
    // Given
    val educationStartDate = LocalDate.parse("2025-10-02")
    val alnAssessmentDate = LocalDate.parse("2025-10-01")
    val expectedDeadline = LocalDate.parse("2025-10-09")
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.EXEMPT_NO_NEED,
      deadlineDate = LocalDate.parse("2025-01-01"),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = null,
    )
    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(existing)
    given(workingDayService.getNextWorkingDayNDaysFromDate(any(), any())).willReturn(expectedDeadline)

    // When
    service.createOrUpdateDueToNeedChange(
      prisonNumber = prisonNumber,
      educationStartDate = educationStartDate,
      alnAssessmentDate = alnAssessmentDate,
      prisonId = "MDI",
    )

    // Then
    assertEquals(PlanCreationScheduleStatus.SCHEDULED, existing.status)
    assertEquals(IN_THE_FUTURE_DATE, existing.deadlineDate)
    assertEquals(educationStartDate, existing.earliestStartDate)
    assertEquals("MDI", existing.updatedAtPrison)
    verify(needService, never()).hasNeed(any())
    verify(planCreationScheduleRepository, times(2)).saveAndFlush(existing)
    verify(eventPublisher, times(2)).createAndPublishPlanCreationSchedule(prisonNumber)
  }
}

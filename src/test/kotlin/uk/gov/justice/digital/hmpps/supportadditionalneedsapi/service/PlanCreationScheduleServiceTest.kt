package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.Mockito.lenient
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.PLAN_DEADLINE_DAYS_TO_ADD
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource.ALN_SCREENER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspPlanMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday.WorkingDayService
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PlanCreationScheduleServiceTest {

  @Mock
  private lateinit var planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository

  @Mock
  private lateinit var planCreationScheduleRepository: PlanCreationScheduleRepository

  @Mock
  private lateinit var planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper

  @Mock
  private lateinit var educationSupportPlanRepository: ElspPlanRepository

  @Mock
  private lateinit var educationService: EducationService

  @Mock
  private lateinit var needService: NeedService

  @Mock
  private lateinit var eventPublisher: EventPublisher

  @InjectMocks
  private lateinit var service: PlanCreationScheduleService

  @Mock
  private lateinit var pesContractDate: LocalDate

  @Mock
  private lateinit var elspPlanMapper: ElspPlanMapper

  @Mock
  private lateinit var elspPlanRepository: ElspPlanRepository

  @Mock
  private lateinit var workingDayService: WorkingDayService

  private val prisonNumber = randomValidPrisonNumber()

  @BeforeEach
  internal fun setUp() {
    pesContractDate = LocalDate.now()
    reset(
      planCreationScheduleHistoryRepository,
      planCreationScheduleRepository,
      planCreationScheduleHistoryMapper,
      educationSupportPlanRepository,
      educationService,
      needService,
      eventPublisher,
      elspPlanMapper,
      elspPlanRepository,
    )

    service = PlanCreationScheduleService(
      planCreationScheduleHistoryRepository,
      planCreationScheduleRepository,
      planCreationScheduleHistoryMapper,
      needService,
      eventPublisher,
      LocalDate.parse("2025-10-01"),
      elspPlanRepository,
      workingDayService,
    )

    // set up common working day scenarios:
    lenient().whenever(workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, LocalDate.now())).thenReturn(
      LocalDate.now().plusDays(
        PLAN_DEADLINE_DAYS_TO_ADD,
      ),
    )
    lenient().whenever(workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, LocalDate.of(2025, 10, 1)))
      .thenReturn(LocalDate.of(2025, 10, 1).plusDays(PLAN_DEADLINE_DAYS_TO_ADD))
    lenient().whenever(workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, LocalDate.now().plusMonths(6))).thenReturn(LocalDate.now().plusMonths(6).plusDays(PLAN_DEADLINE_DAYS_TO_ADD))
    lenient().whenever(workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, LocalDate.of(2024, 10, 1)))
      .thenReturn(LocalDate.of(2024, 10, 1).plusDays(PLAN_DEADLINE_DAYS_TO_ADD))
  }

  private fun setUpService() = PlanCreationScheduleService(
    planCreationScheduleHistoryRepository,
    planCreationScheduleRepository,
    planCreationScheduleHistoryMapper,
    needService,
    eventPublisher,
    LocalDate.parse("2024-10-01"),
    elspPlanRepository,
    workingDayService,
  )

  @Test
  fun `createSchedule creates when in education and has need`() {
    setUpService()
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)
    whenever(needService.getNeedSources(prisonNumber)).thenReturn(sortedSetOf(ALN_SCREENER))
    whenever(planCreationScheduleRepository.saveAndFlush(any())).thenAnswer { it.arguments[0] }

    val deadline = LocalDate.now().plusDays(5)
    val earliest = LocalDate.now()

    service.createSchedule(prisonNumber = prisonNumber, prisonId = "MDI", deadlineDate = deadline, earliestStartDate = earliest)

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
  }

  @Test
  fun `createSchedule does nothing if not in education or no need`() {
    service.createSchedule(prisonNumber, deadlineDate = IN_THE_FUTURE_DATE, earliestStartDate = null, prisonId = "BXI")

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `getDeadlineDate returns max of start date plus DEADLINE_DAYS_TO_ADD and PES plus DEADLINE_DAYS_TO_ADD`() {
    val start = LocalDate.parse("2024-09-01")
    whenever(workingDayService.getNextWorkingDayNDaysFromDate(PLAN_DEADLINE_DAYS_TO_ADD, start)).thenReturn(LocalDate.parse("2024-09-06"))

    val result = service.getDeadlineDate(start)
    assertEquals(LocalDate.parse("2025-10-01").plusDays(PLAN_DEADLINE_DAYS_TO_ADD), result)
  }

  @Test
  fun `completeSchedule marks completed when scheduled`() {
    val existing = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = LocalDate.now(),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = null,
    )
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(existing)

    service.completeSchedule(prisonNumber, "MDI")

    assertEquals(PlanCreationScheduleStatus.COMPLETED, existing.status)
    assertEquals("MDI", existing.updatedAtPrison)
    verify(planCreationScheduleRepository).save(existing)
  }
}

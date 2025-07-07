package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource.ALN_SCREENER
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventPublisher
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.Instant
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

  private val prisonNumber = randomValidPrisonNumber()

  @BeforeEach
  internal fun setUp() {
    pesContractDate = LocalDate.now()
  }

  @Test
  fun `does nothing if plan already exists`() {
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(mock())

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(planCreationScheduleHistoryRepository, never()).save(any())
  }

  @Test
  fun `does nothing if schedule already exists`() {
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(mock())

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(planCreationScheduleHistoryRepository, never()).save(any())
  }

  @Test
  fun `does nothing if not in education or no need`() {
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(false)

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `creates plan schedule and history when PES contract date is in the past`() {
    val pesContractDate = LocalDate.parse("2024-10-01")
    assertCreatesPlanWithExpectedDeadline(pesContractDate, LocalDate.now().plusDays(5))
  }

  @Test
  fun `creates plan schedule and history when PES contract date is in the future`() {
    val pesContractDate = LocalDate.now().plusMonths(6)
    assertCreatesPlanWithExpectedDeadline(pesContractDate, pesContractDate.plusDays(5))
  }

  private fun assertCreatesPlanWithExpectedDeadline(
    pesContractDate: LocalDate,
    expectedDeadline: LocalDate,
  ) {
    val service = PlanCreationScheduleService(
      planCreationScheduleHistoryRepository,
      planCreationScheduleRepository,
      planCreationScheduleHistoryMapper,
      educationSupportPlanRepository,
      educationService,
      needService,
      eventPublisher,
      pesContractDate,
    )

    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)
    whenever(needService.getNeedSources(prisonNumber)).thenReturn(setOf(ALN_SCREENER))

    val savedEntity = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = expectedDeadline,
      createdAtPrison = "N/A",
      updatedAtPrison = "N/A",
      version = 1,
      needSources = setOf(ALN_SCREENER),
    ).apply {
      createdAt = Instant.now()
      updatedAt = Instant.now()
      createdBy = "system"
      updatedBy = "system"
    }

    whenever(planCreationScheduleRepository.saveAndFlush(any())).thenReturn(savedEntity)

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository).saveAndFlush(
      check {
        assert(it.prisonNumber == prisonNumber)
        assertEquals(it.status, PlanCreationScheduleStatus.SCHEDULED)
        assertEquals(expectedDeadline, it.deadlineDate)
        assertEquals(savedEntity.needSources, it.needSources)
      },
    )
    verify(eventPublisher).createAndPublishPlanCreationSchedule(eq(prisonNumber), any<Instant>())
  }

  @Test
  fun `attemptToUpdate does nothing if plan already exists`() {
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(mock())

    service.attemptToUpdate(prisonNumber)

    verify(planCreationScheduleRepository, never()).findByPrisonNumber(any())
  }

  @Test
  fun `attemptToUpdate does nothing if no schedule exists`() {
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)

    service.attemptToUpdate(prisonNumber)

    verify(planCreationScheduleRepository).findByPrisonNumber(prisonNumber)
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `attemptToUpdate does nothing if schedule is already completed`() {
    val schedule = planCreationScheduleEntity(PlanCreationScheduleStatus.COMPLETED)
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(schedule)

    service.attemptToUpdate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  private fun planCreationScheduleEntity(status: PlanCreationScheduleStatus) =
    PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = status,
      deadlineDate = LocalDate.now(),
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
    )

  @Test
  fun `attemptToUpdate transition from SCHEDULED to EXEMPT_NOT_IN_EDUCATION`() {
    val service = setUpService()
    val schedule = planCreationScheduleEntity(PlanCreationScheduleStatus.SCHEDULED)
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(schedule)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(false)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)

    service.attemptToUpdate(prisonNumber, "MDI")

    assertEquals(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION, schedule.status)
    assertEquals("MDI", schedule.updatedAtPrison)
    assertEquals(null, schedule.deadlineDate)
    verify(planCreationScheduleRepository).saveAndFlush(schedule)
    verify(eventPublisher).createAndPublishPlanCreationSchedule(any<String>(), any<Instant>())
  }

  private fun setUpService() = PlanCreationScheduleService(
    planCreationScheduleHistoryRepository,
    planCreationScheduleRepository,
    planCreationScheduleHistoryMapper,
    educationSupportPlanRepository,
    educationService,
    needService,
    eventPublisher,
    LocalDate.parse("2024-10-01"),
  )

  @Test
  fun `attemptToUpdate transition from EXEMPT_NOT_IN_EDUCATION to SCHEDULED`() {
    val service = setUpService()
    val schedule = planCreationScheduleEntity(PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION)
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(schedule)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)

    service.attemptToUpdate(prisonNumber, "MDI")

    assertEquals(PlanCreationScheduleStatus.SCHEDULED, schedule.status)
    assertEquals("MDI", schedule.updatedAtPrison)
    assertEquals(LocalDate.now().plusDays(5), schedule.deadlineDate)
    verify(planCreationScheduleRepository).saveAndFlush(schedule)
    verify(eventPublisher).createAndPublishPlanCreationSchedule(any<String>(), any<Instant>())
  }

  @Test
  fun `attemptToUpdate transition from SCHEDULED to EXEMPT_NO_NEED`() {
    val service = setUpService()
    val schedule = planCreationScheduleEntity(PlanCreationScheduleStatus.SCHEDULED)
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(schedule)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(false)

    service.attemptToUpdate(prisonNumber, "MDI")

    assertEquals(PlanCreationScheduleStatus.EXEMPT_NO_NEED, schedule.status)
    assertEquals("MDI", schedule.updatedAtPrison)
    assertEquals(null, schedule.deadlineDate)
    verify(planCreationScheduleRepository).saveAndFlush(schedule)
    verify(eventPublisher).createAndPublishPlanCreationSchedule(any<String>(), any<Instant>())
  }

  @Test
  fun `attemptToUpdate transition from EXEMPT_NO_NEED to SCHEDULED`() {
    val service = setUpService()
    val schedule = planCreationScheduleEntity(PlanCreationScheduleStatus.EXEMPT_NO_NEED)
    whenever(educationSupportPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(schedule)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)

    service.attemptToUpdate(prisonNumber, "MDI")

    assertEquals(PlanCreationScheduleStatus.SCHEDULED, schedule.status)
    assertEquals("MDI", schedule.updatedAtPrison)
    assertEquals(null, schedule.deadlineDate)
    verify(planCreationScheduleRepository).saveAndFlush(schedule)
    verify(eventPublisher).createAndPublishPlanCreationSchedule(any<String>(), any<Instant>())
  }
}

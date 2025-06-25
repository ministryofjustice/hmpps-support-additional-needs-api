package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Assertions.assertEquals
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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
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
  private lateinit var educationSupportPlanService: EducationSupportPlanService

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

  @Test
  fun `does nothing if plan already exists`() {
    whenever(educationSupportPlanService.getPlan(prisonNumber)).thenReturn(mock())

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(planCreationScheduleHistoryRepository, never()).save(any())
  }

  @Test
  fun `does nothing if schedule already exists`() {
    whenever(educationSupportPlanService.getPlan(prisonNumber)).thenThrow(PlanNotFoundException("not found"))
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(mock())

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
    verify(planCreationScheduleHistoryRepository, never()).save(any())
  }

  @Test
  fun `does nothing if not in education or no need`() {
    whenever(educationSupportPlanService.getPlan(prisonNumber)).thenThrow(PlanNotFoundException("not found"))
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(false)

    service.attemptToCreate(prisonNumber)

    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `creates plan schedule and history when PES contract date is in the past`() {
    val pesContractDate = LocalDate.parse("2024-10-01")
    assertCreatesPlanWithExpectedDeadline(pesContractDate, LocalDate.now().plusDays(10))
  }

  @Test
  fun `creates plan schedule and history when PES contract date is in the future`() {
    val pesContractDate = LocalDate.now().plusMonths(6)
    assertCreatesPlanWithExpectedDeadline(pesContractDate, pesContractDate.plusDays(10))
  }

  private fun assertCreatesPlanWithExpectedDeadline(
    pesContractDate: LocalDate,
    expectedDeadline: LocalDate,
  ) {
    val service = PlanCreationScheduleService(
      planCreationScheduleHistoryRepository,
      planCreationScheduleRepository,
      planCreationScheduleHistoryMapper,
      educationSupportPlanService,
      educationService,
      needService,
      eventPublisher,
      pesContractDate,
    )

    whenever(educationSupportPlanService.getPlan(prisonNumber)).thenThrow(PlanNotFoundException("not found"))
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)

    val savedEntity = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = expectedDeadline,
      createdAtPrison = "N/A",
      updatedAtPrison = "N/A",
      version = 1,
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
      },
    )
    verify(eventPublisher).createAndPublishPlanCreationSchedule(eq(prisonNumber), any<Instant>())
  }
}

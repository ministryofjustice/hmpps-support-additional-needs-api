package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.check
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

  @InjectMocks
  private lateinit var service: PlanCreationScheduleService

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
  fun `creates plan schedule and history if no plan, no existing schedule, in education, and has need`() {
    whenever(educationSupportPlanService.getPlan(prisonNumber)).thenThrow(PlanNotFoundException("not found"))
    whenever(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)
    whenever(educationService.inEducation(prisonNumber)).thenReturn(true)
    whenever(needService.hasNeed(prisonNumber)).thenReturn(true)

    val savedEntity = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = LocalDate.now().plusDays(10),
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
        assert(it.status == PlanCreationScheduleStatus.SCHEDULED)
        assert(it.deadlineDate == LocalDate.now().plusDays(10))
      },
    )
  }
}

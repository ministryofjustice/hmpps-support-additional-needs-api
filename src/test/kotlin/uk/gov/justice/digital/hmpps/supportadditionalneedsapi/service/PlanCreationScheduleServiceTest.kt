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
  }

  @Test
  fun `createSchedule does nothing if not in education or no need`() {
    // Given

    // When
    service.createSchedule(prisonNumber, deadlineDate = IN_THE_FUTURE_DATE, earliestStartDate = null, prisonId = "BXI")

    // Then
    verify(planCreationScheduleRepository, never()).saveAndFlush(any())
  }

  @Test
  fun `should return deadline date based on PES Contact Date given PES Contact Date plus DEADLINE_DAYS_TO_ADD is greater than education start date plus DEADLINE_DAYS_TO_ADD`() {
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
  fun `should return deadline date based on education start date given education start date plus DEADLINE_DAYS_TO_ADD is greater than PES Contact Date plus DEADLINE_DAYS_TO_ADD`() {
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
}

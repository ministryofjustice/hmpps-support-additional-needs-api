package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class ScheduleServiceTest {

  @Mock
  private lateinit var planCreationScheduleService: PlanCreationScheduleService

  @Mock
  private lateinit var reviewScheduleService: ReviewScheduleService

  @Mock
  private lateinit var educationService: EducationService

  @Mock
  private lateinit var elspPlanRepository: ElspPlanRepository

  @Mock
  private lateinit var prisonerSearchApiClient: PrisonerSearchApiClient

  @InjectMocks
  private lateinit var scheduleService: ScheduleService

  private val prisonId = "ABC"

  @Nested
  inner class ProcessNeedChange {

    @Test
    fun `should exempt plan creation and review schedules when the person no longer has a need`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()

      // When
      scheduleService.processNeedChange(prisonNumber, hasNeed = false, prisonId = prisonId)

      // Then
      verify(planCreationScheduleService).exemptSchedule(prisonNumber, PlanCreationScheduleStatus.EXEMPT_NO_NEED, prisonId = prisonId)
      verify(reviewScheduleService).exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NO_NEED, prisonId = prisonId)
      verify(planCreationScheduleService, never()).createOrUpdateDueToNeedChange(any(), anyOrNull(), anyOrNull(), any())
      verify(reviewScheduleService, never()).createOrUpdateDueToNeedChange(any(), anyOrNull(), anyOrNull(), any())
      verifyNoInteractions(educationService, elspPlanRepository)
    }

    @Test
    fun `should do nothing when the person has a need but is not in education`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      whenever(educationService.hasActiveEducationEnrollment(prisonNumber)).thenReturn(false)

      // When
      scheduleService.processNeedChange(prisonNumber, hasNeed = true, prisonId = prisonId)

      // Then
      verify(educationService, never()).getNonPESEducationStartDate(any())
      verifyNoInteractions(planCreationScheduleService, reviewScheduleService, elspPlanRepository)
    }

    @Test
    fun `should create or update plan creation schedule when the person has a need, is in education and has no plan`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val educationStartDate = LocalDate.now().minusDays(10)
      val alnAssessmentDate = LocalDate.now().minusDays(5)
      whenever(educationService.hasActiveEducationEnrollment(prisonNumber)).thenReturn(true)
      whenever(educationService.getNonPESEducationStartDate(prisonNumber)).thenReturn(educationStartDate)
      whenever(elspPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)

      // When
      scheduleService.processNeedChange(prisonNumber, hasNeed = true, alnAssessmentDate = alnAssessmentDate, prisonId = prisonId)

      // Then
      verify(planCreationScheduleService).createOrUpdateDueToNeedChange(
        prisonNumber = prisonNumber,
        educationStartDate = educationStartDate,
        alnAssessmentDate = alnAssessmentDate,
        prisonId = prisonId,
      )
      verifyNoInteractions(reviewScheduleService)
    }

    @Test
    fun `should create or update review schedule when the person has a need, is in education and has a plan`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      val educationStartDate = LocalDate.now().minusDays(10)
      val alnAssessmentDate = LocalDate.now().minusDays(5)
      whenever(educationService.hasActiveEducationEnrollment(prisonNumber)).thenReturn(true)
      whenever(educationService.getNonPESEducationStartDate(prisonNumber)).thenReturn(educationStartDate)
      whenever(elspPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(anElspPlan(prisonNumber))

      // When
      scheduleService.processNeedChange(prisonNumber, hasNeed = true, alnAssessmentDate = alnAssessmentDate, prisonId = prisonId)

      // Then
      verify(reviewScheduleService).createOrUpdateDueToNeedChange(
        prisonNumber = prisonNumber,
        educationStartDate = educationStartDate,
        alnAssessmentDate = alnAssessmentDate,
        prisonId = prisonId,
      )
      verifyNoInteractions(planCreationScheduleService)
    }

    @Test
    fun `should pass null dates through when the course is not PES and no ALN assessment date is provided`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      whenever(educationService.hasActiveEducationEnrollment(prisonNumber)).thenReturn(true)
      // getNonPESEducationStartDate returning null indicates the course is non PES
      whenever(educationService.getNonPESEducationStartDate(prisonNumber)).thenReturn(null)
      whenever(elspPlanRepository.findByPrisonNumber(prisonNumber)).thenReturn(null)

      // When
      scheduleService.processNeedChange(prisonNumber, hasNeed = true, prisonId = prisonId)

      // Then
      verify(planCreationScheduleService).createOrUpdateDueToNeedChange(
        prisonNumber = prisonNumber,
        educationStartDate = null,
        alnAssessmentDate = null,
        prisonId = prisonId,
      )
      verifyNoInteractions(reviewScheduleService)
    }
  }

  private fun anElspPlan(prisonNumber: String) = ElspPlanEntity(
    prisonNumber = prisonNumber,
    individualSupport = "Some support required",
    createdAtPrison = prisonId,
    updatedAtPrison = prisonId,
  )
}

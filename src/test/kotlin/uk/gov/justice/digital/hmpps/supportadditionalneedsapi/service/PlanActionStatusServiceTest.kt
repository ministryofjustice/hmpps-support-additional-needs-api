package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.eq
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PrisonerOverviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PrisonerOverviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleExemptionReason
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class PlanActionStatusServiceTest {

  @InjectMocks
  private lateinit var service: PlanActionStatusService

  @Mock
  private lateinit var searchService: SearchService

  @Mock
  private lateinit var prisonerOverviewRepository: PrisonerOverviewRepository

  @Mock
  private lateinit var planCreationScheduleRepository: PlanCreationScheduleRepository

  @Test
  fun `should return action status with deadlines and mapped exemption reason`() {
    // Given

    val planCreationDeadline = LocalDate.now().minusMonths(1)
    val reviewDeadline = LocalDate.now().plusMonths(1)
    val prisonNumber = randomValidPrisonNumber()
    val prisonerOverview = PrisonerOverviewEntity(
      deadlineDate = reviewDeadline,
      reviewDeadlineDate = reviewDeadline,
      planCreationDeadlineDate = planCreationDeadline,
      prisonNumber = "BXI",
      hasAlnNeed = false,
      hasLddNeed = false,
    )
    whenever(prisonerOverviewRepository.findByPrisonNumber(prisonNumber)).thenReturn(prisonerOverview)

    val exemptionReasonString = "EXEMPT_NOT_REQUIRED"
    val exemptionDetail = "about to be released"

    val status = PlanStatus.PLAN_DUE
    given(searchService.determinePlanStatus(prisonerOverview)).willReturn(status)

    val planCreationSchedule = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.SCHEDULED,
      deadlineDate = planCreationDeadline,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = LocalDate.now().minusDays(10),
      exemptionReason = exemptionReasonString,
      exemptionDetail = exemptionDetail,
    )

    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(planCreationSchedule)

    // When
    val result: PlanActionStatus = service.getPlanActionStatus(prisonNumber)

    // Then
    assertThat(result.status).isEqualTo(status)
    assertThat(result.planCreationDeadlineDate).isEqualTo(planCreationDeadline)
    assertThat(result.reviewDeadlineDate).isEqualTo(reviewDeadline)
    assertThat(result.exemptionDetail).isEqualTo(exemptionDetail)
    assertThat(result.exemptionReason).isEqualTo(PlanCreationScheduleExemptionReason.forValue(exemptionReasonString))
    then(searchService).should().determinePlanStatus(eq(prisonerOverview))
    verifyNoMoreInteractions(searchService)
  }
}

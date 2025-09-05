package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import org.mockito.kotlin.then
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.manageusers.UserDetailsDto
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PrisonerOverviewEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PrisonerOverviewRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.InstantMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanActionStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationScheduleExemptionReason
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

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

  @Mock
  private lateinit var userService: ManageUserService

  @Mock
  private lateinit var instantMapper: InstantMapper

  @Test
  fun `should return action status with deadlines`() {
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

    val status = PlanStatus.PLAN_DUE
    given(searchService.determinePlanStatus(prisonerOverview)).willReturn(status)

    // When
    val result: PlanActionStatus = service.getPlanActionStatus(prisonNumber)

    // Then
    assertThat(result.status).isEqualTo(status)
    assertThat(result.planCreationDeadlineDate).isEqualTo(planCreationDeadline)
    assertThat(result.reviewDeadlineDate).isEqualTo(reviewDeadline)
    assertThat(result.exemptionDetail).isNull()
    assertThat(result.exemptionReason).isNull()
    assertThat(result.exemptionRecordedBy).isNull()
    assertThat(result.exemptionRecordedAt).isNull()
    then(searchService).should().determinePlanStatus(prisonerOverview)
    verifyNoMoreInteractions(searchService)
    verifyNoMoreInteractions(userService)
    verifyNoMoreInteractions(instantMapper)
  }

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
      planDeclined = true,
    )
    whenever(prisonerOverviewRepository.findByPrisonNumber(prisonNumber)).thenReturn(prisonerOverview)

    val exemptionReasonString = "EXEMPT_NOT_REQUIRED"
    val exemptionDetail = "about to be released"

    val status = PlanStatus.PLAN_DECLINED
    given(searchService.determinePlanStatus(prisonerOverview)).willReturn(status)

    val username = "ASMITH_GEN"
    val expectedUserDisplayName = "Alex Smith"
    given(userService.getUserDetails(username)).willReturn(UserDetailsDto(username, true, expectedUserDisplayName))

    val planCreatedUpdatedAt = Instant.now()
    val planCreationSchedule = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY,
      deadlineDate = planCreationDeadline,
      createdAtPrison = "BXI",
      updatedAtPrison = "BXI",
      earliestStartDate = LocalDate.now().minusDays(10),
      exemptionReason = exemptionReasonString,
      exemptionDetail = exemptionDetail,
      updatedBy = username,
      updatedAt = planCreatedUpdatedAt,
    )

    val expectedExemptionRecordedAt = OffsetDateTime.now()
    given(instantMapper.toOffsetDateTime(planCreatedUpdatedAt)).willReturn(expectedExemptionRecordedAt)

    given(planCreationScheduleRepository.findByPrisonNumber(prisonNumber)).willReturn(planCreationSchedule)

    // When
    val result: PlanActionStatus = service.getPlanActionStatus(prisonNumber)

    // Then
    assertThat(result.status).isEqualTo(status)
    assertThat(result.planCreationDeadlineDate).isEqualTo(planCreationDeadline)
    assertThat(result.reviewDeadlineDate).isEqualTo(reviewDeadline)
    assertThat(result.exemptionDetail).isEqualTo(exemptionDetail)
    assertThat(result.exemptionReason).isEqualTo(PlanCreationScheduleExemptionReason.forValue(exemptionReasonString))
    assertThat(result.exemptionRecordedBy).isEqualTo(expectedUserDisplayName)
    assertThat(result.exemptionRecordedAt).isEqualTo(expectedExemptionRecordedAt)
    then(searchService).should().determinePlanStatus(prisonerOverview)
    then(userService).should().getUserDetails(username)
    then(instantMapper).should().toOffsetDateTime(planCreatedUpdatedAt)
    verifyNoMoreInteractions(searchService)
    verifyNoMoreInteractions(userService)
    verifyNoMoreInteractions(instantMapper)
  }
}

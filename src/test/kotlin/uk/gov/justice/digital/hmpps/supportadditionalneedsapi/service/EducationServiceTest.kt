package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions
import org.mockito.kotlin.whenever
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.Education
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.EducationDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EducationEnrolmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationStatusUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

class EducationServiceTest {

  private val curiousApiClient: CuriousApiClient = mock()
  private val prisonerSearchApiClient: PrisonerSearchApiClient = mock()
  private val educationEnrolmentRepository: EducationEnrolmentRepository = mock()
  private val needService: NeedService = mock()
  private val reviewScheduleService: ReviewScheduleService = mock()
  private val planCreationScheduleService: PlanCreationScheduleService = mock()
  private val elspPlanRepository: ElspPlanRepository = mock()
  private val alnAssessmentRepository: AlnAssessmentRepository = mock()

  private val fixedTimestamp = Instant.parse("2026-04-17T09:13:22.123Z")
  private val clock = Clock.fixed(fixedTimestamp, ZoneId.of("UTC"))

  private val educationService = EducationService(
    curiousApiClient = curiousApiClient,
    prisonerSearchApiClient = prisonerSearchApiClient,
    educationEnrolmentRepository = educationEnrolmentRepository,
    needService = needService,
    reviewScheduleService = reviewScheduleService,
    planCreationScheduleService = planCreationScheduleService,
    elspPlanRepository = elspPlanRepository,
    alnAssessmentRepository = alnAssessmentRepository,
    clock = clock,
  )

  private val today = LocalDate.now(clock)

  @Nested
  inner class ProcessEducationStatusUpdate {

    private lateinit var prisonNumber: String
    private lateinit var prisoner: Prisoner
    private lateinit var educationStatusUpdateAdditionalInformation: EducationStatusUpdateAdditionalInformation

    @BeforeEach
    fun beforeEach() {
      prisonNumber = randomValidPrisonNumber()
      prisoner = aValidPrisoner(
        prisonerNumber = prisonNumber,
        prisonId = "BXI",
      )
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(prisoner)

      educationStatusUpdateAdditionalInformation = EducationStatusUpdateAdditionalInformation(
        curiousExternalReference = UUID.randomUUID(),
      )

      doAnswer { it.getArgument<EducationEnrolmentEntity>(0) }.whenever(educationEnrolmentRepository).save(any())
    }

    @Nested
    inner class EducationStopHandling {
      private lateinit var inboundEvent: InboundEvent

      @BeforeEach
      fun beforeEach() {
        inboundEvent = educationStatusUpdateInboundEvent(prisonNumber, "EDUCATION_STOPPED")
      }

      @Test
      fun `should process education-stop given a prisoner has active education in another prison (Curious processing transfer event and sending education-sop message)`() {
        // Given
        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "TSI", "English", today.minusYears(1), "PES"),
            anEducationEnrolment(prisonNumber, "TSI", "Maths", today.minusYears(1), "PES", endDate = today.minusMonths(3)),
            anEducationEnrolment(prisonNumber, "TSI", "Woodwork", today.minusMonths(9), "DPS"),
          ),
        )

        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "TSI",
              courseCode = "English",
              startDate = today.minusYears(1),
            ),
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "TSI",
              courseCode = "Maths",
              startDate = today.minusYears(1),
              endDate = today.minusMonths(3),
            ),
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "TSI",
              courseCode = "Woodwork",
              startDate = today.minusMonths(9),
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // When
        educationService.processEducationStatusUpdate(prisonNumber, educationStatusUpdateAdditionalInformation, inboundEvent)

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verifyNoInteractions(needService)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository, times(2)).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository - assert they have all been ended today
        val captor = argumentCaptor<List<EducationEnrolmentEntity>>()
        verify(educationEnrolmentRepository).saveAll(captor.capture())
        with(captor.singleValue) {
          with(this[0]) {
            assertThat(prisonNumber).isEqualTo(prisonNumber)
            assertThat(qualificationCode).isEqualTo("English")
            assertThat(fundingType).isEqualTo("PES")
            assertThat(learningStartDate).isEqualTo(today.minusYears(1))
            assertThat(endDate).isEqualTo(today)
          }
          with(this[1]) {
            assertThat(prisonNumber).isEqualTo(prisonNumber)
            assertThat(qualificationCode).isEqualTo("Maths")
            assertThat(fundingType).isEqualTo("PES")
            assertThat(learningStartDate).isEqualTo(today.minusYears(1))
            assertThat(endDate).isEqualTo(today)
          }
          with(this[2]) {
            assertThat(prisonNumber).isEqualTo(prisonNumber)
            assertThat(qualificationCode).isEqualTo("Woodwork")
            assertThat(fundingType).isEqualTo("DPS")
            assertThat(learningStartDate).isEqualTo(today.minusMonths(9))
            assertThat(endDate).isEqualTo(today)
          }
        }
      }

      @Test
      fun `should process education-stop given a prisoner stops a non-PES course`() {
        // Given
        given(needService.hasNeed(any())).willReturn(false)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf())

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "BXI", "Woodwork", today.minusMonths(9), "DPS"),
          ),
        )

        val educationDto = EducationDTO(
          educationData = listOf(
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Woodwork",
              startDate = today.minusMonths(9),
              endDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // When
        educationService.processEducationStatusUpdate(prisonNumber, educationStatusUpdateAdditionalInformation, inboundEvent)

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository, times(2)).findAllByPrisonNumber(prisonNumber)

        // assert any schedules were exempted
        verify(planCreationScheduleService)
          .exemptSchedule(prisonNumber = prisonNumber, status = PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION, prisonId = "BXI")
        verify(reviewScheduleService)
          .exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION, "BXI")

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Woodwork")
          assertThat(fundingType).isEqualTo("DPS")
          assertThat(learningStartDate).isEqualTo(today.minusMonths(9))
          assertThat(endDate).isEqualTo(today)
        }
      }

      @Test
      fun `should process education-stop given a prisoner stops a PES course`() {
        // Given
        given(needService.hasNeed(any())).willReturn(false)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf())

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "BXI", "Spanish", today.minusMonths(9), "PES"),
          ),
        )

        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Spanish",
              startDate = today.minusMonths(9),
              endDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // When
        educationService.processEducationStatusUpdate(prisonNumber, educationStatusUpdateAdditionalInformation, inboundEvent)

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository, times(2)).findAllByPrisonNumber(prisonNumber)

        // assert any schedules were exempted
        verify(planCreationScheduleService)
          .exemptSchedule(prisonNumber = prisonNumber, status = PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION, prisonId = "BXI")
        verify(reviewScheduleService)
          .exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION, "BXI")

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Spanish")
          assertThat(fundingType).isEqualTo("PES")
          assertThat(learningStartDate).isEqualTo(today.minusMonths(9))
          assertThat(endDate).isEqualTo(today)
        }
      }

      @Test
      fun `should process education-stop given a prisoner stops a PES course but is still on another PES course`() {
        // Given
        given(needService.hasNeed(any())).willReturn(false)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf())

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "BXI", "Spanish", today.minusMonths(9), "PES"),
            anEducationEnrolment(prisonNumber, "BXI", "French", today.minusMonths(6), "PES"),
          ),
        )

        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Spanish",
              startDate = today.minusMonths(9),
              endDate = today,
            ),
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "French",
              startDate = today.minusMonths(6),
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // When
        educationService.processEducationStatusUpdate(prisonNumber, educationStatusUpdateAdditionalInformation, inboundEvent)

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository, times(2)).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Spanish")
          assertThat(fundingType).isEqualTo("PES")
          assertThat(learningStartDate).isEqualTo(today.minusMonths(9))
          assertThat(endDate).isEqualTo(today)
        }
      }
    }

    @Nested
    inner class EducationStartHandling {
      private lateinit var inboundEvent: InboundEvent

      @BeforeEach
      fun beforeEach() {
        inboundEvent = educationStatusUpdateInboundEvent(prisonNumber, "EDUCATION_STARTED")
      }

      @Test
      fun `AC5 should process education-start given a prisoner without a plan and with needs is starting a PES course and has no existing course enrolments`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(null)

        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(emptyList())

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to the planCreationScheduleService with isPesCourse being true
        verify(planCreationScheduleService).createOrUpdateDueToEducationUpdate(
          prisonNumber = prisonNumber,
          startDate = today,
          isPesCourse = true,
          subjectToKPIRules = true,
          prisonId = "BXI",
        )

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("PES")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      /*
      This test is wrong currently.  If the person isn't on any PES courses and doesn't have a plan, then enroling on a non-PES course should _NOT_ trigger
      a plan creation schedule to be set up.
      However currently the code will create or update a plan creation schedule with deadline 2099 on receiving a non-PES course enrolment.
       */
      @Test
      fun `AC1 Given a prisoner without a plan and with needs is starting a non-PES course and has no existing course enrolments, should store the new non_PES course enrolment and not make any schedule updates`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(null)

        val educationDto = EducationDTO(
          educationData = listOf(
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(emptyList())

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert any schedules were exempted
        verify(planCreationScheduleService)
          .exemptSchedule(
            prisonNumber = prisonNumber,
            status = PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
            prisonId = "BXI"
          )
        verify(reviewScheduleService)
          .exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION, "BXI")

        // assert a call to the planCreationScheduleService with isPesCourse being false
        verify(planCreationScheduleService).createOrUpdateDueToEducationUpdate(
          prisonNumber = prisonNumber,
          startDate = today,
          isPesCourse = false,
          subjectToKPIRules = true,
          prisonId = "BXI",
        )

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("DPS")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `AC2 should process education-start given a prisoner with a plan and needs is starting a new PES course`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(anELSP(prisonNumber))

        // Given the prisoner already has a plan they by definition have an existing PES course
        //TODO: They could have a non-PES course, or a completed PES course...
        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "English",
              startDate = today.minusWeeks(1),
            ),
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // Given the prisoner already has a plan they by definition must be on a course already
        //TODO: The course could have finished? And/or be non-PES.
        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "BXI", "English", today.minusWeeks(1), "PES"),
          ),
        )

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert it did update the review schedule
        verify(reviewScheduleService).createOrUpdateDueToEducationUpdate(
          prisonNumber = prisonNumber,
          startDate = today,
          prisonId = "BXI",
        )

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("PES")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `AC1 Given a prisoner with a plan and needs and already on a PES course is starting a non-PES course, should store the new non-PES course enrolment and not make any schedule changes`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(anELSP(prisonNumber))

        val educationDto = EducationDTO(
          educationData = listOf(
            // Existing PES course
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "English",
              startDate = today.minusWeeks(1),
            ),
            // New non-PES course
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        // The prisoner is already on a PES course, so they have an existing course enrolment
        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(prisonNumber, "BXI", "English", today.minusWeeks(1), "PES"),
          ),
        )

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("DPS")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `AC6 should process education-start given a prisoner without needs is starting a PES course and has no existing course enrolments`() {
        // Given
        given(needService.hasNeed(any())).willReturn(false)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf())

        val educationDto = EducationDTO(
          educationData = listOf(
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(emptyList())

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("PES")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `AC1 Given a prisoner without needs is starting a non-PES course and has no existing course enrolments, should store the new non-PES course enrolment and not make any schedule updates`() {
        // Given
        given(needService.hasNeed(any())).willReturn(false)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf())

        val educationDto = EducationDTO(
          educationData = listOf(
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(emptyList())

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verifyNoInteractions(elspPlanRepository)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert any schedules were exempted
        verify(planCreationScheduleService)
          .exemptSchedule(
            prisonNumber = prisonNumber,
            status = PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
            prisonId = "BXI"
          )
        verify(reviewScheduleService)
          .exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION, "BXI")

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("DPS")
          assertThat(learningStartDate).isEqualTo(today)
          assertThat(endDate).isNull()
        }
      }

      @Test
      fun `AC1 Given a prisoner with an inactive plan and with needs is starting a non-PES course and has no current PES enrolments, should store the new non-PES course enrolment and not make any schedule updates`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(anELSP(prisonNumber))

        val educationDto = EducationDTO(
          educationData = listOf(
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "English",
              startDate = today.minusWeeks(1),
              endDate = today.minusDays(1),
            )
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(
              prisonNumber,
              "BXI",
              "English",
              today.minusWeeks(1),
              "PES",
              endDate = today.minusDays(1)
            ),
          ),
        )

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // we will call exemptSchedule() even though the schedule(s) will already been exempted when the previous . The exemptSchedule() function handles
        // not re-exempting the schedule if it is already exempted due to not being in PES education.
        verify(planCreationScheduleService)
          .exemptSchedule(
            prisonNumber = prisonNumber,
            status = PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
            prisonId = "BXI"
          )
        verify(reviewScheduleService)
          .exemptSchedule(prisonNumber, ReviewScheduleStatus.EXEMPT_NOT_IN_EDUCATION, "BXI")

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("DPS")
        }
      }

      /*
      This test currently fails, as the code updates the plan creation schedule when a non-PES course is started, and sets the
      deadline to 2099, instead of leaving it as it is with a deadline of PES start date + 5 days.
       */
      @Test
      fun `AC1 Given a prisoner with no plan yet but with needs and a current PES enrolment is starting a non-PES course, should store the new non-PES course enrolment and not make any schedule updates`() {
        // Given
        given(needService.hasNeed(any())).willReturn(true)
        given(needService.getNeedSources(any())).willReturn(sortedSetOf(NeedSource.ALN_SCREENER))
        given(elspPlanRepository.findByPrisonNumber(any())).willReturn(null)

        val educationDto = EducationDTO(
          educationData = listOf(
            aNonPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "Maths",
              startDate = today,
            ),
            aPesCourse(
              prisonNumber = prisonNumber,
              prisonId = "BXI",
              courseCode = "English",
              startDate = today.minusWeeks(1),
            )
          ),
        )
        given(curiousApiClient.getEducation(any())).willReturn(educationDto)

        given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
          listOf(
            anEducationEnrolment(
              prisonNumber,
              "BXI",
              "English",
              today.minusWeeks(1),
              "PES"
            ),
          ),
        )

        // When
        educationService.processEducationStatusUpdate(
          prisonNumber,
          educationStatusUpdateAdditionalInformation,
          inboundEvent
        )

        // Then
        verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
        verify(needService).hasNeed(prisonNumber)
        verify(elspPlanRepository).findByPrisonNumber(prisonNumber)
        verify(curiousApiClient).getEducation(prisonNumber)
        verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

        // assert it did not exempt any schedules
        verify(planCreationScheduleService, never())
          .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
        verify(reviewScheduleService, never())
          .exemptSchedule(any(), any(), any())

        // assert a call to the planCreationScheduleService was not made
        verify(planCreationScheduleService, never())
          .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

        // assert a call to create or update the review schedule was not made
        verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

        // assert the records written/updated to the EducationEnrolmentRepository
        val captor = argumentCaptor<EducationEnrolmentEntity>()
        verify(educationEnrolmentRepository).save(captor.capture())
        with(captor.singleValue) {
          assertThat(prisonNumber).isEqualTo(prisonNumber)
          assertThat(qualificationCode).isEqualTo("Maths")
          assertThat(fundingType).isEqualTo("DPS")
        }
      }

        @Test
        fun `should process education-start given no changes were made to the course enrolments`() {
          // Given

          val educationDto = EducationDTO(
            educationData = listOf(
              aPesCourse(
                prisonNumber = prisonNumber,
                prisonId = "BXI",
                courseCode = "Maths",
                startDate = today.minusWeeks(1),
              ),
            ),
          )
          given(curiousApiClient.getEducation(any())).willReturn(educationDto)

          given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
            listOf(
              anEducationEnrolment(prisonNumber, "BXI", "Maths", today.minusWeeks(1), "PES"),
            ),
          )

          // When
          educationService.processEducationStatusUpdate(
            prisonNumber,
            educationStatusUpdateAdditionalInformation,
            inboundEvent
          )

          // Then
          verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
          verifyNoInteractions(needService)
          verifyNoInteractions(elspPlanRepository)
          verify(curiousApiClient).getEducation(prisonNumber)
          verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

          // assert it did not exempt any schedules
          verify(planCreationScheduleService, never())
            .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
          verify(reviewScheduleService, never())
            .exemptSchedule(any(), any(), any())

          // assert a call to the planCreationScheduleService was not made
          verify(planCreationScheduleService, never())
            .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

          // assert a call to create or update the review schedule was not made
          verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

          // assert nothing was written/updated to the EducationEnrolmentRepository
          verifyNoMoreInteractions(educationEnrolmentRepository)
        }

        @Test
        fun `should process education-start given a previously ended PES course is restarted`() {
          // Given
          given(needService.hasNeed(any())).willReturn(false)
          given(needService.getNeedSources(any())).willReturn(sortedSetOf())

          given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
            listOf(
              anEducationEnrolment(
                prisonNumber,
                "BXI",
                "Maths",
                today.minusWeeks(2),
                "PES",
                endDate = today.minusWeeks(1)
              ),
            ),
          )

          val educationDto = EducationDTO(
            educationData = listOf(
              aPesCourse(
                prisonNumber = prisonNumber,
                prisonId = "BXI",
                courseCode = "Maths",
                // even though Curious is telling us the course is restarting today, they do not change their startDate field. This is CRUCIAL for us to find the right course in our DB because the composite key uses this field
                startDate = today.minusWeeks(2),
                endDate = null,
              ),
            ),
          )
          given(curiousApiClient.getEducation(any())).willReturn(educationDto)

          // When
          educationService.processEducationStatusUpdate(
            prisonNumber,
            educationStatusUpdateAdditionalInformation,
            inboundEvent
          )

          // Then
          verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
          verify(needService).hasNeed(prisonNumber)
          verifyNoInteractions(elspPlanRepository)
          verify(curiousApiClient).getEducation(prisonNumber)
          verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

          // assert it did not exempt any schedules
          verify(planCreationScheduleService, never())
            .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
          verify(reviewScheduleService, never())
            .exemptSchedule(any(), any(), any())

          // assert a call to the planCreationScheduleService was not made
          verify(planCreationScheduleService, never())
            .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

          // assert a call to create or update the review schedule was not made
          verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(any(), any(), any())

          // assert the records written/updated to the EducationEnrolmentRepository
          val captor = argumentCaptor<EducationEnrolmentEntity>()
          verify(educationEnrolmentRepository).save(captor.capture())
          with(captor.singleValue) {
            assertThat(prisonNumber).isEqualTo(prisonNumber)
            assertThat(qualificationCode).isEqualTo("Maths")
            assertThat(fundingType).isEqualTo("PES")
            assertThat(learningStartDate).isEqualTo(today.minusWeeks(2)) // When re-starting a course we do not update the start date; it is deliberately immutable in the Entity class
            assertThat(endDate).isNull()
          }
        }

        @Test
        fun `AC3 should process education-start given a prisoner with no needs and with plan is starting a new PES course`() {
          // Expect to record the new course in the education table and not change anything in scheduling because they have no needs.

          // Given
          given(needService.hasNeed(any())).willReturn(false)
          given(needService.getNeedSources(any())).willReturn(sortedSetOf())
          given(elspPlanRepository.findByPrisonNumber(any())).willReturn(anELSP(prisonNumber))

          val educationDto = EducationDTO(
            educationData = listOf(
              aPesCourse(
                prisonNumber = prisonNumber,
                prisonId = "BXI",
                courseCode = "English",
                startDate = today.minusWeeks(1),
              ),
              aPesCourse(
                prisonNumber = prisonNumber,
                prisonId = "BXI",
                courseCode = "Maths",
                startDate = today,
              ),
            ),
          )
          given(curiousApiClient.getEducation(any())).willReturn(educationDto)

          given(educationEnrolmentRepository.findAllByPrisonNumber(any())).willReturn(
            listOf(
              anEducationEnrolment(prisonNumber, "BXI", "English", today.minusWeeks(1), "PES"),
            ),
          )

          // When
          educationService.processEducationStatusUpdate(
            prisonNumber,
            educationStatusUpdateAdditionalInformation,
            inboundEvent
          )

          // Then
          verify(prisonerSearchApiClient).getPrisoner(prisonNumber)
          verify(needService).hasNeed(prisonNumber)
          verify(curiousApiClient).getEducation(prisonNumber)
          verify(educationEnrolmentRepository).findAllByPrisonNumber(prisonNumber)

          // assert it did not exempt any schedules
          verify(planCreationScheduleService, never())
            .exemptSchedule(any(), any(), anyOrNull(), anyOrNull(), any(), any())
          verify(reviewScheduleService, never())
            .exemptSchedule(any(), any(), any())

          // assert a call to the reviewScheduleService was not made
          verify(reviewScheduleService, never()).createOrUpdateDueToEducationUpdate(
            any(), any(), any()
          )

          // assert a call to the planCreationScheduleService was not made
          verify(planCreationScheduleService, never())
            .createOrUpdateDueToEducationUpdate(any(), any(), any(), any(), any())

          // assert the records written/updated to the EducationEnrolmentRepository
          val captor = argumentCaptor<EducationEnrolmentEntity>()
          verify(educationEnrolmentRepository).save(captor.capture())
          with(captor.singleValue) {
            assertThat(prisonNumber).isEqualTo(prisonNumber)
            assertThat(qualificationCode).isEqualTo("Maths")
            assertThat(fundingType).isEqualTo("PES")
            assertThat(learningStartDate).isEqualTo(today)
            assertThat(endDate).isNull()
          }
        }
      }

  }

  private fun educationStatusUpdateInboundEvent(prisonNumber: String, subEventType: String) = mock<InboundEvent>().apply {
    given(this.prisonNumber()).willReturn(prisonNumber)
    given(this.eventType).willReturn(EventType.EDUCATION_STATUS_UPDATE)
    given(this.description).willReturn(subEventType)
  }

  private fun aPesCourse(prisonNumber: String, prisonId: String, courseCode: String, startDate: LocalDate, endDate: LocalDate? = null) = aCourse(
    prisonNumber,
    courseCode,
    prisonId,
    "PES",
    startDate,
    endDate,
  )

  private fun aNonPesCourse(prisonNumber: String, prisonId: String, courseCode: String, startDate: LocalDate, endDate: LocalDate? = null) = aCourse(
    prisonNumber,
    courseCode,
    prisonId,
    "DPS",
    startDate,
    endDate,
  )

  private fun aCourse(prisonNumber: String, courseCode: String, prisonId: String, fundingType: String, startDate: LocalDate, endDate: LocalDate? = null) = Education(
    fundingType = fundingType,
    prn = prisonNumber,
    qualificationCode = courseCode,
    learningStartDate = startDate,
    learningActualEndDate = endDate,
    establishmentId = prisonId,
  )

  private fun anELSP(prisonNumber: String) = ElspPlanEntity(
    prisonNumber = prisonNumber,
    individualSupport = "Some support required",
    createdAtPrison = "BXI",
    updatedAtPrison = "BXI",
  )

  private fun anEducationEnrolment(prisonNumber: String, prisonId: String, courseCode: String, startDate: LocalDate, fundingType: String, endDate: LocalDate? = null) = EducationEnrolmentEntity(
    prisonNumber = prisonNumber,
    establishmentId = prisonId,
    qualificationCode = courseCode,
    learningStartDate = startDate,
    fundingType = fundingType,
    endDate = endDate,
  )
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import com.fasterxml.jackson.databind.node.JsonNodeFactory
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.NullAndEmptySource
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.ALNAssessment
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.ALNAssessmentDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.ALNData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.Assessments
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.DEFAULT_PRISON_ID
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.STRENGTH
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ALNScreenerNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ALNScreenerMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.AdditionalInformation.EducationALNAssessmentUpdateAdditionalInformation
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.EventType
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.Identifier
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.InboundEvent
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.messaging.PersonReference
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ALNScreenerServiceTest {

  @Mock
  private lateinit var challengeService: ChallengeService

  @Mock
  private lateinit var strengthService: StrengthService

  @Mock
  private lateinit var alnScreenerRepository: AlnScreenerRepository

  @Mock
  private lateinit var curiousApiClient: CuriousApiClient

  @Mock
  private lateinit var needService: NeedService

  @Mock
  private lateinit var alnScreenerMapper: ALNScreenerMapper

  @Mock
  private lateinit var scheduleService: ScheduleService

  @Mock
  private lateinit var dataDeletionEventService: DataDeletionEventService

  @InjectMocks
  private lateinit var service: ALNScreenerService

  /*
   * TODO - write unit tests for public methods:
   *  * createScreener
   *  * getScreeners
   */

  @Nested
  inner class DeleteCurrentScreener {
    val prisonNumber = "A1234AB"
    val prisonId = "AAA"
    val reason = DeletionReason.ENTERED_IN_ERROR

    val currentAlnScreener = ALNScreenerEntity(
      prisonNumber = prisonNumber,
      screeningDate = LocalDate.now(),
      createdAtPrison = prisonId,
      updatedAtPrison = prisonId,
    )

    @Test
    fun `should delete current Screener and re-enable the previous ALN Screener given there is a previous ALN Screener`() {
      // Given
      // Previous screener exists with archived strengths and challenges
      val previousAlnScreener = ALNScreenerEntity(
        prisonNumber = prisonNumber,
        screeningDate = LocalDate.now().minusDays(1),
        createdAtPrison = prisonId,
        updatedAtPrison = prisonId,
      ).apply {
        strengths.add(
          StrengthEntity(
            prisonNumber = prisonNumber,
            strengthType = ReferenceDataEntity(
              key = ReferenceDataKey(domain = STRENGTH, code = "READING"),
              description = "Reading",
            ),
            createdAtPrison = prisonId,
            updatedAtPrison = prisonId,
            active = false,
          ),
        )
        challenges.add(
          ChallengeEntity(
            prisonNumber = prisonNumber,
            challengeType = ReferenceDataEntity(
              key = ReferenceDataKey(domain = STRENGTH, code = "READING"),
              description = "Reading",
            ),
            createdAtPrison = prisonId,
            updatedAtPrison = prisonId,
            active = false,
          ),
        )
      }

      given(alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(any()))
        .willReturn(currentAlnScreener, previousAlnScreener)

      // When
      service.deleteCurrentScreener(prisonNumber, prisonId, reason)

      // Then
      assertThat(previousAlnScreener.strengths).allSatisfy { it.active }
      assertThat(previousAlnScreener.challenges).allSatisfy { it.active }
      verify(alnScreenerRepository, times(2)).findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
      verify(alnScreenerRepository).delete(currentAlnScreener)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should delete current Screener given there is no previous ALN Screener`() {
      // Given
      given(alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(any()))
        .willReturn(currentAlnScreener)

      // When
      service.deleteCurrentScreener(prisonNumber, prisonId, reason)

      // Then
      verify(alnScreenerRepository, times(2)).findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
      verify(alnScreenerRepository).delete(currentAlnScreener)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should not delete ALN Screener given ALN Screener does not exist`() {
      // Given
      given(alnScreenerRepository.findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(ALNScreenerNotFoundException::class.java) {
        service.deleteCurrentScreener(prisonNumber, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      verify(alnScreenerRepository).findFirstByPrisonNumberOrderByScreeningDateDescCreatedAtDesc(prisonNumber)
      verify(alnScreenerRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }
  }

  @Nested
  inner class ProcessALNAssessmentUpdate {
    val prisonNumber = "A1234AA"
    val curiousReference: UUID = UUID.randomUUID()
    val info = EducationALNAssessmentUpdateAdditionalInformation(curiousExternalReference = curiousReference)
    val inboundEvent = InboundEvent(
      eventType = EventType.EDUCATION_ALN_ASSESSMENT_UPDATE,
      personReference = PersonReference(listOf(Identifier("NOMS", prisonNumber))),
      additionalInformation = JsonNodeFactory.instance.objectNode(),
      occurredAt = "2026-10-20T12:00:00Z",
      description = "ALN assessment updated",
      detailUrl = null,
      version = "1",
    )
    val assessmentDate: LocalDate = LocalDate.of(2026, 10, 1)

    @Test
    fun `should record ALN need and process need change given outcome is YES and overall need changes to true`() {
      // Given
      given(needService.hasNeed(any())).willReturn(false, true)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(curiousResponse(anAlnAssessment(assessmentDate = assessmentDate, outcome = "YES", establishmentId = "AAA")))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService, times(2)).hasNeed(prisonNumber)
      verify(curiousApiClient).getALNAssessment(prisonNumber)
      verify(needService).recordAlnScreenerNeed(prisonNumber, true, curiousReference, assessmentDate)
      verify(scheduleService).processNeedChange(prisonNumber, true, assessmentDate, "AAA")
    }

    @Test
    fun `should record no ALN need and process need change without assessment date given outcome is NO and overall need changes to false`() {
      // Given
      given(needService.hasNeed(any())).willReturn(true, false)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(curiousResponse(anAlnAssessment(assessmentDate = assessmentDate, outcome = "NO", establishmentId = "AAA")))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService).recordAlnScreenerNeed(prisonNumber, false, curiousReference, assessmentDate)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, "AAA")
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `should record ALN need but not process need change given overall need is unchanged`(overallNeed: Boolean) {
      // Given
      given(needService.hasNeed(any())).willReturn(overallNeed, overallNeed)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(curiousResponse(anAlnAssessment(assessmentDate = assessmentDate, outcome = "YES", establishmentId = "AAA")))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService, times(2)).hasNeed(prisonNumber)
      verify(needService).recordAlnScreenerNeed(prisonNumber, true, curiousReference, assessmentDate)
      verifyNoInteractions(scheduleService)
    }

    @Test
    fun `should treat assessment outcome case-insensitively`() {
      // Given
      given(needService.hasNeed(any())).willReturn(false, true)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(curiousResponse(anAlnAssessment(assessmentDate = assessmentDate, outcome = "yes", establishmentId = "AAA")))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService).recordAlnScreenerNeed(prisonNumber, true, curiousReference, assessmentDate)
      verify(scheduleService).processNeedChange(prisonNumber, true, assessmentDate, "AAA")
    }

    @Test
    fun `should use the assessment with the latest assessment date`() {
      // Given
      val laterDate = assessmentDate.plusDays(10)

      given(needService.hasNeed(any())).willReturn(false, true)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(
          curiousResponse(
            anAlnAssessment(assessmentDate = assessmentDate, outcome = "NO", establishmentId = "BBB"),
            anAlnAssessment(assessmentDate = laterDate, outcome = "YES", establishmentId = "AAA"),
            anAlnAssessment(assessmentDate = assessmentDate.minusDays(10), outcome = "NO", establishmentId = "CCC"),
          ),
        )

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService).recordAlnScreenerNeed(prisonNumber, true, curiousReference, laterDate)
      verify(scheduleService).processNeedChange(prisonNumber, true, laterDate, "AAA")
    }

    @Test
    fun `should use the assessment with the latest modified date given multiple assessments on the same date`() {
      // Given
      val modified = assessmentDate.atTime(9, 0)
      given(needService.hasNeed(any())).willReturn(true, false)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(
          curiousResponse(
            anAlnAssessment(assessmentDate = assessmentDate, outcome = "YES", establishmentId = "BBB", modifiedDate = modified),
            anAlnAssessment(assessmentDate = assessmentDate, outcome = "NO", establishmentId = "AAA", modifiedDate = modified.plusHours(2)),
            anAlnAssessment(assessmentDate = assessmentDate, outcome = "YES", establishmentId = "CCC", modifiedDate = null),
          ),
        )

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(needService).recordAlnScreenerNeed(prisonNumber, false, curiousReference, assessmentDate)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, "AAA")
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = ["AA", "AAAA"])
    fun `should use default prison id given assessment establishment id is not a valid prison id`(establishmentId: String?) {
      // Given
      given(needService.hasNeed(any())).willReturn(false, true)
      given(curiousApiClient.getALNAssessment(any()))
        .willReturn(curiousResponse(anAlnAssessment(assessmentDate = assessmentDate, outcome = "YES", establishmentId = establishmentId)))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(scheduleService).processNeedChange(prisonNumber, true, assessmentDate, DEFAULT_PRISON_ID)
    }

    @Test
    fun `should not record ALN need given Curious returns no ALN assessments`() {
      // Given
      given(curiousApiClient.getALNAssessment(any())).willReturn(curiousResponse())

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(curiousApiClient).getALNAssessment(prisonNumber)
      verify(needService, never()).recordAlnScreenerNeed(any(), any(), any(), any())
      verifyNoInteractions(scheduleService)
    }

    @Test
    fun `should not record ALN need given Curious returns no data`() {
      // Given
      given(curiousApiClient.getALNAssessment(any())).willReturn(ALNAssessmentDTO(data = null))

      // When
      service.processALNAssessmentUpdate(prisonNumber, info, inboundEvent)

      // Then
      verify(curiousApiClient).getALNAssessment(prisonNumber)
      verify(needService, never()).recordAlnScreenerNeed(any(), any(), any(), any())
      verifyNoInteractions(scheduleService)
    }

    private fun curiousResponse(vararg assessments: ALNAssessment) = ALNAssessmentDTO(
      data = ALNData(assessments = Assessments(aln = assessments.toList()), prn = prisonNumber),
    )

    private fun anAlnAssessment(
      assessmentDate: LocalDate,
      outcome: String,
      establishmentId: String?,
      modifiedDate: LocalDateTime? = null,
    ) = ALNAssessment(
      assessmentDate = assessmentDate,
      assessmentOutcome = outcome,
      establishmentId = establishmentId,
      establishmentName = null,
      hasPrisonerConsent = "YES",
      stakeholderReferral = null,
      createdDate = null,
      modifiedDate = modifiedDate,
    )
  }
}

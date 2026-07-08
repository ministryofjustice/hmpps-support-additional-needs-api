package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowableOfType
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.STRENGTH
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ALNScreenerNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ALNScreenerMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import java.time.LocalDate

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
   *  * processALNAssessmentUpdate
   */

  @Nested
  inner class DeleteCurrentScreener {
    val prisonNumber = "A1234AB"
    val prisonId = "BXI"
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
}

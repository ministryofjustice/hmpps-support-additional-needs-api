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
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.CHALLENGE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeAlnScreenerException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class ChallengeServiceTest {

  @Mock
  private lateinit var challengeRepository: ChallengeRepository

  @Mock
  private lateinit var referenceDataRepository: ReferenceDataRepository

  @Mock
  private lateinit var challengeMapper: ChallengeMapper

  @Mock
  private lateinit var alnScreenerRepository: AlnScreenerRepository

  @Mock
  private lateinit var scheduleService: ScheduleService

  @Mock
  private lateinit var needService: NeedService

  @Mock
  private lateinit var dataDeletionEventService: DataDeletionEventService

  @InjectMocks
  private lateinit var service: ChallengeService

  /*
   * TODO - write unit tests for public methods:
   *  * getChallenges
   *  * createChallenges
   *  * createAlnChallenges
   *  * updateChallenge
   *  * getChallenge
   *  * archiveChallenge
   *  * archiveAllScreenerChallenges
   *  * hasActiveNonALNChallenge
   */

  @Nested
  inner class DeleteChallenge {
    val prisonNumber = "A1234AB"
    val prisonId = "BXI"
    val reason = DeletionReason.ENTERED_IN_ERROR

    val challenge = ChallengeEntity(
      prisonNumber = prisonNumber,
      challengeType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = CHALLENGE, code = "READING"),
        description = "Reading",
      ),
      createdAtPrison = prisonId,
      updatedAtPrison = prisonId,
    )

    val challengeReference = challenge.reference

    @Test
    fun `should delete challenge given challenge is not an ALN challenge and prisoner has no needs after deletion`() {
      // Given
      challenge.alnScreenerId = null

      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)

      given(needService.hasNeed(any())).willReturn(false)

      // When
      service.deleteChallenge(prisonNumber, challengeReference, prisonId, reason)

      // Then
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository).delete(challenge)
      verify(needService).hasNeed(prisonNumber)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, prisonId)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should delete challenge given challenge is not an ALN challenge and prisoner has needs after deletion`() {
      // Given
      challenge.alnScreenerId = null

      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)

      given(needService.hasNeed(any())).willReturn(true)

      // When
      service.deleteChallenge(prisonNumber, challengeReference, prisonId, reason)

      // Then
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository).delete(challenge)
      verify(needService).hasNeed(prisonNumber)
      verifyNoInteractions(scheduleService)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should not delete challenge given challenge is an ALN challenge`() {
      // Given
      challenge.alnScreenerId = UUID.randomUUID()

      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)

      // When
      val actual = catchThrowableOfType(ChallengeAlnScreenerException::class.java) {
        service.deleteChallenge(prisonNumber, challengeReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(challengeReference)
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }

    @Test
    fun `should not delete challenge given challenge does not exist`() {
      // Given
      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(ChallengeNotFoundException::class.java) {
        service.deleteChallenge(prisonNumber, challengeReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(challengeReference)
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }
  }
}

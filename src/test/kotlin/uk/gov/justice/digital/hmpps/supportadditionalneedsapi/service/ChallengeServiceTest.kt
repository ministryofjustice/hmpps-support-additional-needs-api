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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeArchivedException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ChallengeNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ChallengeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveChallengeRequest
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
   *  * archiveAllScreenerChallenges
   *  * hasActiveNonALNChallenge
   */

  @Nested
  inner class DeleteChallenge {
    val prisonNumber = "A1234AB"
    val prisonId = "AAA"
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
      verifyNoInteractions(needService, scheduleService, dataDeletionEventService)
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
      verifyNoInteractions(needService, scheduleService, dataDeletionEventService)
    }
  }

  @Nested
  inner class ArchiveChallenge {
    val prisonNumber = "A1234AB"
    val createdAtPrisonId = "AAA"
    val request = ArchiveChallengeRequest(archiveReason = "No longer relevant", prisonId = "BBB")

    val challenge = ChallengeEntity(
      prisonNumber = prisonNumber,
      challengeType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = CHALLENGE, code = "READING"),
        description = "Reading",
      ),
      createdAtPrison = createdAtPrisonId,
      updatedAtPrison = createdAtPrisonId,
      active = true,
    )

    val challengeReference = challenge.reference

    @Test
    fun `should archive challenge given active non-ALN challenge and prisoner has no needs after archiving`() {
      // Given
      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)
      given(needService.hasNeed(any())).willReturn(false)

      // When
      service.archiveChallenge(prisonNumber, challengeReference, request)

      // Then
      assertThat(challenge.active).isFalse()
      assertThat(challenge.archiveReason).isEqualTo("No longer relevant")
      assertThat(challenge.updatedAtPrison).isEqualTo("BBB")
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository).save(challenge)
      verify(needService).hasNeed(prisonNumber)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, "BBB")
    }

    @Test
    fun `should archive challenge given active non-ALN challenge and prisoner still has needs after archiving`() {
      // Given
      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)
      given(needService.hasNeed(any())).willReturn(true)

      // When
      service.archiveChallenge(prisonNumber, challengeReference, request)

      // Then
      assertThat(challenge.active).isFalse()
      assertThat(challenge.archiveReason).isEqualTo("No longer relevant")
      assertThat(challenge.updatedAtPrison).isEqualTo("BBB")
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository).save(challenge)
      verify(needService).hasNeed(prisonNumber)
      verifyNoInteractions(scheduleService)
    }

    @Test
    fun `should not archive challenge given challenge is already archived`() {
      // Given
      challenge.active = false
      challenge.archiveReason = "Previously archived"

      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)

      // When
      val actual = catchThrowableOfType(ChallengeArchivedException::class.java) {
        service.archiveChallenge(prisonNumber, challengeReference, request)
      }

      // Then
      assertThat(actual)
        .hasMessage("Challenge with reference [$challengeReference] has been archived for prisoner [$prisonNumber]")
      assertThat(challenge.archiveReason).isEqualTo("Previously archived")
      assertThat(challenge.updatedAtPrison).isEqualTo(createdAtPrisonId)
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository, never()).save(any())
      verifyNoInteractions(needService, scheduleService)
    }

    @Test
    fun `should not archive challenge given challenge is an ALN challenge`() {
      // Given
      challenge.alnScreenerId = UUID.randomUUID()

      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(challenge)

      // When
      val actual = catchThrowableOfType(ChallengeAlnScreenerException::class.java) {
        service.archiveChallenge(prisonNumber, challengeReference, request)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(challengeReference)
      assertThat(challenge.active).isTrue()
      assertThat(challenge.archiveReason).isNull()
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository, never()).save(any())
      verifyNoInteractions(needService, scheduleService)
    }

    @Test
    fun `should not archive challenge given challenge does not exist`() {
      // Given
      given(challengeRepository.getChallengeEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(ChallengeNotFoundException::class.java) {
        service.archiveChallenge(prisonNumber, challengeReference, request)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(challengeReference)
      verify(challengeRepository).getChallengeEntityByPrisonNumberAndReference(prisonNumber, challengeReference)
      verify(challengeRepository, never()).save(any())
      verifyNoInteractions(needService, scheduleService)
    }
  }
}

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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.STRENGTH
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.StrengthRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthAlnScreenerException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.StrengthNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.StrengthMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import java.util.UUID

@ExtendWith(MockitoExtension::class)
class StrengthServiceTest {

  @Mock
  private lateinit var strengthRepository: StrengthRepository

  @Mock
  private lateinit var referenceDataRepository: ReferenceDataRepository

  @Mock
  private lateinit var alnScreenerRepository: AlnScreenerRepository

  @Mock
  private lateinit var strengthMapper: StrengthMapper

  @Mock
  private lateinit var dataDeletionEventService: DataDeletionEventService

  @InjectMocks
  private lateinit var service: StrengthService

  /*
   * TODO - write unit tests for public methods:
   *  * getStrengths
   *  * createStrengths
   *  * createAlnStrengths
   *  * updateStrength
   *  * getStrength
   *  * archiveStrength
   *  * archiveAllScreenerStrengths
   */

  @Nested
  inner class DeleteStrength {
    val prisonNumber = "A1234AB"
    val prisonId = "BXI"
    val reason = DeletionReason.ENTERED_IN_ERROR

    val strength = StrengthEntity(
      prisonNumber = prisonNumber,
      strengthType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = STRENGTH, code = "READING"),
        description = "Reading",
      ),
      createdAtPrison = prisonId,
      updatedAtPrison = prisonId,
    )

    val strengthReference = strength.reference

    @Test
    fun `should delete strength given strength is not an ALN strength`() {
      // Given
      strength.alnScreenerId = null

      given(strengthRepository.getStrengthEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(strength)

      // When
      service.deleteStrength(prisonNumber, strengthReference, prisonId, reason)

      // Then
      verify(strengthRepository).getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      verify(strengthRepository).delete(strength)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should not delete strength given strength is an ALN strength`() {
      // Given
      strength.alnScreenerId = UUID.randomUUID()

      given(strengthRepository.getStrengthEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(strength)

      // When
      val actual = catchThrowableOfType(StrengthAlnScreenerException::class.java) {
        service.deleteStrength(prisonNumber, strengthReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(strengthReference)
      verify(strengthRepository).getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      verify(strengthRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }

    @Test
    fun `should not delete strength given strength does not exist`() {
      // Given
      given(strengthRepository.getStrengthEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(StrengthNotFoundException::class.java) {
        service.deleteStrength(prisonNumber, strengthReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(strengthReference)
      verify(strengthRepository).getStrengthEntityByPrisonNumberAndReference(prisonNumber, strengthReference)
      verify(strengthRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }
  }
}

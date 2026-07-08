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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.SUPPORT_STRATEGY
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.SupportStrategyRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.SupportStrategyNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SupportStrategyMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason

@ExtendWith(MockitoExtension::class)
class SupportStrategyServiceTest {

  @Mock
  private lateinit var supportStrategyRepository: SupportStrategyRepository

  @Mock
  private lateinit var referenceDataRepository: ReferenceDataRepository

  @Mock
  private lateinit var supportStrategyMapper: SupportStrategyMapper

  @Mock
  private lateinit var dataDeletionEventService: DataDeletionEventService

  @InjectMocks
  private lateinit var service: SupportStrategyService

  /*
   * TODO - write unit tests for public methods:
   *  * getSupportStrategies
   *  * hasActiveSupportStrategies
   *  * createSupportStrategies
   *  * getSupportStrategy
   *  * updateSupportStrategy
   *  * archiveSupportStrategy
   */

  @Nested
  inner class DeleteSupportStrategy {
    val prisonNumber = "A1234AB"
    val prisonId = "BXI"
    val reason = DeletionReason.ENTERED_IN_ERROR

    val supportStrategy = SupportStrategyEntity(
      prisonNumber = prisonNumber,
      supportStrategyType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = SUPPORT_STRATEGY, code = "GENERAL"),
        description = "General need",
      ),
      createdAtPrison = prisonId,
      updatedAtPrison = prisonId,
    )

    val supportStrategyReference = supportStrategy.reference

    @Test
    fun `should delete support strategy`() {
      given(supportStrategyRepository.getSupportStrategyEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(supportStrategy)

      // When
      service.deleteSupportStrategy(prisonNumber, supportStrategyReference, prisonId, reason)

      // Then
      verify(supportStrategyRepository).getSupportStrategyEntityByPrisonNumberAndReference(prisonNumber, supportStrategyReference)
      verify(supportStrategyRepository).delete(supportStrategy)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should not delete support strategy given support strategy does not exist`() {
      // Given
      given(supportStrategyRepository.getSupportStrategyEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(SupportStrategyNotFoundException::class.java) {
        service.deleteSupportStrategy(prisonNumber, supportStrategyReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(supportStrategyReference)
      verify(supportStrategyRepository).getSupportStrategyEntityByPrisonNumberAndReference(prisonNumber, supportStrategyReference)
      verify(supportStrategyRepository, never()).delete(any())
      verifyNoInteractions(dataDeletionEventService)
    }
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.DataDeletionEventEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.DataDeletionEventRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.DeletionReason as DeletionReasonEntity

@ExtendWith(MockitoExtension::class)
class DataDeletionEventServiceTest {

  @Mock
  private lateinit var dataDeletionEventRepository: DataDeletionEventRepository

  @InjectMocks
  private lateinit var service: DataDeletionEventService

  @Test
  fun `should record data deletion event`() {
    // Given
    val prisonNumber = "A1234AB"
    val prisonId = "MDI"
    val deletionReason = DeletionReason.ENTERED_IN_ERROR

    val expectedDataDeletionEvent = DataDeletionEventEntity(
      prisonNumber = "A1234AB",
      reason = DeletionReasonEntity.ENTERED_IN_ERROR,
      dataDeletedAtPrison = "MDI",
    )

    // When
    service.recordDataDeletionEvent(prisonNumber, prisonId, deletionReason)

    // Then
    verify(dataDeletionEventRepository).save(expectedDataDeletionEvent)
  }
}

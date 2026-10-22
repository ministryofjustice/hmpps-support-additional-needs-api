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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain.CONDITION
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataKey
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReferenceDataRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionArchivedException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.ConditionNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ConditionMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.DeletionReason

@ExtendWith(MockitoExtension::class)
class ConditionServiceTest {

  @Mock
  private lateinit var conditionRepository: ConditionRepository

  @Mock
  private lateinit var referenceDataRepository: ReferenceDataRepository

  @Mock
  private lateinit var conditionMapper: ConditionMapper

  @Mock
  private lateinit var scheduleService: ScheduleService

  @Mock
  private lateinit var needService: NeedService

  @Mock
  private lateinit var dataDeletionEventService: DataDeletionEventService

  @InjectMocks
  private lateinit var service: ConditionService

  /*
   * TODO - write unit tests for public methods:
   *  * getConditions
   *  * createConditions
   *  * getCondition
   *  * updateCondition
   */

  @Nested
  inner class DeleteCondition {
    val prisonNumber = "A1234AA"
    val createdAtPrisonId = "AAA"
    val prisonId = "BBB"
    val reason = DeletionReason.ENTERED_IN_ERROR

    val condition = ConditionEntity(
      prisonNumber = prisonNumber,
      source = Source.SELF_DECLARED,
      conditionType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = CONDITION, code = "ADHD"),
        description = "Attention Deficit Hyperactivity Disorder (ADHD / ADD)",
      ),
      createdAtPrison = createdAtPrisonId,
      updatedAtPrison = createdAtPrisonId,
    )

    val conditionReference = condition.reference

    @Test
    fun `should delete condition given prisoner has no needs after deletion`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(condition)

      given(needService.hasNeed(any())).willReturn(false)

      // When
      service.deleteCondition(prisonNumber, conditionReference, prisonId, reason)

      // Then
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      verify(conditionRepository).delete(condition)
      verify(needService).hasNeed(prisonNumber)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, prisonId)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should delete condition given prisoner has needs after deletion`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(condition)

      given(needService.hasNeed(any())).willReturn(true)

      // When
      service.deleteCondition(prisonNumber, conditionReference, prisonId, reason)

      // Then
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      verify(conditionRepository).delete(condition)
      verify(needService).hasNeed(prisonNumber)
      verifyNoInteractions(scheduleService)
      verify(dataDeletionEventService).recordDataDeletionEvent(prisonNumber, prisonId, reason)
    }

    @Test
    fun `should not delete condition given condition does not exist`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(ConditionNotFoundException::class.java) {
        service.deleteCondition(prisonNumber, conditionReference, prisonId, reason)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(conditionReference)
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, conditionReference)
      verify(conditionRepository, never()).delete(any())
      verifyNoInteractions(needService, scheduleService, dataDeletionEventService)
    }
  }

  @Nested
  inner class ArchiveCondition {
    val prisonNumber = "A1234AA"
    val createdAtPrisonId = "AAA"
    val request = ArchiveConditionRequest(archiveReason = "No longer relevant", prisonId = "BBB")

    val condition = ConditionEntity(
      prisonNumber = prisonNumber,
      source = Source.SELF_DECLARED,
      conditionType = ReferenceDataEntity(
        key = ReferenceDataKey(domain = CONDITION, code = "ADHD"),
        description = "Attention Deficit Hyperactivity Disorder (ADHD / ADD)",
      ),
      createdAtPrison = createdAtPrisonId,
      updatedAtPrison = createdAtPrisonId,
      active = true,
    )

    @Test
    fun `should archive condition given active condition and prisoner has no needs after archiving`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(condition)
      given(needService.hasNeed(any())).willReturn(false)

      // When
      service.archiveCondition(prisonNumber, condition.reference, request)

      // Then
      assertThat(condition.active).isFalse()
      assertThat(condition.archiveReason).isEqualTo("No longer relevant")
      assertThat(condition.updatedAtPrison).isEqualTo("BBB")
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, condition.reference)
      verify(conditionRepository).save(condition)
      verify(needService).hasNeed(prisonNumber)
      verify(scheduleService).processNeedChange(prisonNumber, false, null, "BBB")
    }

    @Test
    fun `should archive condition given active condition and prisoner still has needs after archiving`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(condition)
      given(needService.hasNeed(any())).willReturn(true)

      // When
      service.archiveCondition(prisonNumber, condition.reference, request)

      // Then
      assertThat(condition.active).isFalse()
      assertThat(condition.archiveReason).isEqualTo("No longer relevant")
      assertThat(condition.updatedAtPrison).isEqualTo("BBB")
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, condition.reference)
      verify(conditionRepository).save(condition)
      verify(needService).hasNeed(prisonNumber)
      verifyNoInteractions(scheduleService)
    }

    @Test
    fun `should not archive condition given condition is already archived`() {
      // Given
      condition.active = false
      condition.archiveReason = "Previously archived"

      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(condition)

      // When
      val actual = catchThrowableOfType(ConditionArchivedException::class.java) {
        service.archiveCondition(prisonNumber, condition.reference, request)
      }

      // Then
      assertThat(actual)
        .hasMessage("Condition with reference [${condition.reference}] has been archived for prisoner [$prisonNumber]")
      assertThat(condition.archiveReason).isEqualTo("Previously archived")
      assertThat(condition.updatedAtPrison).isEqualTo(createdAtPrisonId)
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, condition.reference)
      verify(conditionRepository, never()).save(any())
      verifyNoInteractions(needService, scheduleService)
    }

    @Test
    fun `should not archive condition given condition does not exist`() {
      // Given
      given(conditionRepository.getConditionEntityByPrisonNumberAndReference(any(), any()))
        .willReturn(null)

      // When
      val actual = catchThrowableOfType(ConditionNotFoundException::class.java) {
        service.archiveCondition(prisonNumber, condition.reference, request)
      }

      // Then
      assertThat(actual.prisonNumber).isEqualTo(prisonNumber)
      assertThat(actual.reference).isEqualTo(condition.reference)
      verify(conditionRepository).getConditionEntityByPrisonNumberAndReference(prisonNumber, condition.reference)
      verify(conditionRepository, never()).save(any())
      verifyNoInteractions(needService, scheduleService)
    }
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import uk.gov.justice.digital.hmpps.curiousapi.resource.model.aValidLearnerNeurodivergenceDTO
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousPrisonerRecordNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber

@ExtendWith(MockitoExtension::class)
class CuriousApiServiceTest {
  @InjectMocks
  private lateinit var service: CuriousApiService

  @Mock
  private lateinit var curiousApiClient: CuriousApiClient

  @Test
  fun `should determine whether prisoner has learning difficulties and disabilities given populated response from Curious API`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(curiousApiClient.getLearningDifficultiesAndDisabilities(prisonNumber)).willReturn(
      listOf(aValidLearnerNeurodivergenceDTO(prn = prisonNumber)),
    )

    // When
    val actual = service.hasLearningDifficultiesAndDisabilities(prisonNumber)

    // Then
    assertThat(actual).isTrue
  }

  @Test
  fun `should determine whether prisoner has learning difficulties and disabilities given empty response from Curious API`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(curiousApiClient.getLearningDifficultiesAndDisabilities(prisonNumber)).willReturn(
      emptyList(),
    )

    // When
    val actual = service.hasLearningDifficultiesAndDisabilities(prisonNumber)

    // Then
    assertThat(actual).isFalse
  }

  @Test
  fun `should determine whether prisoner has learning difficulties and disabilities given not found exception from Curious API`() {
    // Given
    val prisonNumber = randomValidPrisonNumber()
    given(curiousApiClient.getLearningDifficultiesAndDisabilities(prisonNumber)).willThrow(
      CuriousPrisonerRecordNotFoundException(prisonNumber),
    )

    // When
    val actual = service.hasLearningDifficultiesAndDisabilities(prisonNumber)

    // Then
    assertThat(actual).isFalse
  }
}

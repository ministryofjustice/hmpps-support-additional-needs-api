package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import org.springframework.web.reactive.function.client.WebClientResponseException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PagedPrisonerResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PrisonerSearchApiException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner

@ExtendWith((MockitoExtension::class))
class PrisonerSearchApiServiceTest {
  @Mock
  private lateinit var prisonerSearchApiClient: PrisonerSearchApiClient

  @InjectMocks
  private lateinit var service: PrisonerSearchApiService

  @Nested
  inner class GetAllPrisonersInPrison {
    @Test
    fun `should get all prisoners in a prison given they are all returned in the first request`() {
      // Given
      val prisonId = "BXI"

      val expectedPrisoners = listOf(
        aValidPrisoner(
          prisonerNumber = "A1234BC",
          prisonId = prisonId,
        ),
        aValidPrisoner(
          prisonerNumber = "A9999XX",
          prisonId = prisonId,
        ),
      )

      val pagedPrisonerResponse = PagedPrisonerResponse(true, expectedPrisoners)
      given(prisonerSearchApiClient.getPrisonersByPrisonId(any(), any(), any())).willReturn(pagedPrisonerResponse)

      // When
      val actual = service.getAllPrisonersInPrison(prisonId, 250)

      // Then
      assertThat(actual).isEqualTo(expectedPrisoners)
      verify(prisonerSearchApiClient).getPrisonersByPrisonId("BXI", 0, 250)
    }

    @Test
    fun `should get all prisoners in a prison given they are returned over multiple requests`() {
      // Given
      val prisonId = "BXI"

      val prisonersReturnedInFirstPage = List(250) { index ->
        aValidPrisoner(
          prisonerNumber = "A${index.toString().padEnd(4, '0')}BC",
          prisonId = prisonId,
        )
      }
      val prisonersReturnedInSecondPage = List(250) { index ->
        aValidPrisoner(
          prisonerNumber = "A${(250 + index).toString().padEnd(4, '0')}BC",
          prisonId = prisonId,
        )
      }
      val prisonersReturnedInThirdPage = List(50) { index ->
        aValidPrisoner(
          prisonerNumber = "A${(500 + index).toString().padEnd(4, '0')}BC",
          prisonId = prisonId,
        )
      }

      given(prisonerSearchApiClient.getPrisonersByPrisonId(any(), any(), any())).willReturn(
        PagedPrisonerResponse(false, prisonersReturnedInFirstPage),
        PagedPrisonerResponse(false, prisonersReturnedInSecondPage),
        PagedPrisonerResponse(true, prisonersReturnedInThirdPage),
      )

      // When
      val actual = service.getAllPrisonersInPrison(prisonId, 250)

      // Then
      assertThat(actual.size).isEqualTo(550)
      verify(prisonerSearchApiClient).getPrisonersByPrisonId("BXI", 0, 250)
      verify(prisonerSearchApiClient).getPrisonersByPrisonId("BXI", 1, 250)
      verify(prisonerSearchApiClient).getPrisonersByPrisonId("BXI", 2, 250)
    }

    @Test
    fun `should not get all prisoners given prisoner search API returns an error`() {
      // Given
      val prisonId = "BXI"

      val expectedException = PrisonerSearchApiException(
        "Error retrieving prisoners by prisonId BXI",
        WebClientResponseException(500, "Service unavailable", null, null, null),
      )
      given(prisonerSearchApiClient.getPrisonersByPrisonId(any(), any(), any())).willThrow(expectedException)

      // When
      val exception = assertThrows(PrisonerSearchApiException::class.java) {
        service.getAllPrisonersInPrison(prisonId, 250)
      }

      // Then
      assertThat(exception).isEqualTo(expectedException)
      verify(prisonerSearchApiClient).getPrisonersByPrisonId("BXI", 0, 250)
    }
  }

  @Nested
  inner class GetPrisoner {
    @Test
    fun `should get prisoner by their prison number`() {
      // Given
      val prisonNumber = "A1234BC"
      val expectedPrisoner = aValidPrisoner(prisonerNumber = prisonNumber)
      given(prisonerSearchApiClient.getPrisoner(any())).willReturn(expectedPrisoner)

      // When
      val actual = service.getPrisoner(prisonNumber)

      // Then
      assertThat(actual).isEqualTo(expectedPrisoner)
      verify(prisonerSearchApiClient).getPrisoner("A1234BC")
    }

    @Test
    fun `should not get prisoner given prisoner search API returns an error`() {
      // Given
      val prisonNumber = "A1234BC"

      val expectedException = PrisonerSearchApiException(
        "Error retrieving prisoner by prisonNumber A1234BC",
        WebClientResponseException(500, "Service unavailable", null, null, null),
      )
      given(prisonerSearchApiClient.getPrisoner(any())).willThrow(expectedException)

      // When
      val exception = assertThrows(PrisonerSearchApiException::class.java) {
        service.getPrisoner(prisonNumber)
      }

      // Then
      assertThat(exception).isEqualTo(expectedException)
      verify(prisonerSearchApiClient).getPrisoner("A1234BC")
    }

    @Test
    fun `should not get prisoner given prisoner search API returns a not found error`() {
      // Given
      val prisonNumber = "A1234BC"

      val expectedException = PrisonerNotFoundException(prisonNumber)
      given(prisonerSearchApiClient.getPrisoner(any())).willThrow(expectedException)

      // When
      val exception = assertThrows(PrisonerNotFoundException::class.java) {
        service.getPrisoner(prisonNumber)
      }

      // Then
      assertThat(exception).isEqualTo(expectedException)
      verify(prisonerSearchApiClient).getPrisoner("A1234BC")
    }
  }
}

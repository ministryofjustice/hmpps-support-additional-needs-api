package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.given
import org.mockito.kotlin.verify
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.SentenceTypeMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.Person
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortDirection
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchSortField
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class SearchServiceTest {
  @Mock
  private lateinit var prisonerSearchApiService: PrisonerSearchApiService

  @InjectMocks
  private lateinit var service: SearchService

  @Nested
  inner class FilterByCriteria {
    @Test
    fun `should search for prisoners given search criteria with no filtering`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        prisonerNameOrNumber = null,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect John Baker, Albert Johnson, Peter Smith due to default sort order by name ascending
        JOHN_BAKER.asPerson(),
        ALBERT_JOHNSON.asPerson(),
        PETER_SMITH.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria with filtering that matches some prisoners`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        prisonerNameOrNumber = "john",
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        JOHN_BAKER.asPerson(),
        ALBERT_JOHNSON.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria with filtering that matches nobody`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        prisonerNameOrNumber = "henry",
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).isEmpty()
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }
  }

  @Nested
  inner class SortBy {
    @Test
    fun `should search for prisoners given search criteria that sorts on name descending`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        sortBy = SearchSortField.PRISONER_NAME,
        sortDirection = SearchSortDirection.DESC,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect Peter Smith, Albert Johnson, John Baker due to sort order by name descending
        PETER_SMITH.asPerson(),
        ALBERT_JOHNSON.asPerson(),
        JOHN_BAKER.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria that sorts on cell location descending`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        sortBy = SearchSortField.CELL_LOCATION,
        sortDirection = SearchSortDirection.DESC,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect Peter Smith, John Baker, Albert Johnson due to sort order by cell location descending
        PETER_SMITH.asPerson(),
        JOHN_BAKER.asPerson(),
        ALBERT_JOHNSON.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria that sorts on cell location ascending`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        sortBy = SearchSortField.CELL_LOCATION,
        sortDirection = SearchSortDirection.ASC,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect Albert Johnson, John Baker, Peter Smith due to sort order by cell location ascending
        ALBERT_JOHNSON.asPerson(),
        JOHN_BAKER.asPerson(),
        PETER_SMITH.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria that sorts on release date descending`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        sortBy = SearchSortField.RELEASE_DATE,
        sortDirection = SearchSortDirection.DESC,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect Peter Smith, Albert Johnson, John Baker due to sort order by release date descending
        PETER_SMITH.asPerson(),
        ALBERT_JOHNSON.asPerson(),
        JOHN_BAKER.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }

    @Test
    fun `should search for prisoners given search criteria that sorts on release date ascending`() {
      // Given
      val searchCriteria = basicSearchCriteria().copy(
        sortBy = SearchSortField.RELEASE_DATE,
        sortDirection = SearchSortDirection.ASC,
      )

      given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(PRISONERS_IN_PRISON)

      // When
      val actual = service.searchPrisoners(searchCriteria)

      // Then
      assertThat(actual).containsExactly(
        // expect John Baker, Albert Johnson, Peter Smith due to sort order by release date ascending
        JOHN_BAKER.asPerson(),
        ALBERT_JOHNSON.asPerson(),
        PETER_SMITH.asPerson(),
      )
      verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
    }
  }

  @Test
  fun `should search for prisoners given prisoner search API returns no prisoners`() {
    // Given
    val searchCriteria = basicSearchCriteria()

    given(prisonerSearchApiService.getAllPrisonersInPrison(any(), anyOrNull())).willReturn(emptyList())

    // When
    val actual = service.searchPrisoners(searchCriteria)

    // Then
    assertThat(actual).isEmpty()
    verify(prisonerSearchApiService).getAllPrisonersInPrison(PRISON_ID)
  }

  private fun basicSearchCriteria() = SearchCriteria(
    prisonId = PRISON_ID,
    prisonerNameOrNumber = null,
    sortBy = SearchSortField.PRISONER_NAME,
    sortDirection = SearchSortDirection.ASC,
  )

  private fun Prisoner.asPerson(): Person = Person(
    forename = firstName,
    surname = lastName,
    prisonNumber = prisonerNumber,
    dateOfBirth = dateOfBirth,
    releaseDate = releaseDate,
    cellLocation = cellLocation,
    sentenceType = SentenceTypeMapper.fromPrisonerSearchApiToModel(legalStatus),
    additionalNeeds = null,
  )

  companion object {
    private const val PRISON_ID = "BXI"
    private val PETER_SMITH = aValidPrisoner(
      firstName = "Peter",
      lastName = "Smith",
      prisonerNumber = "A1234BC",
      prisonId = PRISON_ID,
      cellLocation = "C2",
      releaseDate = LocalDate.parse("2029-01-01"),
    )
    private val JOHN_BAKER = aValidPrisoner(
      firstName = "John",
      lastName = "Baker",
      prisonerNumber = "A9999XX",
      prisonId = PRISON_ID,
      cellLocation = "A9",
      releaseDate = LocalDate.parse("2025-02-15"),
    )
    private val ALBERT_JOHNSON = aValidPrisoner(
      firstName = "Albert",
      lastName = "Johnson",
      prisonerNumber = "C1299ZA",
      prisonId = PRISON_ID,
      cellLocation = "A1",
      releaseDate = LocalDate.parse("2027-10-29"),
    )
    private val PRISONERS_IN_PRISON = listOf(PETER_SMITH, JOHN_BAKER, ALBERT_JOHNSON)
  }
}

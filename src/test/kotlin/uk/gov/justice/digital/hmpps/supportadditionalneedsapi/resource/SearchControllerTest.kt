package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PaginationMetaData
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.aValidPerson
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SearchService

@ExtendWith(MockitoExtension::class)
class SearchControllerTest {
  @Mock
  private lateinit var service: SearchService

  @InjectMocks
  private lateinit var controller: SearchController

  @Nested
  inner class SearchByPrison {
    @Nested
    inner class Pagination {
      @Test
      fun `should search by prison given no pagination parameters`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..152).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 152,
          totalPages = 4,
          page = 1,
          pageSize = 50,
          first = true,
          last = false,
        )

        // When
        val actual = controller.searchByPrison(PRISON_ID)

        // Then
        assertThat(actual.people).hasSize(50)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }

      @Test
      fun `should search by prison given pagination parameters where the page would be the first of the results`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..152).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 152,
          totalPages = 8,
          page = 1,
          pageSize = 20,
          first = true,
          last = false,
        )

        // When
        val actual = controller.searchByPrison(
          prisonId = PRISON_ID,
          page = 1,
          pageSize = 20,
        )

        // Then
        assertThat(actual.people).hasSize(20)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }

      @Test
      fun `should search by prison given pagination parameters where the page would be the last of the results`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..152).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 152,
          totalPages = 8,
          page = 8,
          pageSize = 20,
          first = false,
          last = true,
        )

        // When
        val actual = controller.searchByPrison(
          prisonId = PRISON_ID,
          page = 8,
          pageSize = 20,
        )

        // Then
        assertThat(actual.people).hasSize(12)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }

      @Test
      fun `should search by prison given pagination parameters where the page would be in the middle of the results`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..152).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 152,
          totalPages = 8,
          page = 3,
          pageSize = 20,
          first = false,
          last = false,
        )

        // When
        val actual = controller.searchByPrison(
          prisonId = PRISON_ID,
          page = 3,
          pageSize = 20,
        )

        // Then
        assertThat(actual.people).hasSize(20)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }

      @Test
      fun `should search by prison given pagination parameters but there are fewer results than the page size`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..5).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 5,
          totalPages = 1,
          page = 1,
          pageSize = 20,
          first = true,
          last = true,
        )

        // When
        val actual = controller.searchByPrison(
          prisonId = PRISON_ID,
          page = 3,
          pageSize = 20,
        )

        // Then
        assertThat(actual.people).hasSize(5)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }

      @Test
      fun `should search by prison given pagination parameters where the requested page is beyond the end of the results`() {
        // Given
        given(service.searchPrisoners(any())).willReturn((1..152).map { aValidPerson() })

        val expectedPagination = PaginationMetaData(
          totalElements = 152,
          totalPages = 8,
          page = 8,
          pageSize = 20,
          first = false,
          last = true,
        )

        // When
        val actual = controller.searchByPrison(
          prisonId = PRISON_ID,
          page = 9,
          pageSize = 20,
        )

        // Then
        assertThat(actual.people).hasSize(12)
        assertThat(actual.pagination).isEqualTo(expectedPagination)
      }
    }

    @Test
    fun `should search by prison given no service returns no prisoners`() {
      // Given
      given(service.searchPrisoners(any())).willReturn(emptyList())

      val expected = SearchByPrisonResponse(
        people = emptyList(),
        pagination = PaginationMetaData(
          totalElements = 0,
          totalPages = 1,
          page = 1,
          pageSize = 50,
          first = true,
          last = true,
        ),
      )

      // When
      val actual = controller.searchByPrison(PRISON_ID)

      // Then
      assertThat(actual).isEqualTo(expected)
    }
  }

  companion object {
    private const val PRISON_ID = "BXI"
  }
}

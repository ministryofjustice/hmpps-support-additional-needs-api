package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.util.DefaultUriBuilderFactory
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate

class SearchByPrisonTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/search/prisons/{prisonId}/people"
    private const val PRISON_ID = "BXI"

    private val today = LocalDate.now()

    private val PRISONER_1 = aValidPrisoner(lastName = "PRISONER_1", prisonerNumber = randomValidPrisonNumber(), releaseDate = today.plusDays(1), cellLocation = "Z-3")
    private val PRISONER_2 = aValidPrisoner(lastName = "PRISONER_2", prisonerNumber = randomValidPrisonNumber(), releaseDate = today.plusDays(100), cellLocation = "Z-1")
    private val PRISONER_3 = aValidPrisoner(lastName = "PRISONER_3", prisonerNumber = randomValidPrisonNumber(), releaseDate = today.plusDays(10), cellLocation = "A-1")
    private val PRISONER_4 = aValidPrisoner(lastName = "PRISONER_4", prisonerNumber = randomValidPrisonNumber(), releaseDate = today.plusDays(50), cellLocation = "B-2")
    private val PRISONER_5 = aValidPrisoner(lastName = "PRISONER_5", prisonerNumber = randomValidPrisonNumber(), releaseDate = today.plusDays(30), cellLocation = "Z-2")
    private val PRISONERS_IN_PRISON = listOf(PRISONER_1, PRISONER_2, PRISONER_3, PRISONER_4, PRISONER_5)
  }

  @BeforeEach
  fun `setup data`() {
    stubGetTokenFromHmppsAuth()
    stubGetPrisonersInPrisonFromPrisonerSearchApi(PRISON_ID, PRISONERS_IN_PRISON)
    stubForBankHoliday()

    educationEnrolmentRepository.deleteAll()
    educationRepository.deleteAll()
    prisonerInEducation(PRISONER_3.prisonerNumber)
    prisonerInEducation(PRISONER_2.prisonerNumber)
    prisonerInEducation(PRISONER_5.prisonerNumber)

    conditionRepository.deleteAll()
    prisonerHasNeed(PRISONER_1.prisonerNumber)
    prisonerHasNeed(PRISONER_5.prisonerNumber)
    prisonerHasNeed(PRISONER_4.prisonerNumber)
  }

  @Test
  fun `should return paged results given valid request`() {
    // Given

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SearchByPrisonResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .isPage(1)
      .hasPageSize(50)
      .isFirstPage()
      .isLastPage()
      .hasTotalElements(5)
      .currentPageHasNumberOfRecords(5)
  }

  @Test
  fun `should return paged results with one result given search by prisoner Id`() {
    // Given
    stubGetTokenFromHmppsAuth()
    aPrisonerExists(PRISONER_1.prisonerNumber, PRISON_ID)
    stubForBankHoliday()

    // When
    val response = webTestClient.get()
      .uri { uriBuilder ->
        uriBuilder
          .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
          .queryParam("prisonerNameOrNumber", PRISONER_1.prisonerNumber)
          .build()
      }
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SearchByPrisonResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .isPage(1)
      .hasPageSize(50)
      .isFirstPage()
      .isLastPage()
      .hasTotalElements(1)
      .currentPageHasNumberOfRecords(1)
  }

  @Test
  fun `should return paged results containing trimmed name values`() {
    // Given
    stubGetTokenFromHmppsAuth()
    val apiResponse = """
      {
        "last": true,
        "content": [
          {
            "prisonerNumber": "  A1234AA  ",
            "legalStatus": "SENTENCED",
            "releaseDate": "2033-01-01",
            "prisonId": "BXI",
            "isIndeterminateSentence": false,
            "isRecall": false,
            "firstName": "  BOB   ",
            "lastName": "  SMITH   ",
            "cellLocation": "B-2-022",
            "dateOfBirth": "1980-01-01",
            "releaseType": "ARD"
          }        
        ]
      }
    """.trimIndent()
    stubForBankHoliday()
    stubGetPrisonersInPrisonFromPrisonerSearchApi(PRISON_ID, apiResponse)

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus()
      .isOk
      .returnResult(SearchByPrisonResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .person(1) {
        it.hasPrisonNumber("A1234AA")
          .hasForename("BOB")
          .hasSurname("SMITH")
      }
  }

  @Test
  fun `should return unauthorized given no bearer token`() {
    webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .exchange()
      .expectStatus()
      .isUnauthorized
  }

  @Test
  fun `should return forbidden given bearer token without required role`() {
    // Given

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, PRISON_ID)
      .headers(setAuthorisation(roles = listOf()))
      .exchange()
      .expectStatus()
      .isForbidden
      .returnResult(ErrorResponse::class.java)

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual)
      .hasStatus(HttpStatus.FORBIDDEN.value())
      .hasUserMessage("Access Denied")
      .hasDeveloperMessage("Access denied on uri=/search/prisons/BXI/people")
  }

  @Nested
  inner class Sorting {
    @Test
    fun `sort by release date ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "RELEASE_DATE")
            .queryParam("sortDirection", "ASC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_3") })
        .person(3, { it.hasSurname("PRISONER_5") })
        .person(4, { it.hasSurname("PRISONER_4") })
        .person(5, { it.hasSurname("PRISONER_2") })
    }

    @Test
    fun `sort by release date descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "RELEASE_DATE")
            .queryParam("sortDirection", "DESC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_2") })
        .person(2, { it.hasSurname("PRISONER_4") })
        .person(3, { it.hasSurname("PRISONER_5") })
        .person(4, { it.hasSurname("PRISONER_3") })
        .person(5, { it.hasSurname("PRISONER_1") })
    }

    @Test
    fun `sort by name ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "PRISONER_NAME")
            .queryParam("sortDirection", "ASC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_2") })
        .person(3, { it.hasSurname("PRISONER_3") })
        .person(4, { it.hasSurname("PRISONER_4") })
        .person(5, { it.hasSurname("PRISONER_5") })
    }

    @Test
    fun `sort by name descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "PRISONER_NAME")
            .queryParam("sortDirection", "DESC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_5") })
        .person(2, { it.hasSurname("PRISONER_4") })
        .person(3, { it.hasSurname("PRISONER_3") })
        .person(4, { it.hasSurname("PRISONER_2") })
        .person(5, { it.hasSurname("PRISONER_1") })
    }

    @Test
    fun `sort by cell location ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "CELL_LOCATION")
            .queryParam("sortDirection", "ASC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_3") })
        .person(2, { it.hasSurname("PRISONER_4") })
        .person(3, { it.hasSurname("PRISONER_2") })
        .person(4, { it.hasSurname("PRISONER_5") })
        .person(5, { it.hasSurname("PRISONER_1") })
    }

    @Test
    fun `sort by cell location descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "CELL_LOCATION")
            .queryParam("sortDirection", "DESC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_5") })
        .person(3, { it.hasSurname("PRISONER_2") })
        .person(4, { it.hasSurname("PRISONER_4") })
        .person(5, { it.hasSurname("PRISONER_3") })
    }

    @Test
    fun `sort by in education ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "IN_EDUCATION")
            .queryParam("sortDirection", "ASC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_4") })
        .person(3, { it.hasSurname("PRISONER_2") })
        .person(4, { it.hasSurname("PRISONER_3") })
        .person(5, { it.hasSurname("PRISONER_5") })
    }

    @Test
    fun `sort by in education descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "IN_EDUCATION")
            .queryParam("sortDirection", "DESC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_2") })
        .person(2, { it.hasSurname("PRISONER_3") })
        .person(3, { it.hasSurname("PRISONER_5") })
        .person(4, { it.hasSurname("PRISONER_1") })
        .person(5, { it.hasSurname("PRISONER_4") })
    }

    @Test
    fun `sort by has additional need ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "HAS_ADDITIONAL_NEED")
            .queryParam("sortDirection", "ASC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_2") })
        .person(2, { it.hasSurname("PRISONER_3") })
        .person(3, { it.hasSurname("PRISONER_1") })
        .person(4, { it.hasSurname("PRISONER_4") })
        .person(5, { it.hasSurname("PRISONER_5") })
    }

    @Test
    fun `sort by has additional need descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "HAS_ADDITIONAL_NEED")
            .queryParam("sortDirection", "DESC")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .returnResult(SearchByPrisonResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasTotalElements(5)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_4") })
        .person(3, { it.hasSurname("PRISONER_5") })
        .person(4, { it.hasSurname("PRISONER_2") })
        .person(5, { it.hasSurname("PRISONER_3") })
    }

    @Test
    fun `should return bad request given request with invalid sortBy value`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "invalid sortBy value")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessageContaining("Method parameter 'sortBy': Failed to convert value of type 'java.lang.String' to required type")
    }

    @Test
    fun `should return bad request given request with invalid sortDirection value`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortDirection", "invalid sortDirection value")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessageContaining("Method parameter 'sortDirection': Failed to convert value of type 'java.lang.String' to required type")
    }
  }

  @Nested
  inner class Pagination {
    @Test
    fun `should return bad request given request with invalid page value`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("page", "invalid page value")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessageContaining("Method parameter 'page': Failed to convert value of type 'java.lang.String' to required type")
    }

    @Test
    fun `should return bad request given request with invalid pageSize value`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("pageSize", "invalid pageSize value")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessageContaining("Method parameter 'pageSize': Failed to convert value of type 'java.lang.String' to required type")
    }

    @Test
    fun `should return bad request given request with page value less than 1`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("page", "0")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessage("searchByPrison.page must be greater than or equal to 1")
    }

    @Test
    fun `should return bad request given request with pageSize less than 1`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("pageSize", "0")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessage("searchByPrison.pageSize must be greater than or equal to 1")
    }

    @Test
    fun `should return bad request given request with both page and pageSize less than 1`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("page", "0")
            .queryParam("pageSize", "0")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .returnResult(ErrorResponse::class.java)

      // Then
      val actual = response.responseBody.blockFirst()
      assertThat(actual)
        .hasStatus(HttpStatus.BAD_REQUEST.value())
        .hasUserMessageContaining("searchByPrison.pageSize must be greater than or equal to 1")
        .hasUserMessageContaining("searchByPrison.page must be greater than or equal to 1")
    }
  }
}

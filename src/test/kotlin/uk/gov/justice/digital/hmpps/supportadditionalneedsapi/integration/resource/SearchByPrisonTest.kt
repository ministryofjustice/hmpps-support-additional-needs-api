package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.web.util.DefaultUriBuilderFactory
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.aValidPrisoner
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.Constants.Companion.IN_THE_FUTURE_DATE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.ACTIVE_PLAN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.INACTIVE_PLAN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.NEEDS_PLAN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.NO_PLAN
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.PLAN_DECLINED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.PLAN_DUE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.PLAN_OVERDUE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.REVIEW_DUE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanStatus.REVIEW_OVERDUE
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SearchByPrisonResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.assertThat
import java.time.LocalDate

class SearchByPrisonTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/search/prisons/{prisonId}/people"
    private const val PRISON_ID = "BXI"

    private val today = LocalDate.now()

    private val PRISONER_1 = aValidPrisoner(
      lastName = "PRISONER_1",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(1),
      cellLocation = "Z-3",
    )
    private val PRISONER_2 = aValidPrisoner(
      lastName = "PRISONER_2",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = null,
      cellLocation = "Z-1",
    )
    private val PRISONER_3 = aValidPrisoner(
      lastName = "PRISONER_3",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(10),
      cellLocation = "A-1",
    )
    private val PRISONER_4 = aValidPrisoner(
      lastName = "PRISONER_4",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(50),
      cellLocation = "B-2",
    )
    private val PRISONER_5 = aValidPrisoner(
      lastName = "PRISONER_5",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(30),
      cellLocation = "Z-2",
    )
    private val PRISONER_6 = aValidPrisoner(
      lastName = "PRISONER_6",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(80),
      cellLocation = null,
    )
    private val PRISONER_7 = aValidPrisoner(
      lastName = "PRISONER_7",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(60),
      cellLocation = "C-2",
    )
    private val PRISONER_8 = aValidPrisoner(
      lastName = "PRISONER_8",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(90),
      cellLocation = "C-1",
    )
    private val PRISONER_9 = aValidPrisoner(
      lastName = "PRISONER_9",
      prisonerNumber = randomValidPrisonNumber(),
      releaseDate = today.plusDays(20),
      cellLocation = "Z-4",
    )
    private val PRISONERS_IN_PRISON =
      listOf(PRISONER_1, PRISONER_2, PRISONER_3, PRISONER_4, PRISONER_5, PRISONER_6, PRISONER_7, PRISONER_8, PRISONER_9)
  }

  @BeforeEach
  fun `setup data`() {
    stubGetTokenFromHmppsAuth()
    stubGetPrisonersInPrisonFromPrisonerSearchApi(PRISON_ID, PRISONERS_IN_PRISON)
    stubForBankHoliday()

    educationEnrolmentRepository.deleteAll()
    educationRepository.deleteAll()
    conditionRepository.deleteAll()
    planCreationScheduleRepository.deleteAll()
    elspReviewRepository.deleteAll()
    reviewScheduleRepository.deleteAll()

    // set up each person to have a specific education, needs, and plan status
    // needsPlan
    // PRISONER_1
    prisonerInEducation(PRISONER_1.prisonerNumber)
    prisonerHasNeed(PRISONER_1.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_1.prisonerNumber, deadlineDate = IN_THE_FUTURE_DATE)

    // PlanDue
    // PRISONER_2
    prisonerInEducation(PRISONER_2.prisonerNumber)
    prisonerHasNeed(PRISONER_2.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_2.prisonerNumber, deadlineDate = today.plusDays(2))

    // ReviewDue
    // PRISONER_3
    prisonerInEducation(PRISONER_3.prisonerNumber)
    prisonerHasNeed(PRISONER_3.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_3.prisonerNumber, deadlineDate = today.plusDays(3))

    // ActivePlan
    // PRISONER_4
    prisonerInEducation(PRISONER_4.prisonerNumber)
    prisonerHasNeed(PRISONER_4.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_4.prisonerNumber, deadlineDate = today.plusWeeks(2))
    anElSPExists(PRISONER_4.prisonerNumber)

    // PlanOverDue
    // PRISONER_5
    prisonerInEducation(PRISONER_5.prisonerNumber)
    prisonerHasNeed(PRISONER_5.prisonerNumber)
    aValidPlanCreationScheduleExists(prisonNumber = PRISONER_5.prisonerNumber, deadlineDate = today.minusDays(5))

    // ReviewOverDue
    // PRISONER_6
    prisonerInEducation(PRISONER_6.prisonerNumber)
    prisonerHasNeed(PRISONER_6.prisonerNumber)
    aValidReviewScheduleExists(prisonNumber = PRISONER_6.prisonerNumber, deadlineDate = today.minusDays(6))

    // Inactive plan
    // PRISONER_7
    prisonerHasNeed(PRISONER_7.prisonerNumber)
    anElSPExists(PRISONER_7.prisonerNumber)

    // Declined plan
    // PRISONER_8
    prisonerHasNeed(PRISONER_8.prisonerNumber)
    aValidPlanCreationScheduleExists(
      prisonNumber = PRISONER_8.prisonerNumber,
      deadlineDate = today.minusDays(8),
      status = PlanCreationScheduleStatus.EXEMPT_PRISONER_NOT_COMPLY,
    )

    // No plan
    // PRISONER_9
    // no data in san
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
      .hasTotalElements(9)
      .currentPageHasNumberOfRecords(9)
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_1").hasReleaseDate(today.plusDays(1)) })
        .person(2, { it.hasSurname("PRISONER_3").hasReleaseDate(today.plusDays(10)) })
        .person(3, { it.hasSurname("PRISONER_9").hasReleaseDate(today.plusDays(20)) })
        .person(4, { it.hasSurname("PRISONER_5").hasReleaseDate(today.plusDays(30)) })
        .person(5, { it.hasSurname("PRISONER_4").hasReleaseDate(today.plusDays(50)) })
        .person(6, { it.hasSurname("PRISONER_7").hasReleaseDate(today.plusDays(60)) })
        .person(7, { it.hasSurname("PRISONER_6").hasReleaseDate(today.plusDays(80)) })
        .person(8, { it.hasSurname("PRISONER_8").hasReleaseDate(today.plusDays(90)) })
        .person(9, { it.hasSurname("PRISONER_2").hasNoReleaseDate() })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_2").hasNoReleaseDate() })
        .person(2, { it.hasSurname("PRISONER_8").hasReleaseDate(today.plusDays(90)) })
        .person(3, { it.hasSurname("PRISONER_6").hasReleaseDate(today.plusDays(80)) })
        .person(4, { it.hasSurname("PRISONER_7").hasReleaseDate(today.plusDays(60)) })
        .person(5, { it.hasSurname("PRISONER_4").hasReleaseDate(today.plusDays(50)) })
        .person(6, { it.hasSurname("PRISONER_5").hasReleaseDate(today.plusDays(30)) })
        .person(7, { it.hasSurname("PRISONER_9").hasReleaseDate(today.plusDays(20)) })
        .person(8, { it.hasSurname("PRISONER_3").hasReleaseDate(today.plusDays(10)) })
        .person(9, { it.hasSurname("PRISONER_1").hasReleaseDate(today.plusDays(1)) })
    }

    @Test
    fun `sort by deadline date ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "DEADLINE_DATE")
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_6").hasDeadlineDate(today.minusDays(6)) })
        .person(2, { it.hasSurname("PRISONER_5").hasDeadlineDate(today.minusDays(5)) })
        .person(3, { it.hasSurname("PRISONER_2").hasDeadlineDate(today.plusDays(2)) })
        .person(4, { it.hasSurname("PRISONER_3").hasDeadlineDate(today.plusDays(3)) })
        .person(5, { it.hasSurname("PRISONER_4").hasDeadlineDate(today.plusWeeks(2)) })
        .person(6, { it.hasSurname("PRISONER_1").hasNoDeadlineDate() })
        .person(7, { it.hasSurname("PRISONER_7").hasNoDeadlineDate() })
        .person(8, { it.hasSurname("PRISONER_8").hasNoDeadlineDate() })
        .person(9, { it.hasSurname("PRISONER_9").hasNoDeadlineDate() })
    }

    @Test
    fun `sort by deadline date descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "DEADLINE_DATE")
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_1").hasNoDeadlineDate() })
        .person(2, { it.hasSurname("PRISONER_7").hasNoDeadlineDate() })
        .person(3, { it.hasSurname("PRISONER_8").hasNoDeadlineDate() })
        .person(4, { it.hasSurname("PRISONER_9").hasNoDeadlineDate() })
        .person(5, { it.hasSurname("PRISONER_4").hasDeadlineDate(today.plusWeeks(2)) })
        .person(6, { it.hasSurname("PRISONER_3").hasDeadlineDate(today.plusDays(3)) })
        .person(7, { it.hasSurname("PRISONER_2").hasDeadlineDate(today.plusDays(2)) })
        .person(8, { it.hasSurname("PRISONER_5").hasDeadlineDate(today.minusDays(5)) })
        .person(9, { it.hasSurname("PRISONER_6").hasDeadlineDate(today.minusDays(6)) })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_1") })
        .person(2, { it.hasSurname("PRISONER_2") })
        .person(3, { it.hasSurname("PRISONER_3") })
        .person(4, { it.hasSurname("PRISONER_4") })
        .person(5, { it.hasSurname("PRISONER_5") })
        .person(6, { it.hasSurname("PRISONER_6") })
        .person(7, { it.hasSurname("PRISONER_7") })
        .person(8, { it.hasSurname("PRISONER_8") })
        .person(9, { it.hasSurname("PRISONER_9") })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_9") })
        .person(2, { it.hasSurname("PRISONER_8") })
        .person(3, { it.hasSurname("PRISONER_7") })
        .person(4, { it.hasSurname("PRISONER_6") })
        .person(5, { it.hasSurname("PRISONER_5") })
        .person(6, { it.hasSurname("PRISONER_4") })
        .person(7, { it.hasSurname("PRISONER_3") })
        .person(8, { it.hasSurname("PRISONER_2") })
        .person(9, { it.hasSurname("PRISONER_1") })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_3").hasCellLocation("A-1") })
        .person(2, { it.hasSurname("PRISONER_4").hasCellLocation("B-2") })
        .person(3, { it.hasSurname("PRISONER_8").hasCellLocation("C-1") })
        .person(4, { it.hasSurname("PRISONER_7").hasCellLocation("C-2") })
        .person(5, { it.hasSurname("PRISONER_2").hasCellLocation("Z-1") })
        .person(6, { it.hasSurname("PRISONER_5").hasCellLocation("Z-2") })
        .person(7, { it.hasSurname("PRISONER_1").hasCellLocation("Z-3") })
        .person(8, { it.hasSurname("PRISONER_9").hasCellLocation("Z-4") })
        .person(9, { it.hasSurname("PRISONER_6").hasNoCellLocation() })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_6").hasNoCellLocation() })
        .person(2, { it.hasSurname("PRISONER_9").hasCellLocation("Z-4") })
        .person(3, { it.hasSurname("PRISONER_1").hasCellLocation("Z-3") })
        .person(4, { it.hasSurname("PRISONER_5").hasCellLocation("Z-2") })
        .person(5, { it.hasSurname("PRISONER_2").hasCellLocation("Z-1") })
        .person(6, { it.hasSurname("PRISONER_7").hasCellLocation("C-2") })
        .person(7, { it.hasSurname("PRISONER_8").hasCellLocation("C-1") })
        .person(8, { it.hasSurname("PRISONER_4").hasCellLocation("B-2") })
        .person(9, { it.hasSurname("PRISONER_3").hasCellLocation("A-1") })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_7").isNotInEducation() })
        .person(2, { it.hasSurname("PRISONER_8").isNotInEducation() })
        .person(3, { it.hasSurname("PRISONER_9").isNotInEducation() })
        .person(4, { it.hasSurname("PRISONER_1").isInEducation() })
        .person(5, { it.hasSurname("PRISONER_2").isInEducation() })
        .person(6, { it.hasSurname("PRISONER_3").isInEducation() })
        .person(7, { it.hasSurname("PRISONER_4").isInEducation() })
        .person(8, { it.hasSurname("PRISONER_5").isInEducation() })
        .person(9, { it.hasSurname("PRISONER_6").isInEducation() })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_1").isInEducation() })
        .person(2, { it.hasSurname("PRISONER_2").isInEducation() })
        .person(3, { it.hasSurname("PRISONER_3").isInEducation() })
        .person(4, { it.hasSurname("PRISONER_4").isInEducation() })
        .person(5, { it.hasSurname("PRISONER_5").isInEducation() })
        .person(6, { it.hasSurname("PRISONER_6").isInEducation() })
        .person(7, { it.hasSurname("PRISONER_7").isNotInEducation() })
        .person(8, { it.hasSurname("PRISONER_8").isNotInEducation() })
        .person(9, { it.hasSurname("PRISONER_9").isNotInEducation() })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_9").doesNotHaveAdditionalNeed() })
        .person(2, { it.hasSurname("PRISONER_1").hasAdditionalNeed() })
        .person(3, { it.hasSurname("PRISONER_2").hasAdditionalNeed() })
        .person(4, { it.hasSurname("PRISONER_3").hasAdditionalNeed() })
        .person(5, { it.hasSurname("PRISONER_4").hasAdditionalNeed() })
        .person(6, { it.hasSurname("PRISONER_5").hasAdditionalNeed() })
        .person(7, { it.hasSurname("PRISONER_6").hasAdditionalNeed() })
        .person(8, { it.hasSurname("PRISONER_7").hasAdditionalNeed() })
        .person(9, { it.hasSurname("PRISONER_8").hasAdditionalNeed() })
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_1").hasAdditionalNeed() })
        .person(2, { it.hasSurname("PRISONER_2").hasAdditionalNeed() })
        .person(3, { it.hasSurname("PRISONER_3").hasAdditionalNeed() })
        .person(4, { it.hasSurname("PRISONER_4").hasAdditionalNeed() })
        .person(5, { it.hasSurname("PRISONER_5").hasAdditionalNeed() })
        .person(6, { it.hasSurname("PRISONER_6").hasAdditionalNeed() })
        .person(7, { it.hasSurname("PRISONER_7").hasAdditionalNeed() })
        .person(8, { it.hasSurname("PRISONER_8").hasAdditionalNeed() })
        .person(9, { it.hasSurname("PRISONER_9").doesNotHaveAdditionalNeed() })
    }

    @Test
    fun `sort by plan status ascending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "PLAN_STATUS")
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_4").hasPlanStatus(ACTIVE_PLAN) })
        .person(2, { it.hasSurname("PRISONER_2").hasPlanStatus(PLAN_DUE) })
        .person(3, { it.hasSurname("PRISONER_1").hasPlanStatus(NEEDS_PLAN) })
        .person(4, { it.hasSurname("PRISONER_3").hasPlanStatus(REVIEW_DUE) })
        .person(5, { it.hasSurname("PRISONER_5").hasPlanStatus(PLAN_OVERDUE) })
        .person(6, { it.hasSurname("PRISONER_6").hasPlanStatus(REVIEW_OVERDUE) })
        .person(7, { it.hasSurname("PRISONER_8").hasPlanStatus(PLAN_DECLINED) })
        .person(8, { it.hasSurname("PRISONER_7").hasPlanStatus(INACTIVE_PLAN) })
        .person(9, { it.hasSurname("PRISONER_9").hasPlanStatus(NO_PLAN) })
    }

    @Test
    fun `sort by plan status descending`() {
      // Given

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(DefaultUriBuilderFactory().expand(URI_TEMPLATE, PRISON_ID).path)
            .queryParam("sortBy", "PLAN_STATUS")
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
        .hasTotalElements(9)
        .person(1, { it.hasSurname("PRISONER_9").hasPlanStatus(NO_PLAN) })
        .person(2, { it.hasSurname("PRISONER_7").hasPlanStatus(INACTIVE_PLAN) })
        .person(3, { it.hasSurname("PRISONER_8").hasPlanStatus(PLAN_DECLINED) })
        .person(4, { it.hasSurname("PRISONER_6").hasPlanStatus(REVIEW_OVERDUE) })
        .person(5, { it.hasSurname("PRISONER_5").hasPlanStatus(PLAN_OVERDUE) })
        .person(6, { it.hasSurname("PRISONER_3").hasPlanStatus(REVIEW_DUE) })
        .person(7, { it.hasSurname("PRISONER_1").hasPlanStatus(NEEDS_PLAN) })
        .person(8, { it.hasSurname("PRISONER_2").hasPlanStatus(PLAN_DUE) })
        .person(9, { it.hasSurname("PRISONER_4").hasPlanStatus(ACTIVE_PLAN) })
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

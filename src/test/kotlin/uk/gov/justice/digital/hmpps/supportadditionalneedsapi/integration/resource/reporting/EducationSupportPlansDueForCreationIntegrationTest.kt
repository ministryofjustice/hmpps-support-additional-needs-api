package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource.reporting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ErrorResponse
import java.time.LocalDate
import java.util.UUID

class EducationSupportPlansDueForCreationIntegrationTest : IntegrationTestBase() {
  companion object {
    private const val URI_TEMPLATE = "/reports/education-support-plans-due-for-creation"
    private val TODAY = LocalDate.now()
    private val TOMORROW = TODAY.plusDays(1)
    private val YESTERDAY = TODAY.minusDays(1)
    private val NEXT_WEEK = TODAY.plusWeeks(1)
    private val LAST_WEEK = TODAY.minusWeeks(1)
    private val NEXT_MONTH = TODAY.plusMonths(1)
  }

  @BeforeEach
  fun setUp() {
    planCreationScheduleRepository.deleteAll()
  }

  @Nested
  inner class HappyPath {
    @Test
    fun `should return CSV with plans within the date range for included prisons when Accept header is text-csv`() {
      // Given
      val prisonNumber1 = randomValidPrisonNumber()
      val prisonNumber2 = randomValidPrisonNumber()
      val prisonNumber3 = randomValidPrisonNumber()

      // Plan within range, included prison
      val plan1 = createPlanCreationSchedule(
        prisonNumber = prisonNumber1,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // Plan within range, included prison
      val plan2 = createPlanCreationSchedule(
        prisonNumber = prisonNumber2,
        deadlineDate = TOMORROW,
        createdAtPrison = "HMP",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // Plan outside range (before)
      createPlanCreationSchedule(
        prisonNumber = prisonNumber3,
        deadlineDate = LAST_WEEK,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .header("Accept", "text/csv")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines()
      assertThat(lines[0]).isEqualTo("reference,prison_number,created_at_prison,deadline_date,status")
      assertThat(lines.filter { it.isNotBlank() }).hasSize(3) // header + 2 data rows

      // Check that both prison numbers are in the CSV
      assertThat(csvContent).contains(prisonNumber1)
      assertThat(csvContent).contains(prisonNumber2)
      assertThat(csvContent).doesNotContain(prisonNumber3)
    }

    @Test
    fun `should return JSON with plans within the date range for included prisons when Accept header is application-json`() {
      // Given
      val prisonNumber1 = randomValidPrisonNumber()
      val prisonNumber2 = randomValidPrisonNumber()
      val prisonNumber3 = randomValidPrisonNumber()

      // Plan within range, included prison
      val plan1 = createPlanCreationSchedule(
        prisonNumber = prisonNumber1,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // Plan within range, included prison
      val plan2 = createPlanCreationSchedule(
        prisonNumber = prisonNumber2,
        deadlineDate = TOMORROW,
        createdAtPrison = "HMP",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // Plan outside range (before)
      createPlanCreationSchedule(
        prisonNumber = prisonNumber3,
        deadlineDate = LAST_WEEK,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .header("Accept", "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("application/json")
        .expectBody()
        .jsonPath("$.length()").isEqualTo(2)
        .jsonPath("$[0].prison_number").isEqualTo(prisonNumber1)
        .jsonPath("$[0].deadline_date").isEqualTo(TODAY.toString())
        .jsonPath("$[0].created_at_prison").isEqualTo("MDI")
        .jsonPath("$[0].status").isEqualTo("SCHEDULED")
        .jsonPath("$[1].prison_number").isEqualTo(prisonNumber2)
        .jsonPath("$[1].deadline_date").isEqualTo(TOMORROW.toString())
        .jsonPath("$[1].created_at_prison").isEqualTo("HMP")
        .jsonPath("$[1].status").isEqualTo("SCHEDULED")
        .returnResult()
    }

    @Test
    fun `should return CSV when requested with format parameter`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()

      val plan = createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When - Using format=csv parameter
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.toString())
            .queryParam("format", "csv")
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull
      val lines = csvContent!!.lines()
      assertThat(lines[0]).isEqualTo("reference,prison_number,created_at_prison,deadline_date,status")
      assertThat(csvContent).contains(prisonNumber)
    }

    @Test
    fun `should return CSV with plans exactly on the boundary dates`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()

      val plan = createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines()
      assertThat(lines.filter { it.isNotBlank() }).hasSize(2)
      assertThat(csvContent).contains(prisonNumber)
      assertThat(csvContent).contains(plan.reference.toString())
    }
  }

  @Nested
  inner class ExcludedPrisons {
    @Test
    fun `should exclude plans from excluded prison codes`() {
      // Given
      val excludedPrisons = listOf(
        "ACI", "DGI", "FWI", "FBI", "PBI", "RHI", "WNI", "BWI", "BZI", "PRI",
        "UKI", "WYI", "ASI", "PFI", "UPI", "SWI", "PYI", "CFI", "CKI", "FYI",
        "FEI", "LGI", "MKI",
      )

      // Create a plan for each excluded prison
      excludedPrisons.forEach { prisonCode ->
        createPlanCreationSchedule(
          prisonNumber = randomValidPrisonNumber(),
          deadlineDate = TODAY,
          createdAtPrison = prisonCode,
          status = PlanCreationScheduleStatus.SCHEDULED,
        )
      }

      // Create one plan for an included prison
      val includedPrisonNumber = randomValidPrisonNumber()
      val includedPlan = createPlanCreationSchedule(
        prisonNumber = includedPrisonNumber,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines()
      assertThat(lines.filter { it.isNotBlank() }).hasSize(2)
      assertThat(csvContent).contains(includedPrisonNumber)
      assertThat(csvContent).contains(includedPlan.reference.toString())

      // Should not contain any of the excluded prison codes
      excludedPrisons.forEach { code ->
        assertThat(csvContent).doesNotContain(",$code,")
      }
    }
  }

  @Nested
  inner class EdgeCases {
    @Test
    fun `should exclude plans outside the date range`() {
      // Given - Create plans at various dates
      val yesterdayPlan = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = YESTERDAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      val todayPlan = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      val nextWeekPlan = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = NEXT_WEEK,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      val nextMonthPlan = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = NEXT_MONTH,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When - Query for only TODAY to NEXT_WEEK
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", NEXT_WEEK.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then - Should include only today and next week plans, not yesterday or next month
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines().filter { it.isNotBlank() }
      assertThat(lines).hasSize(3)

      // Check that the correct plans are included/excluded
      assertThat(csvContent).doesNotContain(yesterdayPlan.prisonNumber) // Before range
      assertThat(csvContent).contains(todayPlan.prisonNumber) // Start of range
      assertThat(csvContent).contains(nextWeekPlan.prisonNumber) // End of range
      assertThat(csvContent).doesNotContain(nextMonthPlan.prisonNumber) // After range
    }

    @Test
    fun `should return empty CSV when no plans match criteria`() {
      // Given
      createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = LAST_WEEK,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      // When the collection is empty, the response body is empty (could be null or empty string)
      assertThat(csvContent ?: "").isEmpty()
    }
  }

  @Nested
  inner class DifferentStatuses {
    @Test
    fun `should return CSV with plans having different statuses`() {
      // Given
      val statuses = listOf(
        PlanCreationScheduleStatus.SCHEDULED,
        PlanCreationScheduleStatus.COMPLETED,
        PlanCreationScheduleStatus.EXEMPT_SYSTEM_TECHNICAL_ISSUE,
        PlanCreationScheduleStatus.EXEMPT_NOT_IN_EDUCATION,
      )

      val createdPlans = statuses.map { status ->
        createPlanCreationSchedule(
          prisonNumber = randomValidPrisonNumber(),
          deadlineDate = TODAY,
          createdAtPrison = "MDI",
          status = status,
        )
      }

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines()
      assertThat(lines.filter { it.isNotBlank() }).hasSize(statuses.size + 1)

      // Check all statuses are present
      statuses.forEach { status ->
        assertThat(csvContent).contains(status.name)
      }
    }
  }

  @Nested
  inner class ValidationErrors {
    @Test
    fun `should return bad request when date range exceeds 60 days`() {
      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.plusDays(61).toString())
            .build()
        }
        .header("Accept", "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()

      // Then
      val actual = response.responseBody
      assertThat(actual).isNotNull
      assertThat(actual!!.userMessage).contains("Date range cannot exceed 60 days")
    }

    @Test
    fun `should allow date range of exactly 60 days`() {
      // Given
      val prisonNumber = randomValidPrisonNumber()
      createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY.plusDays(30),
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.plusDays(60).toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull
      assertThat(csvContent).contains("reference,prison_number,created_at_prison,deadline_date,status")
    }

    @Test
    fun `should use default fromDate when missing (toDate minus 14 days)`() {
      // Given - Create a plan within the default range
      val prisonNumber = randomValidPrisonNumber()
      createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY.minusDays(7), // Within default 14 day range
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When - Only provide toDate
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull
      assertThat(csvContent).contains(prisonNumber)
    }

    @Test
    fun `should use default toDate when missing (today)`() {
      // Given - Create a plan for today
      val prisonNumber = randomValidPrisonNumber()
      createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When - Only provide fromDate
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", YESTERDAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull
      assertThat(csvContent).contains(prisonNumber)
    }

    @Test
    fun `should use default dates when both are missing`() {
      // Given - Create a plan for today (within default range)
      val prisonNumber = randomValidPrisonNumber()
      createPlanCreationSchedule(
        prisonNumber = prisonNumber,
        deadlineDate = TODAY.minusDays(5), // Within default 14 day range
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When - No dates provided
      val response = webTestClient.get()
        .uri(URI_TEMPLATE)
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull
      assertThat(csvContent).contains(prisonNumber)
    }

    @Test
    fun `should return bad request when fromDate is after toDate`() {
      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TOMORROW.toString())
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .header("Accept", "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()

      // Then
      val actual = response.responseBody
      assertThat(actual).isNotNull
      assertThat(actual!!.userMessage).contains("fromDate must be before or equal to toDate")
    }

    @Test
    fun `should return bad request when date format is invalid`() {
      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", "invalid-date")
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .header("Accept", "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isBadRequest
        .expectBody(ErrorResponse::class.java)
        .returnResult()

      // Then
      val actual = response.responseBody
      assertThat(actual).isNotNull
      assertThat(actual!!.status).isEqualTo(HttpStatus.BAD_REQUEST.value())
    }
  }

  @Nested
  inner class Security {
    @Test
    fun `should return unauthorized when no bearer token`() {
      webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .exchange()
        .expectStatus()
        .isUnauthorized
    }

    @Test
    fun `should return forbidden when bearer token without required role`() {
      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .header("Accept", "application/json")
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
        .exchange()
        .expectStatus()
        .isForbidden
        .expectBody(ErrorResponse::class.java)
        .returnResult()

      // Then
      val actual = response.responseBody
      assertThat(actual).isNotNull
      assertThat(actual!!.status).isEqualTo(HttpStatus.FORBIDDEN.value())
    }

    @Test
    fun `should return OK when bearer token with ELSP_RW role`() {
      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TOMORROW.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      // When the collection is empty, the response body is empty (could be null or empty string)
      assertThat(csvContent ?: "").isEmpty()
    }
  }

  @Nested
  inner class OrderingAndDistinct {
    @Test
    fun `should return CSV with results ordered by deadline date and reference`() {
      // Given
      val earlierDate = TODAY
      val laterDate = TOMORROW

      val plan1 = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = laterDate,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      val plan2 = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = earlierDate,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      val plan3 = createPlanCreationSchedule(
        prisonNumber = randomValidPrisonNumber(),
        deadlineDate = earlierDate,
        createdAtPrison = "MDI",
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", earlierDate.toString())
            .queryParam("toDate", laterDate.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      val lines = csvContent!!.lines().filter { it.isNotBlank() }
      assertThat(lines).hasSize(4)

      // Extract dates from CSV lines
      val dataLines = lines.drop(1)
      val dates = dataLines.map { line ->
        line.split(",")[3]
      }

      // Check that dates are sorted
      assertThat(dates).isSorted()
    }
  }

  @Nested
  inner class SpecialCharacterHandling {
    @Test
    fun `should properly escape special characters in CSV output`() {
      // Given - Create a plan with special characters that could break CSV formatting
      // Using valid prison number format but with prison codes that contain special chars
      val validPrisonNumber = randomValidPrisonNumber()
      val prisonWithComma = "M,I"
      val prisonWithQuote = "H\"P"

      val plan = createPlanCreationSchedule(
        prisonNumber = validPrisonNumber,
        deadlineDate = TODAY,
        createdAtPrison = prisonWithComma,
        status = PlanCreationScheduleStatus.SCHEDULED,
      )

      // When
      val response = webTestClient.get()
        .uri { uriBuilder ->
          uriBuilder
            .path(URI_TEMPLATE)
            .queryParam("fromDate", TODAY.toString())
            .queryParam("toDate", TODAY.toString())
            .build()
        }
        .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
        .exchange()
        .expectStatus()
        .isOk
        .expectHeader()
        .contentType("text/csv")
        .expectBody(String::class.java)
        .returnResult()

      // Then
      val csvContent = response.responseBody
      assertThat(csvContent).isNotNull

      // Parse CSV properly to verify escaping works
      val lines = csvContent!!.lines()
      assertThat(lines[0]).isEqualTo("reference,prison_number,created_at_prison,deadline_date,status")

      // The CSV should contain the special characters properly escaped
      // Jackson CSV should wrap fields with special chars in quotes
      assertThat(csvContent).contains("\"M,I\"")
      assertThat(csvContent).contains(validPrisonNumber)

      // Verify the CSV is still parseable
      assertThat(lines.filter { it.isNotBlank() }).hasSize(2)
    }
  }

  private fun createPlanCreationSchedule(
    prisonNumber: String,
    deadlineDate: LocalDate,
    createdAtPrison: String,
    status: PlanCreationScheduleStatus,
    reference: UUID = UUID.randomUUID(),
  ): PlanCreationScheduleEntity {
    val entity = PlanCreationScheduleEntity(
      prisonNumber = prisonNumber,
      deadlineDate = deadlineDate,
      earliestStartDate = deadlineDate.minusDays(14),
      status = status,
      createdAtPrison = createdAtPrison,
      updatedAtPrison = createdAtPrison,
      reference = reference,
    )
    return planCreationScheduleRepository.save(entity)
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.InjectMocks
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.given
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays.BankHoliday
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays.BankHolidays
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays.BankHolidaysApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays.RegionBankHolidays
import java.time.LocalDate

@ExtendWith(MockitoExtension::class)
class GovUkBankHolidaysWorkingDayServiceTest {
  @InjectMocks
  private lateinit var service: GovUkBankHolidaysWorkingDayService

  @Mock
  private lateinit var bankHolidaysApiClient: BankHolidaysApiClient

  @ParameterizedTest(name = "[{index}] {0}")
  @MethodSource("dateCalculationTestCases")
  fun `should calculate next working day given a date and number of days to add`(testCase: DateCalculationTestCase) {
    // Given
    setupMockBankHolidays()

    // When
    val actual = service.getNextWorkingDayNDaysFromDate(testCase.numberOfDays, testCase.fromDate)

    // Then
    assertThat(actual).isEqualTo(testCase.expectedDate)
  }

  @ParameterizedTest
  @MethodSource("workingDayTestCases")
  fun `should determine if working day`(date: LocalDate, expected: Boolean) {
    // Given
    setupMockBankHolidays()

    // When
    val actual = service.isWorkingDay(date)

    // Then
    assertThat(actual).isEqualTo(expected)
  }

  private fun setupMockBankHolidays() {
    val bankHolidays = BankHolidays(
      englandAndWales = RegionBankHolidays(
        division = "england-and-wales",
        events = bankHolidays.map { BankHoliday(title = "", date = it, notes = "", bunting = false) },
      ),
      scotland = RegionBankHolidays(
        division = "scotland",
        events = emptyList(),
      ),
      northernIreland = RegionBankHolidays(
        division = "northern-ireland",
        events = emptyList(),
      ),
    )
    given(bankHolidaysApiClient.getBankHolidays()).willReturn(bankHolidays)
  }

  companion object {
    private val bankHolidays = listOf(
      LocalDate.parse("2023-12-25"), // Monday
      LocalDate.parse("2023-12-26"), // Tuesday
      LocalDate.parse("2024-01-01"), // Monday
      LocalDate.parse("2026-12-25"), // Friday
      LocalDate.parse("2026-12-28"), // Monday (Boxing Day Bank Holiday, as 2026-12-26 falls on a Saturday
    )

    @JvmStatic
    fun dateCalculationTestCases(): List<DateCalculationTestCase> = listOf(
      DateCalculationTestCase(
        description = "2 working days from Friday December 22nd",
        fromDate = LocalDate.parse("2023-12-22"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "2 working days from Saturday December 23rd",
        fromDate = LocalDate.parse("2023-12-22"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "2 working days from Sunday December 23th 2023",
        fromDate = LocalDate.parse("2023-12-22"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "2 working days from Christmas day 2023",
        fromDate = LocalDate.parse("2023-12-25"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "2 working days from Boxing day 2023",
        fromDate = LocalDate.parse("2023-12-26"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "5 working days from Wednesday December 27th 2023",
        fromDate = LocalDate.parse("2023-12-27"),
        numberOfDays = 5,
        expectedDate = LocalDate.parse("2024-01-04"),
      ),
      DateCalculationTestCase(
        description = "0 working days from Wednesday December 27th 2023",
        fromDate = LocalDate.parse("2023-12-27"),
        numberOfDays = 0,
        expectedDate = LocalDate.parse("2023-12-27"),
      ),
      DateCalculationTestCase(
        description = "1 working day from Wednesday December 27th 2023",
        fromDate = LocalDate.parse("2023-12-27"),
        numberOfDays = 1,
        expectedDate = LocalDate.parse("2023-12-28"),
      ),
      DateCalculationTestCase(
        description = "2 working days from Wednesday December 27th 2023",
        fromDate = LocalDate.parse("2023-12-27"),
        numberOfDays = 2,
        expectedDate = LocalDate.parse("2023-12-29"),
      ),
      DateCalculationTestCase(
        description = "5 working days from Monday January 8th 2024",
        fromDate = LocalDate.parse("2024-01-08"),
        numberOfDays = 5,
        expectedDate = LocalDate.parse("2024-01-15"),
      ),
      DateCalculationTestCase(
        description = "3 working days from Thursday July 3th 2025",
        fromDate = LocalDate.parse("2025-07-03"),
        numberOfDays = 3,
        expectedDate = LocalDate.parse("2025-07-08"),
      ),
      DateCalculationTestCase(
        description = "3 working days from Wednesday December 23rd 2026",
        fromDate = LocalDate.parse("2026-12-23"),
        numberOfDays = 3,
        expectedDate = LocalDate.parse("2026-12-30"),
      ),
      DateCalculationTestCase(
        description = "5 working days from Monday July 7th 2025",
        fromDate = LocalDate.parse("2025-07-07"),
        numberOfDays = 5,
        expectedDate = LocalDate.parse("2025-07-14"),
      ),
    )

    @JvmStatic
    fun workingDayTestCases(): List<Arguments> = listOf(
      Arguments.of(
        "2025-07-03", // Wednesday July 3rd 2025
        true,
      ),
      Arguments.of(
        "2025-07-05", // Saturday July 5th 2025
        false,
      ),
      Arguments.of(
        "2026-12-25", // Christmas Day (Friday)
        false,
      ),
      Arguments.of(
        "2026-12-26", // Boxing Day (Saturday, so not defined as a Bank Holiday)
        false,
      ),
      Arguments.of(
        "2026-12-27", // Substitute Boxing Day Bank Holiday (Monday)
        false,
      ),
    )
  }

  data class DateCalculationTestCase(
    val description: String,
    val fromDate: LocalDate,
    val numberOfDays: Long,
    val expectedDate: LocalDate,
  ) {
    override fun toString() = description
  }
}

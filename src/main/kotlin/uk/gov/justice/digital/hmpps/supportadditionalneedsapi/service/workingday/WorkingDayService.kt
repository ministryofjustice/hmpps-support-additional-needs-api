package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday

import java.time.LocalDate

/**
 * Interface defining [LocalDate] related methods related to the concept of a Working Day; where a Working Day
 * is expected to be a Monday to Friday inclusive, and not a Bank Holiday.
 */
interface WorkingDayService {

  /**
   * Returns the [LocalDate] of the next Working Day 'n' days from today.
   *
   * For example, on the assumption that there are no Bank Holidays during this period, if today were Thursday 3rd July 2025,
   * given the specified number of days was 3, the expected return value would be Tuesday 8th July 2025.
   */
  fun getNextWorkingDayNDaysFromToday(numberOfDays: Long): LocalDate

  /**
   * Returns the [LocalDate] of the next Working Day 'n' days from the specified date.
   *
   * For example, Christmas Day 2026 is Friday 25th December 2026. Boxing Day is Saturday 26th December 2026 but the Bank Holiday
   * for it is Monday 28th December 2026.
   * Given the specified date is Wednesday 23rd December 2026 and the number of days was 3, the expected return value would
   * be Wednesday 30th December 2026.
   */
  fun getNextWorkingDayNDaysFromDate(numberOfDays: Long, fromDate: LocalDate): LocalDate

  /**
   * Returns true if the specified [LocalDate] is a Working Day.
   */
  fun isWorkingDay(date: LocalDate): Boolean
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.workingday

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.bankholidays.BankHolidaysApiClient
import java.time.LocalDate

/**
 * Implementation of [WorkingDayService] backed by the Bank Holidays data from the GovUK Bank Holidays service.
 */
@Service
class GovUkBankHolidaysWorkingDayService(private val bankHolidaysApiClient: BankHolidaysApiClient) : WorkingDayService {

  override fun getNextWorkingDayNDaysFromToday(numberOfDays: Long): LocalDate = getNextWorkingDayNDaysFromDate(numberOfDays, LocalDate.now())

  override fun getNextWorkingDayNDaysFromDate(numberOfDays: Long, fromDate: LocalDate): LocalDate {
    // TODO - check with BAs - assume we are only concerned with calculating dates with consideration of Bank Holidays in England and Wales
    // Scotland and NI have different Bank Holidays, so could end up adding some significant complexity here
    val englandBankHolidays: List<LocalDate> = bankHolidaysApiClient.getBankHolidays() //
      .englandAndWales
      .events
      .map { it.date }

    var nextWorkingDate = fromDate
    repeat(numberOfDays.toInt()) {
      do {
        nextWorkingDate = nextWorkingDate.plusDays(1)
      } while (!nextWorkingDate.isWorkingDay(englandBankHolidays))
    }
    return nextWorkingDate
  }

  override fun isWorkingDay(date: LocalDate): Boolean = date.isWorkingDay(bankHolidaysApiClient.getBankHolidays().englandAndWales.events.map { it.date })

  private fun LocalDate.isWorkingDay(bankHolidays: List<LocalDate>) = this.isWeekDay() && !bankHolidays.contains(this)

  private fun LocalDate.isWeekDay() = dayOfWeek.value in 1..5
}

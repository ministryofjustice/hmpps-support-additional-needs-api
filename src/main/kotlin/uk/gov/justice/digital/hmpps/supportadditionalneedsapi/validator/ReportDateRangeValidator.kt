package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraintvalidation.SupportedValidationTarget
import jakarta.validation.constraintvalidation.ValidationTarget
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.reflect.KClass

/**
 * Validator to ensure date range does not exceed a maximum number of days
 */
@SupportedValidationTarget(ValidationTarget.PARAMETERS)
class ReportDateRangeValidator : ConstraintValidator<ValidDateRange, Array<Any>> {

  private var maxDays: Long = 60

  override fun initialize(constraintAnnotation: ValidDateRange) {
    this.maxDays = constraintAnnotation.maxDays
  }

  override fun isValid(
    parameters: Array<Any>,
    context: ConstraintValidatorContext,
  ): Boolean {
    // Expect parameters to be: [controller instance, fromDate, toDate]
    // The first LocalDate should be fromDate, the second should be toDate
    val dates = parameters.filterIsInstance<LocalDate>()

    if (dates.size < 2) {
      return true // missing params handled elsewhere
    }

    val fromDate = dates[0]
    val toDate = dates[1]

    return ChronoUnit.DAYS.between(fromDate, toDate) <= maxDays
  }
}

@MustBeDocumented
@Constraint(validatedBy = [ReportDateRangeValidator::class])
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ValidDateRange(
  val message: String = "Date range cannot exceed {maxDays} days",
  val maxDays: Long = 60,
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

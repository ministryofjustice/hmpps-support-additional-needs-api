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
    // Parameters array contains the method parameters
    // Filter for LocalDate instances (which can be null)
    val fromDateParam = parameters.getOrNull(0) as? LocalDate?
    val toDateParam = parameters.getOrNull(1) as? LocalDate?

    // Apply defaults if null
    val toDate = toDateParam ?: LocalDate.now()
    val fromDate = fromDateParam ?: toDate.minusDays(14)

    // Check if fromDate is after toDate
    if (fromDate.isAfter(toDate)) {
      context.disableDefaultConstraintViolation()
      context.buildConstraintViolationWithTemplate("fromDate must be before or equal to toDate")
        .addConstraintViolation()
      return false
    }

    // Check if date range exceeds max days
    if (ChronoUnit.DAYS.between(fromDate, toDate) > maxDays) {
      return false
    }

    return true
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

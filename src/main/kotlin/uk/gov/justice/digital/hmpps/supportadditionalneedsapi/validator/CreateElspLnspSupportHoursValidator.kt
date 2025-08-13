package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraintvalidation.SupportedValidationTarget
import jakarta.validation.constraintvalidation.ValidationTarget
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import kotlin.reflect.KClass

/**
 * Hibernate Constraint Validator class and annotation ta validate that the LNSP Support Hours is populated if
 * LNSP Support is populated
 */
@SupportedValidationTarget(ValidationTarget.PARAMETERS)
class CreateElspLnspSupportHoursValidator : ConstraintValidator<LnspSupportHoursSpecifiedWhenLnspSupportSpecified, Array<Any>> {

  override fun isValid(
    parameters: Array<Any>,
    context: ConstraintValidatorContext,
  ): Boolean {
    val request = parameters.find { it is CreateEducationSupportPlanRequest } as? CreateEducationSupportPlanRequest
      ?: return false // parameter wasn't found, this will be already have been trapped by the controller.

    request.lnspSupport
    return if (request.lnspSupport != null) {
      request.lnspSupportHours != null
    } else {
      true
    }
  }
}

@MustBeDocumented
@Constraint(validatedBy = [CreateElspLnspSupportHoursValidator::class])
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class LnspSupportHoursSpecifiedWhenLnspSupportSpecified(
  val message: String = "LNSP Support Hours must be specified if LNSP is populated",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

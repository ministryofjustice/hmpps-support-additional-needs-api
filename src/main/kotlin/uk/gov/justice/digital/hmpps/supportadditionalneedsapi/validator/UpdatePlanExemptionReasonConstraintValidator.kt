package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator

import jakarta.validation.Constraint
import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import jakarta.validation.Payload
import jakarta.validation.constraintvalidation.SupportedValidationTarget
import jakarta.validation.constraintvalidation.ValidationTarget
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationUpdateStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdatePlanCreationStatusRequest
import kotlin.reflect.KClass

/**
 * Hibernate Constraint Validator class and [] annotation to validate that the Exemption reason is populated or not populated
 * depending on whether the status is an exemption status
 */
@SupportedValidationTarget(ValidationTarget.PARAMETERS)
class UpdatePlanExemptionReasonConstraintValidator : ConstraintValidator<ReasonSpecifiedForExemptionStatusRequest, Array<Any>> {

  override fun isValid(
    parameters: Array<Any>,
    context: ConstraintValidatorContext,
  ): Boolean {
    val request = parameters.find { it is UpdatePlanCreationStatusRequest } as? UpdatePlanCreationStatusRequest
      ?: return false // parameter wasn't found, this will be already have been trapped by the controller.

    return request.status in exemptStatuses()
  }
}

private fun exemptStatuses(): Set<PlanCreationUpdateStatus> = setOf(
  PlanCreationUpdateStatus.EXEMPT_PRISONER_NOT_COMPLY,
)

@MustBeDocumented
@Constraint(validatedBy = [UpdatePlanExemptionReasonConstraintValidator::class])
@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class ReasonSpecifiedForExemptionStatusRequest(
  val message: String = "Reason must be specified for Exemptions",
  val groups: Array<KClass<*>> = [],
  val payload: Array<KClass<out Payload>> = [],
)

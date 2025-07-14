package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import java.util.*

interface InvalidUserRequest {
  val name: String
  val value: String
}

data class InvalidInputException(override val name: String, override val value: String) :
  IllegalArgumentException("$name is invalid"),
  InvalidUserRequest

data class NotActiveException(override val name: String, override val value: String) :
  IllegalArgumentException("$name is not active"),
  InvalidUserRequest

data class MultipleInvalidException(override val name: String, override val value: String) :
  IllegalArgumentException("Multiple invalid $name"),
  InvalidUserRequest

class DuplicateConditionException(prisonNumber: String, conditions: String) : RuntimeException("Attempted to add duplicate condition(s) $conditions for prisoner [$prisonNumber]")

class ConditionNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Condition with reference [$reference] not found for prisoner [$prisonNumber]")

class DuplicateChallengeException(prisonNumber: String, conditions: String) : RuntimeException("Attempted to add duplicate challenge(s) $conditions for prisoner [$prisonNumber]")

class DuplicateStrengthException(prisonNumber: String, strengths: String) : RuntimeException("Attempted to add duplicate strength(s) $strengths for prisoner [$prisonNumber]")

class ChallengeNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Challenge with reference [$reference] not found for prisoner [$prisonNumber]")

class StrengthNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Strength with reference [$reference] not found for prisoner [$prisonNumber]")

class PersonAlreadyHasAPlanException(prisonNumber: String) : RuntimeException("Prisoner [$prisonNumber] already has a plan")

class PlanNotFoundException(prisonNumber: String) : RuntimeException("ELSP plan not found for prisoner [$prisonNumber]")

class PlanCreationScheduleNotFoundException(prisonNumber: String) : RuntimeException("Plan creation schedule not found for prisoner [$prisonNumber]")

class PlanCreationScheduleStateException(
  prisonNumber: String,
  status: PlanCreationScheduleStatus,
  existingStatus: PlanCreationScheduleStatus,
) : RuntimeException("Plan creation schedule status must be [$status] but was [$existingStatus] for prisoner [$prisonNumber]")

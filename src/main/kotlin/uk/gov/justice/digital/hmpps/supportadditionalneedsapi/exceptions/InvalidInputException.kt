package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions

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

class ChallengeNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Challenge with reference [$reference] not found for prisoner [$prisonNumber]")

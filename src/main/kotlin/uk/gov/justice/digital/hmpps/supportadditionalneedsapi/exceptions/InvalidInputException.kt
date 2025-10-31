package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleStatus
import java.util.*

interface InvalidUserRequest {
  val name: String
  val value: String
}

data class InvalidInputException(override val name: String, override val value: String) :
  IllegalArgumentException("Domain $name with code $value is invalid"),
  InvalidUserRequest

data class NotActiveException(override val name: String, override val value: String) :
  IllegalArgumentException("Domain $name with code $value is not active"),
  InvalidUserRequest

data class MultipleInvalidException(override val name: String, override val value: String) :
  IllegalArgumentException("Multiple invalid $name"),
  InvalidUserRequest

class ConditionNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Condition with reference [$reference] not found for prisoner [$prisonNumber]")

class SupportStrategyNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Support Strategy with reference [$reference] not found for prisoner [$prisonNumber]")

class SupportStrategyArchivedException(prisonNumber: String, reference: UUID) : RuntimeException("Support Strategy with reference [$reference] has been archived for prisoner [$prisonNumber]")

class ChallengeNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Challenge with reference [$reference] not found for prisoner [$prisonNumber]")

class StrengthNotFoundException(prisonNumber: String, reference: UUID) : RuntimeException("Strength with reference [$reference] not found for prisoner [$prisonNumber]")

class PersonAlreadyHasAPlanException(prisonNumber: String) : RuntimeException("Prisoner [$prisonNumber] already has a plan")

class PlanNotFoundException(prisonNumber: String) : RuntimeException("ELSP plan not found for prisoner [$prisonNumber]")

class PlanCreationScheduleNotFoundException(prisonNumber: String) : RuntimeException("Plan creation schedule not found for prisoner [$prisonNumber]")

class CannotCompleteReviewWithNoSchedule(prisonNumber: String) : RuntimeException("Attempted to create a Review with no schedule for prisoner [$prisonNumber]")

class PlanCreationScheduleStateException(
  prisonNumber: String,
  statuses: List<PlanCreationScheduleStatus>,
  existingStatus: PlanCreationScheduleStatus,
) : RuntimeException("Plan creation schedule status must be one of [$statuses] but was [$existingStatus] for prisoner [$prisonNumber]")

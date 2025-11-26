package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository.projections

import java.time.Instant
import java.util.UUID

interface ConditionProjection {
  val id: UUID
  val prisonNumber: String
  val conditionTypeId: UUID
  val conditionTypeDescription: String?
  val description: String?
  val medication: String?
  val active: Boolean
  val archiveReason: String?
  val createdAtPrison: String
  val updatedAtPrison: String
  val createdAt: Instant
  val updatedAt: Instant
  val createdBy: String?
  val updatedBy: String?
}

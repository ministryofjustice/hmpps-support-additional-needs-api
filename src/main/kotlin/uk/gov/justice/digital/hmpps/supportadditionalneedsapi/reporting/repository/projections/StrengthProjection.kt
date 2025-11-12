package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository.projections

import java.time.Instant
import java.util.UUID

interface StrengthProjection {
  val id: UUID
  val prisonNumber: String
  val strengthTypeId: UUID
  val strengthTypeDescription: String?
  val description: String?
  val active: Boolean
  val archiveReason: String?
  val createdAtPrison: String
  val updatedAtPrison: String
  val createdAt: Instant
  val updatedAt: Instant
  val createdBy: String?
  val updatedBy: String?
  val alnScreenerId: UUID?
  val fromAlnScreener: Boolean
}

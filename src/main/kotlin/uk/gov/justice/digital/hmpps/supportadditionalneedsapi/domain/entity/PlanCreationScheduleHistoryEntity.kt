package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.UuidGenerator
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents an immutable history record of the schedule for when a prisoner's plan must be completed by.
 */
@Table(name = "plan_creation_schedule_history")
@Entity
data class PlanCreationScheduleHistoryEntity(

  @Column(updatable = false)
  val reference: UUID?,

  @Column(updatable = false)
  val version: Int,

  @Column(updatable = false)
  val prisonNumber: String,

  @Column(updatable = false)
  val deadlineDate: LocalDate,

  @Column(updatable = false)
  @Enumerated(value = EnumType.STRING)
  val status: PlanCreationScheduleStatus,

  @Column(updatable = false)
  val exemptionReason: String?,

  @Column(updatable = false)
  val createdBy: String,

  @Column(updatable = false)
  val createdAt: Instant,

  @Column(updatable = false)
  val updatedBy: String,

  @Column(updatable = false)
  val updatedAt: Instant,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column(updatable = false)
  val updatedAtPrison: String,
) {
  @Id
  @GeneratedValue
  @UuidGenerator
  var id: UUID? = null
}

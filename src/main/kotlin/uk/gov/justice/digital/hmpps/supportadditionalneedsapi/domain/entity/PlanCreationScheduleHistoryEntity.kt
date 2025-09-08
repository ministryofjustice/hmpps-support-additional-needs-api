package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Represents an immutable history record of the schedule for when a prisoner's plan must be completed by.
 */
@Table(name = "plan_creation_schedule_history")
@Entity
@Immutable
data class PlanCreationScheduleHistoryEntity(

  @Column
  val reference: UUID,

  @Column(name = "prison_number")
  val prisonNumber: String,

  @Column(name = "deadline_date")
  val deadlineDate: LocalDate,

  @Column(name = "earliest_start_date")
  val earliestStartDate: LocalDate?,

  @Column
  @Enumerated(value = EnumType.STRING)
  val status: PlanCreationScheduleStatus,

  @Column(name = "exemption_reason")
  val exemptionReason: String?,

  @Column(name = "exemption_detail")
  val exemptionDetail: String?,

  @Column(name = "need_sources")
  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  val needSources: Set<NeedSource> = emptySet(),

  @Column(name = "created_by")
  val createdBy: String,

  @Column(name = "created_at")
  val createdAt: Instant,

  @Column(name = "updated_by")
  val updatedBy: String,

  @Column(name = "updated_at")
  val updatedAt: Instant,

  @Column(name = "created_at_prison")
  val createdAtPrison: String,

  @Column(name = "updated_at_prison")
  val updatedAtPrison: String,

  @Column(name = "version")
  val version: Int? = null,

  @EmbeddedId
  val id: PlanCreationScheduleHistoryEntityKey,
)

@Embeddable
data class PlanCreationScheduleHistoryEntityKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "id")
  val id: UUID,
)

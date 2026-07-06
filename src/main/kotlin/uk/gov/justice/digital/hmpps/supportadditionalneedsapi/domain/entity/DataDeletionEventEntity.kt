package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "data_deletion_event")
data class DataDeletionEventEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(updatable = false)
  val correlationId: UUID,

  @Column(updatable = false)
  @Enumerated(value = EnumType.STRING)
  val reason: DeletionReason,

  @Column(updatable = false)
  val dataDeletedAtPrison: String,
) {
  @Id
  @GeneratedValue
  @UuidGenerator
  var id: UUID? = null

  // Property to record the timestamp that this entity was created, hence the annotation.
  // Semantically the property says when the Data Deletion Event happened, hence the property name.
  @Column(updatable = false)
  @CreatedDate
  var dataDeletedAt: Instant? = null

  // Property to record who this entity was created by, hence the annotation.
  // Semantically the property says who performed the Data Deletion Event happened, hence the property name.
  @Column(updatable = false)
  @CreatedBy
  var dataDeletedBy: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as DataDeletionEventEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

enum class DeletionReason {
  DATA_PROCESSING_OBJECTION,
  ENTERED_IN_ERROR,
}

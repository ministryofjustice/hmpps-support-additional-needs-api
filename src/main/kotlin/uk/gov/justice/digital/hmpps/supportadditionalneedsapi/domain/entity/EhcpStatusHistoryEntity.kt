package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.*

@Entity
@Immutable
@Table(name = "ehcp_status_history")
data class EhcpStatusHistoryEntity(
  @Column(name = "prison_number")
  val prisonNumber: String,

  @Column(name = "has_current_ehcp", nullable = false)
  val hasCurrentEhcp: Boolean = false,

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

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

  @EmbeddedId
  val id: EhcpStatusHistoryEntityKey,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as EhcpStatusHistoryEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

@Embeddable
data class EhcpStatusHistoryEntityKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "id")
  val id: UUID,
)

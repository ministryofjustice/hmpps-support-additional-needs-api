package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "ehcp_status")
@Audited(withModifiedFlag = false)
data class EhcpStatusEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  val hasCurrentEhcp: Boolean = false,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @Id
  @Column
  val id: UUID = UUID.randomUUID(),

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @CreatedBy
  @Column(updatable = false)
  var createdBy: String? = null,

  @CreationTimestamp
  @Column
  var createdAt: Instant? = Instant.now(),

  @LastModifiedBy
  @Column
  var updatedBy: String? = null,

  @UpdateTimestamp
  @Column
  var updatedAt: Instant? = null,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as EhcpStatusEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

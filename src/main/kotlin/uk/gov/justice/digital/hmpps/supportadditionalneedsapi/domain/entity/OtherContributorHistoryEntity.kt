package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinColumns
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.*

@Entity
@Immutable
@Table(name = "other_contributor_history")
class OtherContributorHistoryEntity(

  @Column(length = 200, nullable = false)
  val name: String,

  @Column(name = "job_role")
  val jobRole: String,

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumns(
    JoinColumn(
      name = "rev_id",
      referencedColumnName = "rev_id",
      nullable = false,
      insertable = false,
      updatable = false,
    ),
    JoinColumn(name = "elsp_plan_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
  )
  val plan: ElspPlanHistoryEntity,

  @EmbeddedId
  val id: OtherContributorHistoryEntityKey,

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
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as OtherContributorHistoryEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, name = $name)"
}

@Embeddable
data class OtherContributorHistoryEntityKey(
  @Column(name = "rev_id") val revisionNumber: Long,
  @Column(name = "elsp_plan_id") val planId: UUID,
  @Column(name = "id") val id: UUID,
)

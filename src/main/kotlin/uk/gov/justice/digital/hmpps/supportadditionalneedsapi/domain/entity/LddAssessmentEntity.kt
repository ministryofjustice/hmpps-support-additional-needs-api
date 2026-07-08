package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.support.AuditingEntityListener

/**
 * Entity to record whether the legacy screener in
 * Curious identified if the person has a need.
 */
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "ldd_assessment")
data class LddAssessmentEntity(

  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  var hasNeed: Boolean = false,
) : BaseAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as LddAssessmentEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

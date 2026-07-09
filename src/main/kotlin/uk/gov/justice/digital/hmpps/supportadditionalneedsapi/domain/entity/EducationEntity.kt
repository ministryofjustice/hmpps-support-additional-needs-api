package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.UUID

/**
 * Entity to record the in/out of education information that we will
 * receive from Curious.
 */
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "education")
data class EducationEntity(

  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  val inEducation: Boolean = false,

  /**
   * This is the message reference we will receive when curious send us this information.
   * Nullable because although we might get this from a message
   * we may also manually update the record by calling the api directly
   */

  @Column(nullable = false)
  val curiousReference: UUID? = null,
) : BaseAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as EducationEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

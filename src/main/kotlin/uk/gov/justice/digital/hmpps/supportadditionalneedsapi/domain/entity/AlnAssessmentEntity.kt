package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

/**
 * Entity to record whether the ALN screener in
 * Curious identified if the person has a need.
 */
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "aln_assessment")
data class AlnAssessmentEntity(
  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  val hasNeed: Boolean = false,

  /**
   * This is the message reference we will receive when curious send us this information.
   * Nullable because although we might get this from a message
   * we may also manually update the record by calling the api directly
   */

  @Column(nullable = false)
  val curiousReference: UUID? = null,
) {
  @Id
  @GeneratedValue
  @UuidGenerator
  var id: UUID? = null

  @Column(updatable = false)
  @CreatedBy
  var createdBy: String? = null

  @Column(updatable = false)
  @CreationTimestamp
  var createdAt: Instant? = null

  @Column
  @LastModifiedBy
  var updatedBy: String? = null

  @Column
  @UpdateTimestamp
  var updatedAt: Instant? = null
}

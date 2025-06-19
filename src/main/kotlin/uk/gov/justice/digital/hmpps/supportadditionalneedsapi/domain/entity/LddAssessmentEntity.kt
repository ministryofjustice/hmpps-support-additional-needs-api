package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
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
  val hasNeed: Boolean = false,
) : BaseAuditableEntity()

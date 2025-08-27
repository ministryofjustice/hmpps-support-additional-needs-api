package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
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
@Table(name = "elsp_review")
@Audited(withModifiedFlag = false)
data class ElspReviewEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(length = 200)
  val reviewCreatedByName: String? = null,

  @Column(length = 200)
  val reviewCreatedByJobRole: String? = null,

  @Column(nullable = false)
  val prisonerDeclinedFeedback: Boolean = false,

  @Column
  val prisonerFeedback: String? = null,

  @Column
  val reviewerFeedback: String? = null,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @OneToMany(mappedBy = "elspReview", cascade = [CascadeType.ALL], orphanRemoval = true)
  val otherContributors: MutableList<OtherReviewContributorEntity> = mutableListOf(),

  @Column(updatable = false)
  val reviewScheduleReference: UUID,

  @Id
  @Column
  val id: UUID = UUID.randomUUID(),

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @CreatedBy
  @Column(updatable = false)
  var createdBy: String? = null,

  @CreationTimestamp
  @Column(updatable = false)
  var createdAt: Instant? = null,

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
    other as ElspReviewEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

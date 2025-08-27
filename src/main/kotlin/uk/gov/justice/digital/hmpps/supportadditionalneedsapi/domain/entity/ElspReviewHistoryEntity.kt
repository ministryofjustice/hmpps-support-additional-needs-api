package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "elsp_review_history")
data class ElspReviewHistoryEntity(
  @Column(name = "prison_number", updatable = false)
  val prisonNumber: String,

  @Column(name = "review_created_by_name", length = 200)
  val reviewCreatedByName: String? = null,

  @Column(name = "review_created_by_job_role", length = 200)
  val reviewCreatedByJobRole: String? = null,

  @Column(name = "prisoner_declined_feedback", nullable = false)
  val prisonerDeclinedFeedback: Boolean = false,

  @Column(name = "prisoner_feedback")
  val prisonerFeedback: String? = null,

  @Column(name = "reviewer_feedback")
  val reviewerFeedback: String? = null,

  @OneToMany(mappedBy = "elspReview")
  val otherContributors: List<OtherReviewContributorHistoryEntity> = emptyList(),

  @Column(name = "review_schedule_reference", updatable = false)
  val reviewScheduleReference: UUID,

  @EmbeddedId
  val id: ElspReviewHistoryEntityKey,

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
    other as ElspReviewHistoryEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

@Embeddable
data class ElspReviewHistoryEntityKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "id")
  val id: UUID,
)

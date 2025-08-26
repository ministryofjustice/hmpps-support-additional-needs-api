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
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.*

@Entity
@Immutable
@Table(name = "other_review_contributor_history")
class OtherReviewContributorHistoryEntity(

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
    JoinColumn(name = "elsp_review_id", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
  )
  val elspReview: ElspReviewHistoryEntity,

  @EmbeddedId
  val id: OtherReviewContributorHistoryEntityKey,

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
)

@Embeddable
data class OtherReviewContributorHistoryEntityKey(
  @Column(name = "rev_id") val revisionNumber: Long,
  @Column(name = "elsp_review_id") val elspReviewId: UUID,
  @Column(name = "id") val id: UUID,
)

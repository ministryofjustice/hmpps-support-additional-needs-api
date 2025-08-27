package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
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
@Table(name = "other_review_contributor")
@Audited(withModifiedFlag = false)
class OtherReviewContributorEntity(

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "elsp_review_id", nullable = false)
  var elspReview: ElspReviewEntity,

  @Column(length = 200, nullable = false)
  val name: String,

  @Column(length = 200, nullable = false)
  val jobRole: String,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @Id
  @GeneratedValue
  @Column
  var id: UUID? = null,

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
)

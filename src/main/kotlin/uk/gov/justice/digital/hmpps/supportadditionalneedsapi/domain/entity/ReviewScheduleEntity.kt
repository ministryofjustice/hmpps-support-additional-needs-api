package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Table(name = "review_schedule")
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Audited(withModifiedFlag = false)
data class ReviewScheduleEntity(

  @Column(updatable = false)
  val prisonNumber: String,

  @Column
  var deadlineDate: LocalDate,

  @Column
  @Enumerated(value = EnumType.STRING)
  var status: ReviewScheduleStatus,

  @Column
  var exemptionReason: String?,

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
  @Column(updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedBy
  @Column
  var updatedBy: String? = null,

  @UpdateTimestamp
  @Column
  var updatedAt: Instant? = null,
)

enum class ReviewScheduleStatus(val activeReview: Boolean) {
  SCHEDULED(true),
  EXEMPT_SYSTEM_TECHNICAL_ISSUE(true),
  EXEMPT_PRISONER_TRANSFER(false),
  EXEMPT_PRISONER_RELEASE(false),
  EXEMPT_PRISONER_DEATH(false),
  EXEMPT_PRISONER_MERGE(false),
  EXEMPT_PRISONER_NOT_COMPLY(false),
  EXEMPT_NOT_IN_EDUCATION(false),
  EXEMPT_UNKNOWN(false),
  COMPLETED(false),
}

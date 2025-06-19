package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate

@Table(name = "review_schedule")
@Entity
@EntityListeners(value = [AuditingEntityListener::class])
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
) : BaseAuditableEntity()

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

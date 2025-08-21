package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "timeline")
data class TimelineEntity(

  @Id
  val id: UUID = UUID.randomUUID(),

  @Column(updatable = false)
  val prisonNumber: String,

  @Enumerated(EnumType.STRING)
  var event: TimelineEventType,

  var additionalInfo: String? = null,

  @Column(updatable = false)
  val createdAtPrison: String,

  @CreatedBy
  @Column(updatable = false)
  var createdBy: String? = null,

  @CreationTimestamp
  @Column(updatable = false)
  var createdAt: Instant? = null,

)

enum class TimelineEventType {
  CONDITION_ADDED,
  CHALLENGE_ADDED,
  ALN_CHALLENGE_ADDED,
  ALN_STRENGTH_ADDED,
  ALN_SCREENER_ADDED,
  STRENGTH_ADDED,
  SUPPORT_STRATEGY_ADDED,
  CURIOUS_ASSESSMENT_TRIGGER,
  CURIOUS_EDUCATION_TRIGGER,
  ELSP_CREATED,
  ELSP_REVIEW_CREATED,
}

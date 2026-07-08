package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

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

  @CreatedDate
  @Column(updatable = false)
  var createdAt: Instant? = null,

) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as TimelineEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

enum class TimelineEventType {
  CONDITION_ADDED,
  CONDITION_DELETED,
  CHALLENGE_ADDED,
  CHALLENGE_DELETED,
  ALN_CHALLENGE_ADDED,
  ALN_STRENGTH_ADDED,
  ALN_SCREENER_ADDED,
  ALN_SCREENER_DELETED,
  STRENGTH_ADDED,
  STRENGTH_DELETED,
  SUPPORT_STRATEGY_ADDED,
  SUPPORT_STRATEGY_DELETED,
  CURIOUS_ASSESSMENT_TRIGGER,
  CURIOUS_EDUCATION_TRIGGER,
  ELSP_CREATED,
  ELSP_REVIEW_CREATED,
}

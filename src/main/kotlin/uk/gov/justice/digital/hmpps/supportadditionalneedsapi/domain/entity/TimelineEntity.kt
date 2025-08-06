package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.time.LocalDate
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
  var event: EventType,

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


enum class EventType {
  CONDITION_ADDED,
  CHALLENGE_ADDED,
  STRENGTH_ADDED,
  SUPPORT_STRATERGY_ADDED,
  CURIOUS_ASSESSMENT_TRIGGER,
  CURIOUS_EDUCATION_TRIGGER,
  ELSP_CREATED,
  ELSP_UPDATED,
  ALN_ASSESSMENT_ADDED
}
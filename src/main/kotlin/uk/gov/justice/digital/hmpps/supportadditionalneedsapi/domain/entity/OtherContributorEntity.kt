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
import org.hibernate.annotations.UuidGenerator
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "OTHER_CONTRIBUTOR")
data class OtherContributorEntity(

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "elsp_plan_id")
  var elspPlan: ElspPlanEntity,

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @Column(length = 200, nullable = false)
  val name: String,

  @Column(length = 200, nullable = false)
  val jobRole: String,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

) {
  @Id
  @GeneratedValue
  @UuidGenerator
  var id: UUID? = null

  @Column(updatable = false)
  @CreatedBy
  var createdBy: String? = null

  @Column(updatable = false)
  @CreationTimestamp
  var createdAt: Instant? = null

  @Column
  @LastModifiedBy
  var updatedBy: String? = null

  @Column
  @UpdateTimestamp
  var updatedAt: Instant? = null
}

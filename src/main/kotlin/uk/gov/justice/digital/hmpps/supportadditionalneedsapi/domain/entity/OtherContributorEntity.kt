package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "other_contributor")
@Audited(withModifiedFlag = false)
data class OtherContributorEntity(

  @ManyToOne(optional = false)
  @JoinColumn(name = "elsp_plan_id", nullable = false)
  var elspPlan: ElspPlanEntity,

  @Column(length = 200, nullable = false)
  val name: String,

  @Column(length = 200, nullable = false)
  val jobRole: String,

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

  @CreatedDate
  @Column(updatable = false)
  var createdAt: Instant? = null,

  @LastModifiedBy
  @Column
  var updatedBy: String? = null,

  @LastModifiedDate
  @Column
  var updatedAt: Instant? = null,
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "OTHER_CONTRIBUTOR")
data class OtherContributorEntity(

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "elsp_plan_id")
  var elspPlan: ElspPlanEntity,

  @Column(length = 200, nullable = false)
  val name: String,

  @Column(length = 200, nullable = false)
  val jobRole: String,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

) : BaseAuditableEntity()

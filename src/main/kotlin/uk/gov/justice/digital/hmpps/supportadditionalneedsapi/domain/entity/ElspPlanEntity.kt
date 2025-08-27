package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
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
@Table(name = "elsp_plan")
@Audited(withModifiedFlag = false)
class ElspPlanEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(length = 200)
  val planCreatedByName: String? = null,

  @Column(length = 200)
  val planCreatedByJobRole: String? = null,

  @Column(nullable = false)
  val hasCurrentEhcp: Boolean = false,

  @Column
  var teachingAdjustments: String? = null,

  @Column
  var specificTeachingSkills: String? = null,

  @Column
  var examAccessArrangements: String? = null,

  @Column
  var lnspSupport: String? = null,

  @Column
  var lnspSupportHours: Int? = null,

  @Column
  var detail: String? = null,

  @Column(nullable = false)
  val individualSupport: String,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @OneToMany(mappedBy = "elspPlan", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
  val otherContributors: MutableList<OtherContributorEntity> = mutableListOf(),

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

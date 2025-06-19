package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "elsp_plan")
data class ElspPlanEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(length = 200)
  val planCreatedByName: String? = null,

  @Column(length = 200)
  val planCreatedByJobRole: String? = null,

  @Column(nullable = false)
  val hasCurrentEhcp: Boolean = false,

  @Column
  val learningEnvironmentAdjustments: String? = null,

  @Column
  val teachingAdjustments: String? = null,

  @Column
  val specificTeachingSkills: String? = null,

  @Column
  val examAccessArrangements: String? = null,

  @Column
  val lnspSupport: String? = null,

  @Column
  val detail: String? = null,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @OneToMany(mappedBy = "elspPlan", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.EAGER)
  val otherContributors: MutableList<OtherContributorEntity> = mutableListOf(),
) : BaseAuditableEntity()

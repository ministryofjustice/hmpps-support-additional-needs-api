package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.hibernate.annotations.UuidGenerator
import org.hibernate.envers.Audited
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.UUID

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "elsp_plan")
@Audited(withModifiedFlag = false)
data class ElspPlanEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @Column(length = 200)
  val planCreatedByName: String? = null,

  @Column(length = 200)
  val planCreatedByJobRole: String? = null,

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

  @OneToMany(mappedBy = "elspPlan", cascade = [CascadeType.ALL], orphanRemoval = true)
  val otherContributors: MutableList<OtherContributorEntity> = mutableListOf(),
) {
  @Id
  @GeneratedValue
  @UuidGenerator
  var id: UUID? = null

  @Column(updatable = false)
  @CreationTimestamp
  var createdAt: Instant? = null

  @Column(updatable = false)
  @CreatedBy
  var createdBy: String? = null

  @Column
  @UpdateTimestamp
  var updatedAt: Instant? = null

  @Column
  @LastModifiedBy
  var updatedBy: String? = null

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ElspPlanEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

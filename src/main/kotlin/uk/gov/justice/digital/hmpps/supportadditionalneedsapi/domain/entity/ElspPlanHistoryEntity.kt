package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Embeddable
import jakarta.persistence.EmbeddedId
import jakarta.persistence.Entity
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.hibernate.annotations.Immutable
import java.time.Instant
import java.util.*

@Entity
@Immutable
@Table(name = "elsp_plan_history")
data class ElspPlanHistoryEntity(
  @Column(name = "prison_number")
  val prisonNumber: String,

  @Column(name = "plan_created_by_name")
  val planCreatedByName: String? = null,

  @Column(name = "plan_created_by_job_role")
  val planCreatedByJobRole: String? = null,

  @OneToMany(mappedBy = "plan")
  val otherContributors: List<OtherContributorHistoryEntity> = emptyList(),

  @Column(name = "has_current_ehcp")
  val hasCurrentEhcp: Boolean = false,

  @Column(name = "teaching_adjustments")
  var teachingAdjustments: String? = null,

  @Column(name = "specific_teaching_Skills")
  var specificTeachingSkills: String? = null,

  @Column(name = "exam_access_arrangements")
  var examAccessArrangements: String? = null,

  @Column(name = "lnsp_support")
  var lnspSupport: String? = null,

  @Column(name = "lnsp_support_hours")
  var lnspSupportHours: Int? = null,

  @Column
  var detail: String? = null,

  @Column(name = "individual_support")
  val individualSupport: String,

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID(),

  @Column(name = "created_by")
  val createdBy: String,

  @Column(name = "created_at")
  val createdAt: Instant,

  @Column(name = "updated_by")
  val updatedBy: String,

  @Column(name = "updated_at")
  val updatedAt: Instant,

  @Column(name = "created_at_prison")
  val createdAtPrison: String,

  @Column(name = "updated_at_prison")
  val updatedAtPrison: String,

  @EmbeddedId
  val id: ElspPlanHistoryEntityKey,
) {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ElspPlanHistoryEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

@Embeddable
data class ElspPlanHistoryEntityKey(
  @Column(name = "rev_id")
  val revisionNumber: Long,
  @Column(name = "id")
  val id: UUID,
)

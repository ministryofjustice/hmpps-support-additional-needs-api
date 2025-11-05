package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "condition")
data class ConditionEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column
  @Enumerated(value = EnumType.STRING)
  var source: Source,

  @ManyToOne(optional = false)
  @JoinColumn(name = "condition_type_id", referencedColumnName = "id")
  val conditionType: ReferenceDataEntity,

  @Column
  var conditionName: String? = null,

  @Column
  var conditionDetails: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column
  var archiveReason: String? = null,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity()

enum class Source {
  SELF_DECLARED,
  CONFIRMED_DIAGNOSIS,
}

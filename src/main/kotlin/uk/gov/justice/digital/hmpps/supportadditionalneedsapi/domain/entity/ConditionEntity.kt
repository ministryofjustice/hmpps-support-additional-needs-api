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
  val source: Source,

  @ManyToOne(optional = false)
  @JoinColumn(name = "condition_type_id", referencedColumnName = "id")
  val conditionType: ReferenceDataEntity,

  @Column
  val conditionDetail: String? = null,

  @Column
  val detail: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity()

enum class Source {
  SELF_DECLARED,
  CONFIRMED_DIAGNOSIS,
}

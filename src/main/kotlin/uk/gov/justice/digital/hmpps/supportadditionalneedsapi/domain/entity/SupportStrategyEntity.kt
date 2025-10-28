package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "support_strategy")
data class SupportStrategyEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @ManyToOne(optional = false)
  @JoinColumn(name = "support_strategy_type_id", referencedColumnName = "id")
  val supportStrategyType: ReferenceDataEntity,

  @Column
  var detail: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity()

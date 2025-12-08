package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "ehcp_status")
data class EhcpStatusEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  val hasCurrentEhcp: Boolean = false,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

) : BaseAuditableEntity()

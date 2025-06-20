package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import org.springframework.data.annotation.CreatedBy
import org.springframework.data.annotation.LastModifiedBy
import java.time.Instant
import java.util.UUID

@MappedSuperclass
abstract class BaseAuditableEntity {

  @Id
  @Column
  val id: UUID = UUID.randomUUID()

  @Column(updatable = false)
  val reference: UUID = UUID.randomUUID()

  @CreatedBy
  @Column(updatable = false)
  var createdBy: String? = null

  @CreationTimestamp
  @Column(updatable = false)
  var createdAt: Instant? = null

  @LastModifiedBy
  @Column
  var updatedBy: String? = null

  @UpdateTimestamp
  @Column
  var updatedAt: Instant? = null
}

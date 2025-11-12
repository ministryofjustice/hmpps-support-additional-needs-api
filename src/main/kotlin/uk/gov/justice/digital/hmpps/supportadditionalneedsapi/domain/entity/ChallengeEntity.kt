package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.util.*

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "challenge")
data class ChallengeEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(name = "aln_screener_id", nullable = false)
  var alnScreenerId: UUID? = null,

  @ManyToOne(optional = false)
  @JoinColumn(name = "challenge_type_id", referencedColumnName = "id")
  val challengeType: ReferenceDataEntity,

  @Column
  var symptoms: String? = null,

  @Column
  @JdbcTypeCode(SqlTypes.ARRAY)
  @Enumerated(EnumType.STRING)
  var howIdentified: Set<IdentificationSource> = emptySet(),

  @Column
  var howIdentifiedOther: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column
  var archiveReason: String? = null,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity() {
  val fromALNScreener: Boolean
    get() = alnScreenerId != null
}

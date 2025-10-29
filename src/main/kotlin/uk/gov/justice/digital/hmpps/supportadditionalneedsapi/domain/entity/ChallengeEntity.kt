package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import io.hypersistence.utils.hibernate.type.array.EnumArrayType
import io.hypersistence.utils.hibernate.type.array.ListArrayType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.Parameter
import org.hibernate.annotations.Type
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
  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var howIdentified: Set<IdentificationSource> = emptySet(),

  @Column
  var howIdentifiedOther: String? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity() {
  val fromALNScreener: Boolean
    get() = alnScreenerId != null
}

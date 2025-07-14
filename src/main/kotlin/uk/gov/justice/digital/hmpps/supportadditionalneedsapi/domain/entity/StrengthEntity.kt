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
import java.time.LocalDate

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "strength")
data class StrengthEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(name = "from_aln_screener", nullable = false)
  var fromALNScreener: Boolean = false,

  @ManyToOne(optional = false)
  @JoinColumn(name = "strength_type_id", referencedColumnName = "id")
  val strengthType: ReferenceDataEntity,

  @Column
  val symptoms: String? = null,

  @Column
  @Type(ListArrayType::class, parameters = [Parameter(name = EnumArrayType.SQL_ARRAY_TYPE, value = "varchar")])
  var howIdentified: Set<IdentificationSource> = emptySet(),

  @Column
  val howIdentifiedOther: String? = null,

  @Column
  val screeningDate: LocalDate? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity()

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "aln_screener")
data class ALNScreenerEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(nullable = false)
  var screeningDate: LocalDate,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,

  @Column(name = "hasChallenges", nullable = false)
  var hasChallenges: Boolean = false,

  @Column(name = "hasStrengths", nullable = false)
  var hasStrengths: Boolean = false,

  @OneToMany(mappedBy = "alnScreenerId", fetch = FetchType.EAGER)
  val challenges: MutableList<ChallengeEntity> = mutableListOf(),

  @OneToMany(mappedBy = "alnScreenerId", fetch = FetchType.EAGER)
  val strengths: MutableList<StrengthEntity> = mutableListOf(),
) : BaseAuditableEntity() {
  val needsIdentified: Boolean
    get() = challenges.isNotEmpty() || strengths.isNotEmpty()

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as ALNScreenerEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

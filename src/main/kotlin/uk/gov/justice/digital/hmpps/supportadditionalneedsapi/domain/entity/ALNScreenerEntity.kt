package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.FetchType
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
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
}

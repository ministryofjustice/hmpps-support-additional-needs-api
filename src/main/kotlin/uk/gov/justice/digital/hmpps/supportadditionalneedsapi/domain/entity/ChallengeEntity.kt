package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate

@Entity
@EntityListeners(value = [AuditingEntityListener::class])
@Table(name = "challenge")
data class ChallengeEntity(
  @Column(updatable = false)
  val prisonNumber: String,

  @Column(name = "from_aln_screener", nullable = false)
  var fromALNScreener: Boolean = false,

  @ManyToOne(optional = false)
  @JoinColumn(name = "challenge_type_id", referencedColumnName = "id")
  val challengeType: ReferenceDataEntity,

  @Column
  val symptoms: String? = null,

  @Column
  val howIdentified: String? = null,

  @Column
  val screeningDate: LocalDate? = null,

  @Column(nullable = false)
  var active: Boolean = true,

  @Column(updatable = false)
  val createdAtPrison: String,

  @Column
  var updatedAtPrison: String,
) : BaseAuditableEntity()

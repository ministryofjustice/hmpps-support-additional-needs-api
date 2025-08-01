package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.Immutable
import java.time.LocalDate

@Entity
@Immutable
@Table(name = "prisoner_overview")
data class PrisonerOverview(

  @Id
  @Column(name = "prison_number")
  val prisonNumber: String,

  @Column(name = "has_aln_need")
  val hasAlnNeed: Boolean? = null,

  @Column(name = "has_ldd_need")
  val hasLddNeed: Boolean? = null,

  @Column(name = "in_education")
  val inEducation: Boolean? = null,

  @Column(name = "has_condition")
  val hasCondition: Boolean? = null,

  @Column(name = "has_non_screener_challenge")
  val hasNonScreenerChallenge: Boolean? = null,

  @Column(name = "has_non_screener_strength")
  val hasNonScreenerStrength: Boolean? = null,

  @Column(name = "plan_creation_deadline_date")
  val planCreationDeadlineDate: LocalDate? = null,

  @Column(name = "review_deadline_date")
  val reviewDeadlineDate: LocalDate? = null,

  @Column(name = "deadline_date")
  val deadlineDate: LocalDate? = null,

  @Column(name = "has_plan")
  val hasPlan: Boolean? = null,

  @Column(name = "plan_declined")
  val planDeclined: Boolean? = null,

  @Column(name = "has_need")
  val hasNeed: Boolean? = null,
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EntityListeners
import jakarta.persistence.Table
import org.hibernate.Hibernate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDate
import java.util.*

@Entity
@Table(name = "education_enrolment")
@EntityListeners(value = [AuditingEntityListener::class])
class EducationEnrolmentEntity(

  @Column
  val prisonNumber: String,

  @Column
  val establishmentId: String,

  @Column
  val qualificationCode: String,

  @Column
  val learningStartDate: LocalDate,

  @Column
  var plannedEndDate: LocalDate? = null,

  @Column
  var fundingType: String,

  @Column
  var completionStatus: String? = null,

  @Column
  var endDate: LocalDate? = null,

  @Column
  var lastCuriousReference: UUID? = null,

) : BaseAuditableEntity() {
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other == null || Hibernate.getClass(this) != Hibernate.getClass(other)) return false
    other as EducationEnrolmentEntity

    return id == other.id
  }

  override fun hashCode(): Int = javaClass.hashCode()

  override fun toString(): String = this::class.simpleName + "(id = $id, prisonNumber = $prisonNumber)"
}

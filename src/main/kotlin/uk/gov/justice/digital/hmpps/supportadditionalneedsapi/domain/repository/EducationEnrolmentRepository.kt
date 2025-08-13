package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import java.time.LocalDate
import java.util.UUID

@Repository
interface EducationEnrolmentRepository : JpaRepository<EducationEnrolmentEntity, UUID> {

  fun findAllByPrisonNumber(prisonNumber: String): List<EducationEnrolmentEntity>

  fun findAllByPrisonNumberAndEndDateIsNull(prisonNumber: String): List<EducationEnrolmentEntity>

  fun findByPrisonNumberAndEstablishmentIdAndQualificationCodeAndLearningStartDate(
    prisonNumber: String,
    establishmentId: String,
    qualificationCode: String,
    learningStartDate: LocalDate,
  ): EducationEnrolmentEntity?

  fun existsByPrisonNumberAndEstablishmentIdAndQualificationCodeAndLearningStartDate(
    prisonNumber: String,
    establishmentId: String,
    qualificationCode: String,
    learningStartDate: LocalDate,
  ): Boolean
}

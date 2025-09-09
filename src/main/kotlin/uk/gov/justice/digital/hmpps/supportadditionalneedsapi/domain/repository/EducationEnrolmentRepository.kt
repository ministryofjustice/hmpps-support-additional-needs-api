package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.EducationEnrolmentEntity
import java.time.LocalDate
import java.util.UUID

@Repository
interface EducationEnrolmentRepository : JpaRepository<EducationEnrolmentEntity, UUID> {

  fun findAllByPrisonNumber(prisonNumber: String): List<EducationEnrolmentEntity>

  fun findAllByPrisonNumberAndEndDateIsNull(prisonNumber: String): List<EducationEnrolmentEntity>

  @Query(
    """
      select min(e.learningStartDate)
      from EducationEnrolmentEntity e
      where e.prisonNumber = :prisonNumber
        and e.endDate is null
    """,
  )
  fun findEarliestLearningStartDateWithNoEndDate(prisonNumber: String): LocalDate
}

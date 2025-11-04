package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import java.time.LocalDate
import java.util.UUID

@Repository
interface EducationSupportPlansDueForCreationRepository : JpaRepository<PlanCreationScheduleEntity, UUID> {

  @Query(
    """
      SELECT pcs
      FROM PlanCreationScheduleEntity pcs
      WHERE pcs.deadlineDate BETWEEN :fromDate AND :toDate
      AND pcs.createdAtPrison NOT IN (
       'ACI', 'DGI', 'FWI', 'FBI', 'PBI', 'RHI', 'WNI', 'BWI', 'BZI', 'PRI',
       'UKI', 'WYI', 'ASI', 'PFI', 'UPI', 'SWI', 'PYI', 'CFI', 'CKI', 'FYI',
       'FEI', 'LGI', 'MKI')
      ORDER BY pcs.deadlineDate ASC, pcs.reference ASC
    """,
  )
  fun findEducationSupportPlansDueForCreation(
    @Param("fromDate") fromDate: LocalDate,
    @Param("toDate") toDate: LocalDate,
  ): List<PlanCreationScheduleEntity>
}

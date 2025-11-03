package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.reporting

import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity
import java.time.LocalDate
import java.util.UUID

@org.springframework.stereotype.Repository
interface EducationSupportPlansDueForCreationRepository : Repository<PlanCreationScheduleEntity, UUID> {

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

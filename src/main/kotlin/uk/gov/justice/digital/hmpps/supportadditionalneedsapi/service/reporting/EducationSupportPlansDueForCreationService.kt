package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.reporting

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.reporting.EducationSupportPlansDueForCreationRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.dto.PlanCsvRecord
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class EducationSupportPlansDueForCreationService(
  private val educationSupportPlansDueForCreationRepository: EducationSupportPlansDueForCreationRepository,
) {

  fun getEducationSupportPlansDueForCreation(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): List<PlanCsvRecord> {
    log.debug("Fetching education support plans due for creation between $fromDate and $toDate")

    val planCreationSchedules = educationSupportPlansDueForCreationRepository
      .findEducationSupportPlansDueForCreation(fromDate, toDate)

    log.info("Found ${planCreationSchedules.size} education support plans due for creation between $fromDate and $toDate")

    return PlanCsvRecord.fromList(planCreationSchedules)
  }
}

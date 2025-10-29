package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.reporting

import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.reporting.EducationSupportPlansDueForCreationRepository
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class EducationSupportPlansDueForCreationService(
  private val educationSupportPlansDueForCreationRepository: EducationSupportPlansDueForCreationRepository,
) {

  fun getEducationSupportPlansDueForCreationAsCsv(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): String {
    log.debug("Fetching education support plans due for creation between $fromDate and $toDate")

    if (fromDate.isAfter(toDate)) {
      log.warn("Invalid date range: fromDate ($fromDate) is after toDate ($toDate)")
      return generateCsv(emptyList())
    }

    val planCreationSchedules = educationSupportPlansDueForCreationRepository
      .findEducationSupportPlansDueForCreation(fromDate, toDate)

    log.info("Found ${planCreationSchedules.size} education support plans due for creation between $fromDate and $toDate")

    return generateCsv(planCreationSchedules)
  }

  private fun generateCsv(plans: List<uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity>): String {
    val csvBuilder = StringBuilder()
    
    // CSV Header
    csvBuilder.append("reference,prison_number,created_at_prison,deadline_date,status\n")
    
    // CSV Data
    plans.forEach { plan ->
      csvBuilder.append("${plan.reference},")
      csvBuilder.append("${plan.prisonNumber},")
      csvBuilder.append("${plan.createdAtPrison},")
      csvBuilder.append("${plan.deadlineDate},")
      csvBuilder.append("${plan.status.name}\n")
    }
    
    return csvBuilder.toString()
  }
}
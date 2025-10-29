package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.reporting

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
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
    val csvMapper = CsvMapper().apply {
      registerModule(KotlinModule.Builder().build())
    }
    
    // Define the column order explicitly
    val schema = csvMapper.schemaFor(PlanCsvRecord::class.java)
      .withHeader()
      .sortedBy("reference", "prison_number", "created_at_prison", "deadline_date", "status")
    
    val records = plans.map {
      PlanCsvRecord(
        reference = it.reference.toString(),
        prison_number = it.prisonNumber,
        created_at_prison = it.createdAtPrison,
        deadline_date = it.deadlineDate.toString(),
        status = it.status.name
      )
    }
    
    return csvMapper.writer(schema).writeValueAsString(records)
  }
  
  data class PlanCsvRecord(
    val reference: String,
    val prison_number: String,
    val created_at_prison: String,
    val deadline_date: String,
    val status: String
  )
}
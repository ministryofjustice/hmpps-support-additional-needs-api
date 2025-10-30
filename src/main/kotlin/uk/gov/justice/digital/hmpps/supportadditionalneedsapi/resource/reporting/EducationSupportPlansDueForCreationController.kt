package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.reporting

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.dto.PlanCsvRecord
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.HAS_VIEW_ELSP
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.reporting.EducationSupportPlansDueForCreationService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator.ValidDateRange
import java.time.LocalDate

@RestController
@Hidden
@RequestMapping("/reports/education-support-plans-due-for-creation")
@Validated
class EducationSupportPlansDueForCreationController(
  private val educationSupportPlansDueForCreationService: EducationSupportPlansDueForCreationService,
) {

  @GetMapping(produces = ["text/csv", MediaType.APPLICATION_JSON_VALUE])
  @PreAuthorize(HAS_VIEW_ELSP)
  @ValidDateRange(maxDays = 60)
  fun getEducationSupportPlansDueForCreation(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    fromDate: LocalDate?,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    toDate: LocalDate?,
    @RequestHeader(value = HttpHeaders.ACCEPT, required = false, defaultValue = "text/csv")
    acceptHeader: String,
  ): ResponseEntity<List<PlanCsvRecord>> {
    val effectiveToDate = toDate ?: LocalDate.now()
    val effectiveFromDate = fromDate ?: effectiveToDate.minusDays(14)
    val plans = educationSupportPlansDueForCreationService.getEducationSupportPlansDueForCreation(effectiveFromDate, effectiveToDate)

    // Add Content-Disposition header for CSV downloads
    return if (acceptHeader.contains("csv")) {
      ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"education-support-plans-due-for-creation.csv\"")
        .body(plans)
    } else {
      ResponseEntity.ok(plans)
    }
  }
}

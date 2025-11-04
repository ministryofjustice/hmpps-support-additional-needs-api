package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.controller

import io.swagger.v3.oas.annotations.Hidden
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.service.EducationSupportPlansDueForCreationService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.HAS_VIEW_ELSP
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.validator.ValidDateRange
import java.time.LocalDate

@RestController
@Hidden
@RequestMapping("/reports/education-support-plans-due-for-creation", produces = ["text/csv"])
@Validated
class EducationSupportPlansDueForCreationController(
  private val educationSupportPlansDueForCreationService: EducationSupportPlansDueForCreationService,
) {

  @GetMapping
  @PreAuthorize(HAS_VIEW_ELSP)
  @ValidDateRange(maxDays = 60)
  fun getEducationSupportPlansDueForCreation(
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    fromDate: LocalDate?,
    @RequestParam(required = false)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    toDate: LocalDate?,
  ): ResponseEntity<String> {
    val effectiveToDate = toDate ?: LocalDate.now()
    val effectiveFromDate = fromDate ?: effectiveToDate.minusDays(14)
    val csvData = educationSupportPlansDueForCreationService.getEducationSupportPlansDueForCreationAsCsv(effectiveFromDate, effectiveToDate)

    return ResponseEntity.ok()
      .header(HttpHeaders.CONTENT_TYPE, "text/csv")
      .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"education-support-plans-due-for-creation.csv\"")
      .body(csvData)
  }
}

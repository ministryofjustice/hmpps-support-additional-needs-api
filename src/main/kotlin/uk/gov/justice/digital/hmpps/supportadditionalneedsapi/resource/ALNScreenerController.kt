package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ALNScreenerService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/aln-screener")
class ALNScreenerController(private val alnScreenerService: ALNScreenerService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createALNScreener(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: ALNScreenerRequest,
  ) = alnScreenerService.createScreener(prisonNumber, request)
}

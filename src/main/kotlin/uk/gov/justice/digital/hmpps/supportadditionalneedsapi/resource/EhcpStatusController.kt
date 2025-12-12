package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EhcpStatusResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEhcpRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EhcpStatusService

@RestController
@RequestMapping("/profile/{prisonNumber}/ehcp-status")
class EhcpStatusController(
  private val ehcpStatusService: EhcpStatusService,
) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getEhcpStatus(
    @PathVariable prisonNumber: String,
  ): EhcpStatusResponse = ehcpStatusService.getEhcpStatus(prisonNumber)

  @PreAuthorize(HAS_EDIT_ELSP)
  @PutMapping
  fun updateEhcpStatus(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: UpdateEhcpRequest,
  ): EhcpStatusResponse = ehcpStatusService.updateEhcpStatus(prisonNumber, request)
}

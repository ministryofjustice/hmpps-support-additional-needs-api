package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateSupportStrategiesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateSupportStrategyRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SupportStrategyService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/support-strategies")
class SupportStrategyController(
  private val supportStrategyService: SupportStrategyService,
) {

  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createSupportStrategies(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: CreateSupportStrategiesRequest,
  ): SupportStrategyListResponse = supportStrategyService.createSupportStrategies(prisonNumber, request)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getSupportStrategies(
    @PathVariable prisonNumber: String,
  ): SupportStrategyListResponse = supportStrategyService.getSupportStrategies(prisonNumber)

  @PutMapping("/{supportStrategyReference}")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun updateSupportStrategy(
    @PathVariable prisonNumber: String,
    @PathVariable supportStrategyReference: UUID,
    @Valid @RequestBody request: UpdateSupportStrategyRequest,
  ): SupportStrategyResponse = supportStrategyService.updateSupportStrategy(prisonNumber, supportStrategyReference, request)

  @GetMapping("/{supportStrategyReference}")
  @PreAuthorize(HAS_VIEW_ELSP)
  fun getSupportStrategy(
    @PathVariable prisonNumber: String,
    @PathVariable supportStrategyReference: UUID,
  ): SupportStrategyResponse = supportStrategyService.getSupportStrategy(prisonNumber, supportStrategyReference)
}

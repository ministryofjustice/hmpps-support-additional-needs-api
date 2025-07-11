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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthsService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/strengths")
class StrengthController(private val strengthsService: StrengthsService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createStrengths(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: CreateStrengthsRequest,
  ): StrengthListResponse = strengthsService.createStrengths(prisonNumber, request)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getStrengths(
    @PathVariable prisonNumber: String,
  ): StrengthListResponse = strengthsService.getStrengths(prisonNumber)

  @PutMapping("/{StrengthReference}")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun updateStrength(
    @PathVariable prisonNumber: String,
    @PathVariable StrengthReference: UUID,
    @Valid @RequestBody request: UpdateStrengthRequest,
  ): StrengthResponse = strengthsService.updateStrength(prisonNumber, StrengthReference, request)
}

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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/strengths")
class StrengthController(private val strengthService: StrengthService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createStrengths(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: CreateStrengthsRequest,
  ): StrengthListResponse = strengthService.createStrengths(prisonNumber, request)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getStrengths(
    @PathVariable prisonNumber: String,
  ): StrengthListResponse = strengthService.getStrengths(prisonNumber)

  @PutMapping("/{strengthReference}")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun updateStrength(
    @PathVariable prisonNumber: String,
    @PathVariable strengthReference: UUID,
    @Valid @RequestBody request: UpdateStrengthRequest,
  ): StrengthResponse = strengthService.updateStrength(prisonNumber, strengthReference, request)

  @GetMapping("/{strengthReference}")
  @PreAuthorize(HAS_VIEW_ELSP)
  fun getStrength(
    @PathVariable prisonNumber: String,
    @PathVariable strengthReference: UUID,
  ): StrengthResponse = strengthService.getStrength(prisonNumber, strengthReference)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("/{strengthReference}/archive")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun archiveStrength(
    @PathVariable prisonNumber: String,
    @PathVariable strengthReference: UUID,
    @Valid @RequestBody request: ArchiveStrengthRequest,
  ) {
    strengthService.archiveStrength(prisonNumber, strengthReference, request)
  }
}

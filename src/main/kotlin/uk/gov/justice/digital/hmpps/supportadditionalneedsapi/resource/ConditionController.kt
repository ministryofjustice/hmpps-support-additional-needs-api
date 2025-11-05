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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ArchiveConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateConditionsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateConditionRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/conditions")
class ConditionController(private val conditionService: ConditionService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createConditions(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: CreateConditionsRequest,
  ): ConditionListResponse = conditionService.createConditions(prisonNumber, request)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getConditions(
    @PathVariable prisonNumber: String,
  ): ConditionListResponse = conditionService.getConditions(prisonNumber)

  @PutMapping("/{conditionReference}")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun updateCondition(
    @PathVariable prisonNumber: String,
    @PathVariable conditionReference: UUID,
    @Valid @RequestBody request: UpdateConditionRequest,
  ): ConditionResponse = conditionService.updateCondition(prisonNumber, conditionReference, request)

  @GetMapping("/{conditionReference}")
  @PreAuthorize(HAS_VIEW_ELSP)
  fun getCondition(
    @PathVariable prisonNumber: String,
    @PathVariable conditionReference: UUID,
  ): ConditionResponse = conditionService.getCondition(prisonNumber, conditionReference)

  @ResponseStatus(HttpStatus.NO_CONTENT)
  @PutMapping("/{conditionReference}/archive")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun archiveCondition(
    @PathVariable prisonNumber: String,
    @PathVariable conditionReference: UUID,
    @Valid @RequestBody request: ArchiveConditionRequest,
  ) {
    conditionService.archiveCondition(prisonNumber, conditionReference, request)
  }
}

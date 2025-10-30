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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateChallengesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateChallengeRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import java.util.*

@RestController
@RequestMapping("/profile/{prisonNumber}/challenges")
class ChallengeController(private val challengeService: ChallengeService) {
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  fun createChallenges(
    @PathVariable prisonNumber: String,
    @Valid
    @RequestBody request: CreateChallengesRequest,
  ): ChallengeListResponse = challengeService.createChallenges(prisonNumber, request)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getChallenges(
    @PathVariable prisonNumber: String,
  ): ChallengeListResponse = challengeService.getChallenges(prisonNumber)

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping("/{challengeReference}")
  fun getChallenge(
    @PathVariable prisonNumber: String,
    @PathVariable challengeReference: UUID,
  ): ChallengeResponse = challengeService.getChallenge(prisonNumber, challengeReference)

  @PutMapping("/{challengeReference}")
  @PreAuthorize(HAS_EDIT_ELSP)
  fun updateChallenge(
    @PathVariable prisonNumber: String,
    @PathVariable challengeReference: UUID,
    @Valid @RequestBody request: UpdateChallengeRequest,
  ): ChallengeResponse = challengeService.updateChallenge(prisonNumber, challengeReference, request)
}

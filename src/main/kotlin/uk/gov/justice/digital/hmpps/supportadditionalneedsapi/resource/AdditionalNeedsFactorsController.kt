package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.AdditionalNeedsFactorsResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ConditionService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.StrengthService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SupportStrategyService

@RestController
@RequestMapping("/profile/{prisonNumber}/additional-needs-factors")
class AdditionalNeedsFactorsController(
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val conditionService: ConditionService,
  private val supportStrategyService: SupportStrategyService,
) {

  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun getAdditionalNeedsFactors(
    @PathVariable prisonNumber: String,
  ): AdditionalNeedsFactorsResponse {
    val challenges = challengeService.getChallenges(prisonNumber, includeAln = false)
    val strengths = strengthService.getStrengths(prisonNumber, includeAln = false)
    val supportStrategies = supportStrategyService.getSupportStrategies(prisonNumber)
    val conditions = conditionService.getConditions(prisonNumber)

    return AdditionalNeedsFactorsResponse(
      challenges = challenges.challenges,
      conditions = conditions.conditions,
      strengths = strengths.strengths,
      supportStrategies = supportStrategies.supportStrategies,
    )
  }
}

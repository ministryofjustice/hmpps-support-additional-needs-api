package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.ServiceProperties
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.HasNeedResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.ChallengeService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.EducationSupportPlanService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.SupportStrategyService

@Validated
@RestController
@RequestMapping("/profile/{prisonNumber}/has-need")
class HasNeedController(
  private val needService: NeedService,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
  private val serviceProperties: ServiceProperties,
  private val planService: EducationSupportPlanService,
  private val supportStrategyService: SupportStrategyService,
  private val challengeService: ChallengeService,

) {
  @PreAuthorize(HAS_VIEW_ELSP)
  @GetMapping
  fun hasNeed(@PathVariable prisonNumber: String): HasNeedResponse {
    val url = getUrl(prisonNumber)
    val modalUrl = getModalUrl(prisonNumber)

    // return early if no need
    if (!needService.hasNeed(prisonNumber)) {
      return HasNeedResponse(
        hasNeed = false,
        url = url,
        modalUrl = modalUrl,
      )
    }

    val hasSanInformation =
      planService.hasPlan(prisonNumber) ||
        supportStrategyService.hasActiveSupportStrategies(prisonNumber) ||
        challengeService.hasActiveNonALNChallenge(prisonNumber)

    return HasNeedResponse(
      hasNeed = hasSanInformation,
      url = url,
      modalUrl = modalUrl,
    )
  }

  private fun getUrl(prisonNumber: String): String = serviceProperties.uiBaseUrl + "/profile/" + prisonNumber + "/overview"

  private fun getModalUrl(prisonNumber: String): String = serviceProperties.uiBaseUrl + "/code-fragment/" + prisonNumber + "/additional-needs"
}

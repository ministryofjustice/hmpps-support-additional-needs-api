package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource

import io.swagger.v3.oas.annotations.Hidden
import mu.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.NeedService

@RestController
@RequestMapping("/ldd-setup")
class LDDSetupController(
  private val needService: NeedService,
) {
  private val log = KotlinLogging.logger {}

  /**
   * Endpoint to set up LDD data.
   *
   */
  @PreAuthorize(HAS_EDIT_ELSP)
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Hidden
  fun createLDDData(
    @RequestBody request: SetUpLDDRequest,
  ) {
    request.prisonNumbers.forEach { prisonNumber ->
      try {
        needService.recordLddScreenerNeed(prisonNumber, true)
        log.info("Successfully recorded LDD screener need for prisonNumber={}", prisonNumber)
      } catch (ex: Exception) {
        log.error("Failed to record LDD screener need for prisonNumber={}", prisonNumber, ex)
      }
    }
  }
}

data class SetUpLDDRequest(
  val prisonNumbers: List<String>,
)

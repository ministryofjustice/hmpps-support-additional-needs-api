package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for external API endpoints.
 *
 * Properties are bound from the `apis.*` prefix in application configuration.
 */
@ConfigurationProperties(prefix = "apis")
data class ApisProperties(
  /**
   * Prisoner Search API configuration.
   */
  @field:Valid
  val prisonerSearchApi: ApiEndpoint,

  /**
   * Curious API configuration.
   */
  @field:Valid
  val curiousApi: ApiEndpoint,

  /**
   * Manage Users API configuration.
   */
  @field:Valid
  val manageUsersApi: ApiEndpoint,

  /**
   * Bank Holidays API configuration.
   */
  @field:Valid
  val bankHolidaysApi: ApiEndpoint,
) {
  /**
   * Configuration for an individual API endpoint.
   */
  data class ApiEndpoint(
    /**
     * Base URL for the API endpoint.
     */
    @field:NotBlank(message = "API URL must not be blank")
    val url: String,
  )
}

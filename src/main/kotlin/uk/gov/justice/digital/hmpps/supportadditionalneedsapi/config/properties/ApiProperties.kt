package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties

import jakarta.validation.constraints.NotNull
import org.springframework.boot.context.properties.ConfigurationProperties
import java.time.Duration

/**
 * Configuration properties for API timeout settings.
 *
 * Properties are bound from the `api.*` prefix in application configuration.
 */
@ConfigurationProperties(prefix = "api")
data class ApiProperties(
  /**
   * Timeout duration for health check endpoints.
   * Default: 2 seconds
   */
  @field:NotNull
  val healthTimeout: Duration = Duration.ofSeconds(2),

  /**
   * General timeout duration for API calls.
   * Default: 20 seconds
   */
  @field:NotNull
  val timeout: Duration = Duration.ofSeconds(20),
)

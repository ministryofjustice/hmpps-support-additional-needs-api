package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties

import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the service itself.
 *
 * Properties are bound from the `service.*` prefix in application configuration.
 */
@ConfigurationProperties(prefix = "service")
data class ServiceProperties(
  /**
   * Base URL for this service, used for generating detail URLs in domain events.
   */
  @field:NotBlank(message = "Base URL must not be blank")
  val baseUrl: String,

  /**
   * Base URL for the UI service, used for generating URLS.
   */
  @field:NotBlank(message = "UI Base URL must not be blank")
  val uiBaseUrl: String,
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Configuration

@ConfigurationProperties(prefix = "review")
data class ReviewConfig(
  var reviewDeadlineDaysToAdd: Long = 5,
)

@Configuration
@EnableConfigurationProperties(ReviewConfig::class)
class ReviewConfigLoader

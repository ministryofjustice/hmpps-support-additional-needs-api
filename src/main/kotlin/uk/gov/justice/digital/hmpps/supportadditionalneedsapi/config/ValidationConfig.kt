package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor

@Configuration
class ValidationConfig {
  @Bean
  fun methodValidationPostProcessor(): MethodValidationPostProcessor = MethodValidationPostProcessor()
}

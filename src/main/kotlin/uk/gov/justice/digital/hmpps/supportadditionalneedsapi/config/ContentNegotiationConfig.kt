package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.context.annotation.Configuration
import org.springframework.http.MediaType
import org.springframework.http.converter.HttpMessageConverter
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.csv.CsvHttpMessageConverter

@Configuration
class ContentNegotiationConfig : WebMvcConfigurer {

  override fun configureContentNegotiation(configurer: ContentNegotiationConfigurer) {
    configurer
      .ignoreAcceptHeader(false) // Use Accept header as primary method
      .defaultContentType(MediaType("text", "csv")) // Default to CSV for backward compatibility
      .mediaType("json", MediaType.APPLICATION_JSON)
      .mediaType("csv", MediaType("text", "csv"))
  }

  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    // Add CSV converter before JSON converter
    converters.add(0, CsvHttpMessageConverter())
    super.configureMessageConverters(converters)
  }
}
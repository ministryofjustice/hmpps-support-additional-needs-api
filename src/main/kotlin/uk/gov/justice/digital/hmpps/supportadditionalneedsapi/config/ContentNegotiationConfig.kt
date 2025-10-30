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
      .favorParameter(true)
      .parameterName("format")
      .ignoreAcceptHeader(false)
      .defaultContentType(MediaType.APPLICATION_JSON) // Default to JSON for most endpoints
      .mediaType("json", MediaType.APPLICATION_JSON)
      .mediaType("csv", MediaType("text", "csv"))
  }

  override fun configureMessageConverters(converters: MutableList<HttpMessageConverter<*>>) {
    // Add CSV converter at position 0 to ensure it's evaluated before JSON converter.
    // Spring checks converters in order, so CSV must come first to handle text/csv requests
    // before the default JSON converter attempts to process them.
    converters.add(0, CsvHttpMessageConverter())
    super.configureMessageConverters(converters)
  }
}

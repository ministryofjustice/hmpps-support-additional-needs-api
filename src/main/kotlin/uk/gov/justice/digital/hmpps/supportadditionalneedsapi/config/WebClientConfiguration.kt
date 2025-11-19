package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.ApiProperties
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.ApisProperties
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.properties.HmppsAuthProperties
import uk.gov.justice.hmpps.kotlin.auth.healthWebClient
import kotlin.apply as kotlinApply

@Configuration
class WebClientConfiguration(
  private val hmppsAuthProperties: HmppsAuthProperties,
  private val apiProperties: ApiProperties,
  private val apisProperties: ApisProperties,
) {
  // HMPPS Auth health ping is required if your service calls HMPPS Auth to get a token to call other services
  @Bean
  fun hmppsAuthHealthWebClient(builder: WebClient.Builder): WebClient =
    builder.healthWebClient(hmppsAuthProperties.url, apiProperties.healthTimeout)

  @Bean(name = ["prisonerSearchApiWebClient"])
  fun prisonerSearchApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "prisoner-search-api",
    url = apisProperties.prisonerSearchApi.url,
  )

  @Bean(name = ["curiousApiWebClient"])
  fun curiousApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "curious-api",
    url = apisProperties.curiousApi.url,
  )

  @Bean(name = ["manageUsersApiWebClient"])
  fun manageUsersApiWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    builder: WebClient.Builder,
  ): WebClient = builder.authorisedWebClient(
    authorizedClientManager,
    registrationId = "manage-users-api",
    url = apisProperties.manageUsersApi.url,
  )

  @Bean(name = ["bankHolidaysApiWebClient"])
  fun bankHolidaysApiWebClient(
    builder: WebClient.Builder,
  ): WebClient = builder.baseUrl(apisProperties.bankHolidaysApi.url).build()

  private fun WebClient.Builder.authorisedWebClient(
    authorizedClientManager: OAuth2AuthorizedClientManager,
    registrationId: String,
    url: String,
  ): WebClient {
    val oauth2Client = ServletOAuth2AuthorizedClientExchangeFilterFunction(authorizedClientManager).kotlinApply {
      setDefaultClientRegistrationId(registrationId)
    }

    return baseUrl(url)
      .clientConnector(ReactorClientHttpConnector(HttpClient.create()))
      .filter(oauth2Client)
      .build()
  }
}

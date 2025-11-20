package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.config

import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import io.netty.handler.timeout.ReadTimeoutException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.CuriousApiExtension.Companion.curiousApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.HmppsPrisonerSearchApiExtension.Companion.hmppsPrisonerSearchApi
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock.ManageUsersApiExtension.Companion.manageUsersApi

class WebClientTimeoutTest : IntegrationTestBase() {

  @Autowired
  @Qualifier("prisonerSearchApiWebClient")
  private lateinit var prisonerSearchApiWebClient: WebClient

  @Autowired
  @Qualifier("curiousApiWebClient")
  private lateinit var curiousApiWebClient: WebClient

  @Autowired
  @Qualifier("manageUsersApiWebClient")
  private lateinit var manageUsersApiWebClient: WebClient

  @Test
  fun `prisonerSearchApiWebClient times out after configured timeout`() {
    // Given: Stub OAuth2 token endpoint
    stubGetTokenFromHmppsAuth()

    // And: Mock server configured to delay response beyond test timeout (150ms)
    hmppsPrisonerSearchApi.stubFor(
      get(urlEqualTo("/prisoner/A1234BC"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", "application/json")
            .withBody("""{"prisonerNumber":"A1234BC"}""")
            .withFixedDelay(300), // Delay exceeds test timeout of 150ms
        ),
    )

    // When/Then: Making a request that will timeout should throw ReadTimeoutException
    assertThatThrownBy {
      prisonerSearchApiWebClient
        .get()
        .uri("/prisoner/A1234BC")
        .retrieve()
        .bodyToMono(String::class.java)
        .block()
    }
      .isInstanceOf(WebClientRequestException::class.java)
      .hasCauseInstanceOf(ReadTimeoutException::class.java)
  }

  @Test
  fun `curiousApiWebClient times out after configured timeout`() {
    // Given: Stub OAuth2 token endpoint
    stubGetTokenFromHmppsAuth()

    // And: Mock server configured to delay response beyond test timeout (150ms)
    curiousApi.stubFor(
      get(urlEqualTo("/test-endpoint"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", "application/json")
            .withBody("""{"status":"ok"}""")
            .withFixedDelay(300), // Delay exceeds test timeout of 150ms
        ),
    )

    // When/Then: Making a request that will timeout should throw ReadTimeoutException
    assertThatThrownBy {
      curiousApiWebClient
        .get()
        .uri("/test-endpoint")
        .retrieve()
        .bodyToMono(String::class.java)
        .block()
    }
      .isInstanceOf(WebClientRequestException::class.java)
      .hasCauseInstanceOf(ReadTimeoutException::class.java)
  }

  @Test
  fun `manageUsersApiWebClient times out after configured timeout`() {
    // Given: Stub OAuth2 token endpoint
    stubGetTokenFromHmppsAuth()

    // And: Mock server configured to delay response beyond test timeout (150ms)
    manageUsersApi.stubFor(
      get(urlEqualTo("/users/testuser"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", "application/json")
            .withBody("""{"username":"testuser"}""")
            .withFixedDelay(300), // Delay exceeds test timeout of 150ms
        ),
    )

    // When/Then: Making a request that will timeout should throw ReadTimeoutException
    assertThatThrownBy {
      manageUsersApiWebClient
        .get()
        .uri("/users/testuser")
        .retrieve()
        .bodyToMono(String::class.java)
        .block()
    }
      .isInstanceOf(WebClientRequestException::class.java)
      .hasCauseInstanceOf(ReadTimeoutException::class.java)
  }

  @Test
  fun `prisonerSearchApiWebClient completes successfully when response is within timeout`() {
    // Given: Stub OAuth2 token endpoint
    stubGetTokenFromHmppsAuth()

    // And: Mock server configured to respond quickly (within 150ms timeout)
    hmppsPrisonerSearchApi.stubFor(
      get(urlEqualTo("/prisoner/A1234BC"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", "application/json")
            .withBody("""{"prisonerNumber":"A1234BC"}""")
            .withFixedDelay(50), // Well within test timeout of 150ms
        ),
    )

    // When: Making a request that will complete successfully
    val response = prisonerSearchApiWebClient
      .get()
      .uri("/prisoner/A1234BC")
      .retrieve()
      .bodyToMono(String::class.java)
      .block()

    // Then: Verify request completes successfully
    assertThat(response).contains("A1234BC")
  }

  @Test
  fun `curiousApiWebClient completes successfully when response is within timeout`() {
    // Given: Stub OAuth2 token endpoint
    stubGetTokenFromHmppsAuth()

    // And: Mock server configured to respond quickly (within 150ms timeout)
    curiousApi.stubFor(
      get(urlEqualTo("/test-endpoint"))
        .willReturn(
          aResponse()
            .withStatus(HttpStatus.OK.value())
            .withHeader("Content-Type", "application/json")
            .withBody("""{"status":"ok"}""")
            .withFixedDelay(50), // Well within test timeout of 150ms
        ),
    )

    // When: Making a request that will complete successfully
    val response = curiousApiWebClient
      .get()
      .uri("/test-endpoint")
      .retrieve()
      .bodyToMono(String::class.java)
      .block()

    // Then: Verify request completes successfully
    assertThat(response).contains("ok")
  }
}

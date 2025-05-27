package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.PagedPrisonerResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.Prisoner

class HmppsPrisonerSearchApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val hmppsPrisonerSearchApi = HmppsPrisonerSearchApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    hmppsPrisonerSearchApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    hmppsPrisonerSearchApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    hmppsPrisonerSearchApi.stop()
  }
}

class HmppsPrisonerSearchApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9093
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun stubGetPrisoner(prisonNumber: String, response: Prisoner) {
    stubFor(
      get(urlPathMatching("/prisoner-search/prisoner/$prisonNumber"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
        ),
    )
  }

  fun stubPrisonersInAPrison(prisonId: String, response: List<Prisoner>) {
    stubFor(
      get(urlPathMatching("/prisoner-search/prison/$prisonId"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(PagedPrisonerResponse(last = true, content = response))),
        ),
    )
  }

  fun stubPrisonersInAPrison(prisonId: String, response: String) {
    stubFor(
      get(urlPathMatching("/prisoner-search/prison/$prisonId"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(response),
        ),
    )
  }

  fun stubGetPrisonerNotFound(prisonNumber: String) {
    stubFor(
      get(urlPathMatching("/prisoner-search/prisoner/$prisonNumber"))
        .willReturn(
          responseDefinition()
            .withStatus(404)
            .withHeader("Content-Type", "application/json"),
        ),
    )
  }
}

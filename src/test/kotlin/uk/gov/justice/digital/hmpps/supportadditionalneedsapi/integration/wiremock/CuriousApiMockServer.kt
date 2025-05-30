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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.LearnerNeurodivergenceDTO

class CuriousApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val curiousApi = CuriousApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    curiousApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    curiousApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    curiousApi.stop()
  }
}

class CuriousApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9094
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun stubGetCurious1PrisonerLddData(prisonNumber: String, response: LearnerNeurodivergenceDTO) {
    stubFor(
      get(urlPathMatching("/learnerNeurodivergence/$prisonNumber"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(objectMapper.writeValueAsString(response)),
        ),
    )
  }

  fun stubGetCurious1PrisonerLddDataNotFound(prisonNumber: String) {
    stubFor(
      get(urlPathMatching("/learnerNeurodivergence/$prisonNumber"))
        .willReturn(
          responseDefinition()
            .withStatus(404)
            .withHeader("Content-Type", "application/json"),
        ),
    )
  }
}

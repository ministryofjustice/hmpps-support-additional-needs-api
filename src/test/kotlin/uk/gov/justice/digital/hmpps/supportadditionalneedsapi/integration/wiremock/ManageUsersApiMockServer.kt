package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.wiremock

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.ResponseDefinitionBuilder.responseDefinition
import com.github.tomakehurst.wiremock.client.WireMock.aResponse
import com.github.tomakehurst.wiremock.client.WireMock.get
import com.github.tomakehurst.wiremock.client.WireMock.okJson
import com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo
import com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching
import com.github.tomakehurst.wiremock.http.Fault
import com.github.tomakehurst.wiremock.stubbing.Scenario
import org.junit.jupiter.api.extension.AfterAllCallback
import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.manageusers.UserDetailsDto

class ManageUsersApiExtension :
  BeforeAllCallback,
  AfterAllCallback,
  BeforeEachCallback {

  companion object {
    @JvmField
    val manageUsersApi = ManageUsersApiMockServer()
  }

  override fun beforeAll(context: ExtensionContext) {
    manageUsersApi.start()
  }

  override fun beforeEach(context: ExtensionContext) {
    manageUsersApi.resetRequests()
  }

  override fun afterAll(context: ExtensionContext) {
    manageUsersApi.stop()
  }
}

class ManageUsersApiMockServer : WireMockServer(WIREMOCK_PORT) {
  companion object {
    private const val WIREMOCK_PORT = 9095
    private val objectMapper = ObjectMapper()
      .registerModule(JavaTimeModule())
      .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  }

  fun setUpGetUser(username: String) {
    stubFor(
      get(urlPathMatching("/users/$username"))
        .willReturn(
          responseDefinition()
            .withStatus(200)
            .withHeader("Content-Type", "application/json")
            .withBody(
              objectMapper.writeValueAsString(UserDetailsDto(username = username, active = true, name = "Test User")),
            ),
        ),
    )
  }

  fun setUpManageUsersRepeatPass(username: String) {
    stubFor(
      get(urlEqualTo("/users/$username"))
        .inScenario("Retry scenario")
        .whenScenarioStateIs(Scenario.STARTED)
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        .willSetStateTo("Attempt 2"),
    )
    stubFor(
      get(urlEqualTo("/users/$username"))
        .inScenario("Retry scenario")
        .whenScenarioStateIs("Attempt 2")
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER))
        .willSetStateTo("Success"),
    )

    stubFor(
      get(urlEqualTo("/users/$username"))
        .inScenario("Retry scenario")
        .whenScenarioStateIs("Success")
        .willReturn(okJson("""{"username":"$username","active":true,"name":"Test User"}""")),
    )
  }

  fun setUpManageUsersRepeatFail(username: String) {
    stubFor(
      get(urlEqualTo("/users/$username"))
        .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)),
    )
  }
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.test.web.reactive.server.returnResult
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.HasNeedResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.NeedSource
import java.util.stream.Stream

class HasNeedTest : IntegrationTestBase() {

  companion object {
    private const val URI_TEMPLATE = "/profile/{prisonNumber}/has-need"

    @JvmStatic
    fun scenarios(): Stream<Arguments> = Stream.of(
      Arguments.of(
        Scenario(
          name = "condition + challenge + aln",
          seed = {
            seedCondition()
            seedChallenge()
            seedAln(hasNeed = true)
          },
          expectedHasNeed = true,
          expectedNeedSources = listOf(
            NeedSource.CONDITION_SELF_DECLARED,
            NeedSource.CHALLENGE_NOT_ALN_SCREENER,
            NeedSource.ALN_SCREENER,
          ),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "condition only",
          seed = { seedCondition() },
          expectedHasNeed = false,
          expectedNeedSources = listOf(NeedSource.CONDITION_SELF_DECLARED),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "condition + support strategy",
          seed = {
            seedCondition()
            seedSupportStrategy()
          },
          expectedHasNeed = true,
          expectedNeedSources = listOf(NeedSource.CONDITION_SELF_DECLARED),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "condition + plan",
          seed = {
            seedCondition()
            seedELSP()
          },
          expectedHasNeed = true,
          expectedNeedSources = listOf(NeedSource.CONDITION_SELF_DECLARED),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "challenge only",
          seed = { seedChallenge() },
          expectedHasNeed = true,
          expectedNeedSources = listOf(NeedSource.CHALLENGE_NOT_ALN_SCREENER),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "challenge + plan",
          seed = {
            seedChallenge()
            seedELSP()
          },
          expectedHasNeed = true,
          expectedNeedSources = listOf(NeedSource.CHALLENGE_NOT_ALN_SCREENER),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "aln only",
          seed = { seedAln(hasNeed = true) },
          expectedHasNeed = false,
          expectedNeedSources = listOf(NeedSource.ALN_SCREENER),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "aln + ldd",
          seed = {
            seedAln(hasNeed = true)
            seedLdd(hasNeed = true)
          },
          expectedHasNeed = false,
          expectedNeedSources = listOf(NeedSource.ALN_SCREENER),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "aln + ldd",
          seed = {
            seedAln(hasNeed = false)
            seedLdd(hasNeed = true)
          },
          expectedHasNeed = false,
          expectedNeedSources = emptyList(),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "ldd only",
          seed = { seedLdd(hasNeed = true) },
          expectedHasNeed = false,
          expectedNeedSources = listOf(NeedSource.LDD_SCREENER),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "no need",
          seed = { /* nothing */ },
          expectedHasNeed = false,
          expectedNeedSources = emptyList(),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "Plan only ",
          seed = { seedELSP() },
          expectedHasNeed = false,
          expectedNeedSources = emptyList(),
        ),
      ),
      Arguments.of(
        Scenario(
          name = "Strength only ",
          seed = { seedStrength() },
          expectedHasNeed = false,
          expectedNeedSources = emptyList(),
        ),
      ),
    )
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("scenarios")
  fun `has-need scenarios`(scenario: Scenario) {
    // Given
    stubGetTokenFromHmppsAuth()
    stubGetDisplayName("testuser")
    val prisonNumber = randomValidPrisonNumber()

    scenario.seed(SeedContext(this, prisonNumber))

    // When
    val response = webTestClient.get()
      .uri(URI_TEMPLATE, prisonNumber)
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO"), username = "testuser"))
      .exchange()
      .expectStatus().isOk
      .returnResult<HasNeedResponse>()

    // Then
    val actual = response.responseBody.blockFirst()
    assertThat(actual).isNotNull
    actual!!

    assertThat(actual.hasNeed).isEqualTo(scenario.expectedHasNeed)
    assertThat(actual.url).isEqualTo("http://localhost:8081/profile/$prisonNumber/overview")
    assertThat(actual.modalUrl).isEqualTo("http://localhost:8081/code-fragment/$prisonNumber/additional-needs")
  }

  data class Scenario(
    val name: String,
    val seed: SeedContext.() -> Unit,
    val expectedHasNeed: Boolean,
    val expectedNeedSources: List<NeedSource>,
  ) {
    override fun toString(): String = name
  }

  data class SeedContext(
    val test: HasNeedTest,
    val prisonNumber: String,
  ) {
    fun seedCondition() = with(test) {
      aValidConditionExists(prisonNumber)
    }

    fun seedChallenge() = with(test) {
      aValidChallengeExists(prisonNumber)
    }

    fun seedStrength() = with(test) {
      aValidStrengthExists(prisonNumber)
    }

    fun seedSupportStrategy() = with(test) {
      aValidSupportStrategyExists(prisonNumber)
    }

    fun seedAln(hasNeed: Boolean) = with(test) {
      createALNAssessmentMessage(prisonNumber, hasNeed = hasNeed)
    }

    fun seedLdd(hasNeed: Boolean) = with(test) {
      aValidLDDExists(prisonNumber, hasNeed = hasNeed)
    }

    fun seedELSP() = with(test) {
      anElSPExists(prisonNumber = prisonNumber)
    }
  }
}

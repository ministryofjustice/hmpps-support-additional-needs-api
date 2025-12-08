package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.resource.reporting

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ConditionEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Domain
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ElspPlanEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReferenceDataEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.StrengthEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.SupportStrategyEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.integration.IntegrationTestBase
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate
import java.util.UUID

class PrisonActivitySummaryIntegrationTest : IntegrationTestBase() {

  @BeforeEach
  fun setUp() {
    // Clean up all relevant tables in the correct order (child tables first)
    challengeRepository.deleteAll()
    strengthRepository.deleteAll()
    supportStrategyRepository.deleteAll()
    conditionRepository.deleteAll()
    alnScreenerRepository.deleteAll()
    elspPlanRepository.deleteAll()
  }

  @Test
  fun `should return 200 OK with CSV data for authorized user with RO role`() {
    val fromDate = LocalDate.now().minusDays(14)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"prison-activity-summary.csv\"")
  }

  @Test
  fun `should return 200 OK with CSV data for authorized user with RW role`() {
    val fromDate = LocalDate.now().minusDays(14)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RW")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
      .expectHeader().valueEquals("Content-Disposition", "attachment; filename=\"prison-activity-summary.csv\"")
  }

  @Test
  fun `should return 200 OK with default date range when dates not provided`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
  }

  @Test
  fun `should return 403 Forbidden for user without correct role`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__SEARCH__RO")))
      .exchange()
      .expectStatus().isForbidden
  }

  @Test
  fun `should return 400 Bad Request when date range exceeds maximum`() {
    val fromDate = LocalDate.now().minusDays(90)
    val toDate = LocalDate.now()

    webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$fromDate&toDate=$toDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isBadRequest
  }

  @Test
  fun `should return 401 Unauthorized when no bearer token`() {
    webTestClient
      .get()
      .uri("/reports/prison-activity-summary")
      .exchange()
      .expectStatus().isUnauthorized
  }

  @Test
  fun `should return CSV with aggregated counts from all tables for multiple prisons`() {
    // Given - Set up test data
    val today = LocalDate.now()

    // Set up reference data - use any existing reference data
    val conditions = referenceDataRepository.findByKeyDomainOrderByCategoryListSequenceAscListSequenceAsc(Domain.CONDITION)
    val challenges = referenceDataRepository.findByKeyDomainOrderByCategoryListSequenceAscListSequenceAsc(Domain.CHALLENGE)
    val strengths = referenceDataRepository.findByKeyDomainOrderByCategoryListSequenceAscListSequenceAsc(Domain.STRENGTH)
    val strategies = referenceDataRepository.findByKeyDomainOrderByCategoryListSequenceAscListSequenceAsc(Domain.SUPPORT_STRATEGY)

    if (conditions.isEmpty() || challenges.isEmpty() || strengths.isEmpty() || strategies.isEmpty()) {
      // Skip test if reference data is not set up
      return
    }

    val adhd = conditions.first()
    val dyslexia = conditions.firstOrNull { it != adhd } ?: adhd
    val reading = challenges.first()
    val writing = strengths.first()
    val sensory = strategies.first()

    createAlnScreener("MDI", randomValidPrisonNumber(), today)
    createAlnScreener("MDI", randomValidPrisonNumber(), today)
    createElspPlan("MDI", randomValidPrisonNumber(), today)
    createChallenge("MDI", randomValidPrisonNumber(), today, null, reading) // Not linked to ALN screener
    createChallenge("MDI", randomValidPrisonNumber(), today, null, reading) // Not linked to ALN screener
    createCondition("MDI", randomValidPrisonNumber(), today, adhd)
    createStrength("MDI", randomValidPrisonNumber(), null, writing) // Not linked to ALN screener
    createSupportStrategy("MDI", randomValidPrisonNumber(), sensory)
    createSupportStrategy("MDI", randomValidPrisonNumber(), sensory)

    // Prison BXI - Some records
    createAlnScreener("BXI", randomValidPrisonNumber(), today)
    createElspPlan("BXI", randomValidPrisonNumber(), today)
    createElspPlan("BXI", randomValidPrisonNumber(), today)
    createCondition("BXI", randomValidPrisonNumber(), today, dyslexia)
    createCondition("BXI", randomValidPrisonNumber(), today, adhd)

    // Prison HMP - Single record for each type
    createAlnScreener("HMP", randomValidPrisonNumber(), today)
    createElspPlan("HMP", randomValidPrisonNumber(), today)
    createChallenge("HMP", randomValidPrisonNumber(), today, null, reading)
    createCondition("HMP", randomValidPrisonNumber(), today, adhd)
    createStrength("HMP", randomValidPrisonNumber(), null, writing)
    createSupportStrategy("HMP", randomValidPrisonNumber(), sensory)

    // Records for excluded prisons (should not appear in results)
    createAlnScreener("ACI", randomValidPrisonNumber(), today) // Excluded prison
    createElspPlan("DGI", randomValidPrisonNumber(), today) // Excluded prison

    // Records with ALN screener ID (should be excluded from challenge/strength count)
    val alnWithChallenge = createAlnScreener("MDI", randomValidPrisonNumber(), today)
    createChallenge("MDI", randomValidPrisonNumber(), today, alnWithChallenge.id, reading)
    createStrength("MDI", randomValidPrisonNumber(), alnWithChallenge.id, writing)

    // When - Request the report for today
    val response = webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$today&toDate=$today")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectHeader().contentType("text/csv")
      .expectBody(String::class.java)
      .returnResult()

    // Then - Verify CSV content
    val csvContent = response.responseBody!!
    val lines = csvContent.lines().filter { it.isNotBlank() }

    // Check header
    assertThat(lines[0]).isEqualTo("created_at_prison,aln_screener,elsp_plan,challenge,condition,strength,support_strategy")

    // Should have at least 3 data rows (BXI, HMP, MDI) - alphabetically sorted
    assertThat(lines.size).isGreaterThanOrEqualTo(4) // Header + at least 3 data rows

    // Find the lines for our test prisons
    val bxiLine = lines.find { it.startsWith("BXI,") }
    val hmpLine = lines.find { it.startsWith("HMP,") }
    val mdiLine = lines.find { it.startsWith("MDI,") }

    // Verify BXI data
    assertThat(bxiLine).isNotNull()
    val bxiData = bxiLine!!.split(",")
    assertThat(bxiData[1].toInt()).isGreaterThanOrEqualTo(1) // aln_screener
    assertThat(bxiData[2].toInt()).isGreaterThanOrEqualTo(2) // elsp_plan
    assertThat(bxiData[4].toInt()).isGreaterThanOrEqualTo(2) // condition

    // Verify HMP data
    assertThat(hmpLine).isNotNull()
    val hmpData = hmpLine!!.split(",")
    assertThat(hmpData[1].toInt()).isGreaterThanOrEqualTo(1) // aln_screener
    assertThat(hmpData[2].toInt()).isGreaterThanOrEqualTo(1) // elsp_plan
    assertThat(hmpData[3].toInt()).isGreaterThanOrEqualTo(1) // challenge
    assertThat(hmpData[4].toInt()).isGreaterThanOrEqualTo(1) // condition
    assertThat(hmpData[5].toInt()).isGreaterThanOrEqualTo(1) // strength
    assertThat(hmpData[6].toInt()).isGreaterThanOrEqualTo(1) // support_strategy

    // Verify MDI data
    assertThat(mdiLine).isNotNull()
    val mdiData = mdiLine!!.split(",")
    assertThat(mdiData[1].toInt()).isGreaterThanOrEqualTo(3) // aln_screener (including the one with challenge/strength)
    assertThat(mdiData[2].toInt()).isGreaterThanOrEqualTo(1) // elsp_plan
    assertThat(mdiData[3].toInt()).isGreaterThanOrEqualTo(2) // challenge (only those NOT linked to ALN screener)
    assertThat(mdiData[4].toInt()).isGreaterThanOrEqualTo(1) // condition
    assertThat(mdiData[5].toInt()).isGreaterThanOrEqualTo(1) // strength (only those NOT linked to ALN screener)
    assertThat(mdiData[6].toInt()).isGreaterThanOrEqualTo(2) // support_strategy

    // Verify excluded prisons don't appear
    assertThat(csvContent).doesNotContain("ACI")
    assertThat(csvContent).doesNotContain("DGI")
  }

  @Test
  fun `should return CSV with zero counts for most categories when prison has only support strategy`() {
    // Given - Create data but only for support strategy (to ensure prison appears in results)
    val today = LocalDate.now()
    val strategies = referenceDataRepository.findByKeyDomainOrderByCategoryListSequenceAscListSequenceAsc(Domain.SUPPORT_STRATEGY)
    if (strategies.isEmpty()) {
      // Skip test if reference data is not set up
      return
    }
    val sensory = strategies.first()
    createSupportStrategy("MDI", randomValidPrisonNumber(), sensory)

    // When
    val response = webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$today&toDate=$today")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult()

    // Then
    val csvContent = response.responseBody!!
    val lines = csvContent.lines().filter { it.isNotBlank() }

    assertThat(lines).hasSize(2) // Header + 1 data row

    val dataLine = lines[1].split(",")
    assertThat(dataLine[0]).isEqualTo("MDI")
    assertThat(dataLine[1]).isEqualTo("0") // aln_screener
    assertThat(dataLine[2]).isEqualTo("0") // elsp_plan
    assertThat(dataLine[3]).isEqualTo("0") // challenge
    assertThat(dataLine[4]).isEqualTo("0") // condition
    assertThat(dataLine[5]).isEqualTo("0") // strength
    assertThat(dataLine[6]).isEqualTo("1") // support_strategy
  }

  @Test
  fun `should return empty CSV when no data exists in date range`() {
    // Given
    createAlnScreener("ACI", randomValidPrisonNumber(), LocalDate.now()) // ACI is an excluded prison

    // When - Query for a date range far in the future where no data could exist
    val futureDate = LocalDate.now().plusYears(10)
    val response = webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$futureDate&toDate=$futureDate")
      .headers(setAuthorisation(roles = listOf("ROLE_SUPPORT_ADDITIONAL_NEEDS__ELSP__RO")))
      .exchange()
      .expectStatus().isOk
      .expectBody(String::class.java)
      .returnResult()

    // Then
    val csvContent = response.responseBody!!
    val lines = csvContent.lines().filter { it.isNotBlank() }

    assertThat(lines).hasSize(1) // Only header, no data rows
    assertThat(lines[0]).isEqualTo("created_at_prison,aln_screener,elsp_plan,challenge,condition,strength,support_strategy")
  }

  // Helper methods to create test data
  private fun createAlnScreener(prison: String, prisonNumber: String, date: LocalDate): ALNScreenerEntity {
    val entity = ALNScreenerEntity(
      prisonNumber = prisonNumber,
      screeningDate = date,
      createdAtPrison = prison,
      updatedAtPrison = prison,
      hasChallenges = true,
      hasStrengths = true,
    )
    return alnScreenerRepository.save(entity)
  }

  private fun createElspPlan(prison: String, prisonNumber: String, date: LocalDate): ElspPlanEntity {
    val entity = ElspPlanEntity(
      prisonNumber = prisonNumber,
      updatedAtPrison = prison,
      individualSupport = "support",
      planCreatedByJobRole = "Education coordinator",
      createdAtPrison = prison,
    )
    return elspPlanRepository.save(entity)
  }

  private fun createChallenge(
    prison: String,
    prisonNumber: String,
    date: LocalDate,
    alnScreenerId: UUID?,
    challengeType: ReferenceDataEntity,
  ): ChallengeEntity {
    val entity = ChallengeEntity(
      prisonNumber = prisonNumber,
      challengeType = challengeType,
      createdAtPrison = prison,
      updatedAtPrison = prison,
      alnScreenerId = alnScreenerId,
    )
    return challengeRepository.save(entity)
  }

  private fun createCondition(
    prison: String,
    prisonNumber: String,
    date: LocalDate,
    conditionType: ReferenceDataEntity,
  ): ConditionEntity {
    val entity = ConditionEntity(
      prisonNumber = prisonNumber,
      source = Source.SELF_DECLARED,
      conditionType = conditionType,
      createdAtPrison = prison,
      updatedAtPrison = prison,
    )
    return conditionRepository.save(entity)
  }

  private fun createStrength(
    prison: String,
    prisonNumber: String,
    alnScreenerId: UUID?,
    strengthType: ReferenceDataEntity,
  ): StrengthEntity {
    val entity = StrengthEntity(
      prisonNumber = prisonNumber,
      strengthType = strengthType,
      createdAtPrison = prison,
      updatedAtPrison = prison,
      alnScreenerId = alnScreenerId,
    )
    return strengthRepository.save(entity)
  }

  private fun createSupportStrategy(
    prison: String,
    prisonNumber: String,
    supportStrategyType: ReferenceDataEntity,
  ): SupportStrategyEntity {
    val entity = SupportStrategyEntity(
      prisonNumber = prisonNumber,
      supportStrategyType = supportStrategyType,
      createdAtPrison = prison,
      updatedAtPrison = prison,
    )
    return supportStrategyRepository.save(entity)
  }
}

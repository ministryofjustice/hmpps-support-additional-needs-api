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
import java.time.ZoneOffset
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
    // Given - Set up test data for today's date
    val today = LocalDate.now()
    val yesterday = today.minusDays(1)
    val tomorrow = today.plusDays(1)
    val twoWeeksAgo = today.minusDays(14)
    val nextMonth = today.plusMonths(1)

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

    // Prison MDI - Multiple records within date range
    createAlnScreener("MDI", randomValidPrisonNumber(), today)
    createAlnScreener("MDI", randomValidPrisonNumber(), yesterday)
    createElspPlan("MDI", randomValidPrisonNumber(), today)
    createChallenge("MDI", randomValidPrisonNumber(), today, null, reading) // Not linked to ALN screener
    createChallenge("MDI", randomValidPrisonNumber(), yesterday, null, reading) // Not linked to ALN screener
    createCondition("MDI", randomValidPrisonNumber(), today, adhd)
    createStrength("MDI", randomValidPrisonNumber(), today, null, writing) // Not linked to ALN screener
    createSupportStrategy("MDI", randomValidPrisonNumber(), today, sensory)
    createSupportStrategy("MDI", randomValidPrisonNumber(), yesterday, sensory)

    // Prison BXI - Some records within range
    createAlnScreener("BXI", randomValidPrisonNumber(), yesterday)
    createElspPlan("BXI", randomValidPrisonNumber(), yesterday)
    createElspPlan("BXI", randomValidPrisonNumber(), today)
    createCondition("BXI", randomValidPrisonNumber(), today, dyslexia)
    createCondition("BXI", randomValidPrisonNumber(), yesterday, adhd)

    // Prison HMP - Single record for each type
    createAlnScreener("HMP", randomValidPrisonNumber(), today)
    createElspPlan("HMP", randomValidPrisonNumber(), today)
    createChallenge("HMP", randomValidPrisonNumber(), today, null, reading)
    createCondition("HMP", randomValidPrisonNumber(), today, adhd)
    createStrength("HMP", randomValidPrisonNumber(), today, null, writing)
    createSupportStrategy("HMP", randomValidPrisonNumber(), today, sensory)

    // Records outside date range (should not be counted)
    createAlnScreener("MDI", randomValidPrisonNumber(), twoWeeksAgo.minusDays(1))
    createElspPlan("MDI", randomValidPrisonNumber(), nextMonth)

    // Records for excluded prisons (should not appear in results)
    createAlnScreener("ACI", randomValidPrisonNumber(), today) // Excluded prison
    createElspPlan("DGI", randomValidPrisonNumber(), today) // Excluded prison

    // Records with ALN screener ID (should be excluded from challenge/strength count)
    val alnWithChallenge = createAlnScreener("MDI", randomValidPrisonNumber(), today)
    createChallenge("MDI", randomValidPrisonNumber(), today, alnWithChallenge.id, reading)
    createStrength("MDI", randomValidPrisonNumber(), today, alnWithChallenge.id, writing)

    // When - Request the report for the last 14 days
    val response = webTestClient
      .get()
      .uri("/reports/prison-activity-summary?fromDate=$twoWeeksAgo&toDate=$today")
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

    // Should have 3 data rows (BXI, HMP, MDI) - alphabetically sorted
    assertThat(lines).hasSize(4) // Header + 3 data rows

    // Parse and verify BXI data (1 ALN, 2 ELSP, 0 challenges, 2 conditions, 0 strengths, 0 strategies)
    val bxiLine = lines[1].split(",")
    assertThat(bxiLine[0]).isEqualTo("BXI")
    assertThat(bxiLine[1]).isEqualTo("1") // aln_screener
    assertThat(bxiLine[2]).isEqualTo("2") // elsp_plan
    assertThat(bxiLine[3]).isEqualTo("0") // challenge
    assertThat(bxiLine[4]).isEqualTo("2") // condition
    assertThat(bxiLine[5]).isEqualTo("0") // strength
    assertThat(bxiLine[6]).isEqualTo("0") // support_strategy

    // Parse and verify HMP data (1 of each)
    val hmpLine = lines[2].split(",")
    assertThat(hmpLine[0]).isEqualTo("HMP")
    assertThat(hmpLine[1]).isEqualTo("1") // aln_screener
    assertThat(hmpLine[2]).isEqualTo("1") // elsp_plan
    assertThat(hmpLine[3]).isEqualTo("1") // challenge (not linked to ALN)
    assertThat(hmpLine[4]).isEqualTo("1") // condition
    assertThat(hmpLine[5]).isEqualTo("1") // strength (not linked to ALN)
    assertThat(hmpLine[6]).isEqualTo("1") // support_strategy

    // Parse and verify MDI data (3 ALN, 1 ELSP, 2 challenges not linked, 1 condition, 1 strength not linked, 2 strategies)
    val mdiLine = lines[3].split(",")
    assertThat(mdiLine[0]).isEqualTo("MDI")
    assertThat(mdiLine[1]).isEqualTo("3") // aln_screener (including the one with challenge/strength)
    assertThat(mdiLine[2]).isEqualTo("1") // elsp_plan
    assertThat(mdiLine[3]).isEqualTo("2") // challenge (only those NOT linked to ALN screener)
    assertThat(mdiLine[4]).isEqualTo("1") // condition
    assertThat(mdiLine[5]).isEqualTo("1") // strength (only those NOT linked to ALN screener)
    assertThat(mdiLine[6]).isEqualTo("2") // support_strategy

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
    createSupportStrategy("MDI", randomValidPrisonNumber(), today, sensory)

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
    // Given - Create data outside the query range (more than a month ago)
    val twoMonthsAgo = LocalDate.now().minusMonths(2)
    createAlnScreener("MDI", randomValidPrisonNumber(), twoMonthsAgo)

    // When - Query for just today
    val today = LocalDate.now()
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
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    return alnScreenerRepository.save(entity)
  }

  private fun createElspPlan(prison: String, prisonNumber: String, date: LocalDate): ElspPlanEntity {
    val entity = ElspPlanEntity(
      prisonNumber = prisonNumber,
      updatedAtPrison = prison,
      hasCurrentEhcp = true,
      individualSupport = "support",
      planCreatedByJobRole = "Education coordinator",
      createdAtPrison = prison,
    )
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
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
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
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
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    return conditionRepository.save(entity)
  }

  private fun createStrength(
    prison: String,
    prisonNumber: String,
    date: LocalDate,
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
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    return strengthRepository.save(entity)
  }

  private fun createSupportStrategy(
    prison: String,
    prisonNumber: String,
    date: LocalDate,
    supportStrategyType: ReferenceDataEntity,
  ): SupportStrategyEntity {
    val entity = SupportStrategyEntity(
      prisonNumber = prisonNumber,
      supportStrategyType = supportStrategyType,
      createdAtPrison = prison,
      updatedAtPrison = prison,
    )
    entity.createdAt = date.atStartOfDay().toInstant(ZoneOffset.UTC)
    return supportStrategyRepository.save(entity)
  }
}

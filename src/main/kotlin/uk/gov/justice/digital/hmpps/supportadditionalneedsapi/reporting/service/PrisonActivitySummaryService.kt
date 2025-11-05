package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.service

import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository.PrisonActivitySummaryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository.PrisonActivitySummaryResult
import java.time.LocalDate

private val log = KotlinLogging.logger {}

@Service
class PrisonActivitySummaryService(
  private val prisonActivitySummaryRepository: PrisonActivitySummaryRepository,
) {

  fun getPrisonActivitySummaryAsCsv(
    fromDate: LocalDate,
    toDate: LocalDate,
  ): String {
    log.debug("Fetching prison activity summary between $fromDate and $toDate")

    val activitySummaries = prisonActivitySummaryRepository
      .findPrisonActivitySummary(fromDate, toDate)

    log.info("Found ${activitySummaries.size} prisons with activity between $fromDate and $toDate")

    return generateCsv(activitySummaries)
  }

  private fun generateCsv(summaries: List<PrisonActivitySummaryResult>): String {
    val csvMapper = CsvMapper().apply {
      registerModule(KotlinModule.Builder().build())
    }

    val schema = csvMapper.schemaFor(PrisonActivityCsvRecord::class.java)
      .withHeader()
      .sortedBy("created_at_prison", "aln_screener", "elsp_plan", "challenge", "condition", "strength", "support_strategy")

    val records = summaries.map {
      PrisonActivityCsvRecord(
        created_at_prison = it.createdAtPrison,
        aln_screener = it.alnScreener,
        elsp_plan = it.elspPlan,
        challenge = it.challenge,
        condition = it.condition,
        strength = it.strength,
        support_strategy = it.supportStrategy,
      )
    }

    return csvMapper.writer(schema).writeValueAsString(records)
  }

  data class PrisonActivityCsvRecord(
    val created_at_prison: String,
    val aln_screener: Long,
    val elsp_plan: Long,
    val challenge: Long,
    val condition: Long,
    val strength: Long,
    val support_strategy: Long,
  )
}

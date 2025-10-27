package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerResponse
import java.time.LocalDate
import java.time.OffsetDateTime

data class AlnScreenerReport(
  val screeningDate: LocalDate,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: OffsetDateTime,
  val updatedAtPrison: String,
  val alnChallenges: List<ChallengeReport>,
  val alnStrengths: List<StrengthReport>,

)

fun ALNScreenerResponse.toReportModel(): AlnScreenerReport = AlnScreenerReport(
  screeningDate = screenerDate,
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  alnChallenges = challenges.map { it.toReportModel() },
  alnStrengths = strengths.map { it.toReportModel() },
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ChallengeResponse
import java.time.LocalDate
import java.time.OffsetDateTime

data class ChallengeReport(
  val fromALNScreener: String,
  val challengeType: String?,
  val active: String,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: OffsetDateTime,
  val updatedAtPrison: String,
  val alnScreenerDate: LocalDate? = null,
  val symptoms: String? = null,
  val howIdentified: String? = null,
  val howIdentifiedOther: String? = null,
)

fun ChallengeResponse.toReportModel(): ChallengeReport = ChallengeReport(
  fromALNScreener = toYesNo(fromALNScreener),
  challengeType = challengeType.description,
  active = toYesNo(active),
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  alnScreenerDate = alnScreenerDate,
  symptoms = symptoms,
  howIdentified = toCommaSeparatedString(howIdentified),
  howIdentifiedOther = howIdentifiedOther,
)

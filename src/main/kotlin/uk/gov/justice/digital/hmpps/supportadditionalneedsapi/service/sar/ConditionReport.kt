package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.sar

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ConditionResponse
import java.time.OffsetDateTime

data class ConditionReport(
  val conditionType: String?,
  val active: String,
  val createdBy: String,
  val createdAt: OffsetDateTime,
  val createdAtPrison: String,
  val updatedBy: String,
  val updatedAt: OffsetDateTime,
  val updatedAtPrison: String,
  val source: String,
)

fun ConditionResponse.toReportModel(): ConditionReport = ConditionReport(
  conditionType = conditionType.description,
  active = toYesNo(active),
  createdBy = createdBy,
  createdAt = createdAt,
  createdAtPrison = createdAtPrison,
  updatedBy = updatedBy,
  updatedAt = updatedAt,
  updatedAtPrison = updatedAtPrison,
  source = toCommaSeparatedString(listOf(source)),
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.dto

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonPropertyOrder
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.config.csv.CsvSerializable
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PlanCreationScheduleEntity

@CsvSerializable
@JsonPropertyOrder("reference", "prison_number", "created_at_prison", "deadline_date", "status")
data class PlanCsvRecord(
  @JsonProperty("reference")
  val reference: String,
  
  @JsonProperty("prison_number")
  val prisonNumber: String,
  
  @JsonProperty("created_at_prison")
  val createdAtPrison: String,
  
  @JsonProperty("deadline_date")
  val deadlineDate: String,
  
  @JsonProperty("status")
  val status: String,
) {
  companion object {
    fun from(entity: PlanCreationScheduleEntity): PlanCsvRecord {
      return PlanCsvRecord(
        reference = entity.reference.toString(),
        prisonNumber = entity.prisonNumber,
        createdAtPrison = entity.createdAtPrison,
        deadlineDate = entity.deadlineDate.toString(),
        status = entity.status.name,
      )
    }
    
    fun fromList(entities: List<PlanCreationScheduleEntity>): List<PlanCsvRecord> {
      return entities.map { from(it) }
    }
  }
}
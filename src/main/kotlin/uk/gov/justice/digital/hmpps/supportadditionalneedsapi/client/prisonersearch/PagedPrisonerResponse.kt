package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.StringTrimDeserializer
import java.time.LocalDate

data class PagedPrisonerResponse(
  val last: Boolean,
  val content: List<Prisoner>,
)

data class Prisoner(
  @field:JsonDeserialize(using = StringTrimDeserializer::class)
  val prisonerNumber: String,
  val legalStatus: LegalStatus = LegalStatus.OTHER,
  val releaseDate: LocalDate?,
  val prisonId: String?,
  @field:JsonProperty(value = "indeterminateSentence", defaultValue = "false")
  val isIndeterminateSentence: Boolean,
  @field:JsonProperty(value = "recall", defaultValue = "false")
  val isRecall: Boolean,
  @field:JsonDeserialize(using = StringTrimDeserializer::class)
  val lastName: String,
  @field:JsonDeserialize(using = StringTrimDeserializer::class)
  val firstName: String,
  val dateOfBirth: LocalDate,
  val cellLocation: String?,
  @field:JsonProperty(value = "nonDtoReleaseDateType")
  val releaseType: String?,
)

enum class LegalStatus {
  RECALL,
  DEAD,
  INDETERMINATE_SENTENCE,
  SENTENCED,
  CONVICTED_UNSENTENCED,
  CIVIL_PRISONER,
  IMMIGRATION_DETAINEE,
  REMAND,
  UNKNOWN,
  OTHER,
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch.LegalStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SentenceType

object SentenceTypeMapper {
  fun fromPrisonerSearchApiToModel(legalStatus: LegalStatus): SentenceType = when (legalStatus) {
    LegalStatus.RECALL -> SentenceType.RECALL
    LegalStatus.DEAD -> SentenceType.DEAD
    LegalStatus.INDETERMINATE_SENTENCE -> SentenceType.INDETERMINATE_SENTENCE
    LegalStatus.SENTENCED -> SentenceType.SENTENCED
    LegalStatus.CONVICTED_UNSENTENCED -> SentenceType.CONVICTED_UNSENTENCED
    LegalStatus.CIVIL_PRISONER -> SentenceType.CIVIL_PRISONER
    LegalStatus.IMMIGRATION_DETAINEE -> SentenceType.IMMIGRATION_DETAINEE
    LegalStatus.REMAND -> SentenceType.REMAND
    LegalStatus.UNKNOWN -> SentenceType.UNKNOWN
    LegalStatus.OTHER -> SentenceType.OTHER
  }
}

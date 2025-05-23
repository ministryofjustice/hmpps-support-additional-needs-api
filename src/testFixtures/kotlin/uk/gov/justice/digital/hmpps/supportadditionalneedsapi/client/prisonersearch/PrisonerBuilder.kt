package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.prisonersearch

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate

fun aValidPrisoner(
  prisonerNumber: String = randomValidPrisonNumber(),
  legalStatus: LegalStatus = LegalStatus.SENTENCED,
  releaseDate: LocalDate? = LocalDate.now().plusYears(1),
  prisonId: String? = "BXI",
  isIndeterminateSentence: Boolean = false,
  isRecall: Boolean = false,
  firstName: String = "Bob",
  lastName: String = "Smith",
  cellLocation: String = "B-2-022",
  dateOfBirth: LocalDate = LocalDate.now().minusYears(20),
  releaseType: String = "ARD",
): Prisoner = Prisoner(
  prisonerNumber = prisonerNumber,
  legalStatus = legalStatus,
  releaseDate = releaseDate,
  prisonId = prisonId,
  isRecall = isRecall,
  isIndeterminateSentence = isIndeterminateSentence,
  firstName = firstName,
  lastName = lastName,
  cellLocation = cellLocation,
  dateOfBirth = dateOfBirth,
  releaseType = releaseType,
)

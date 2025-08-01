package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model

import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.randomValidPrisonNumber
import java.time.LocalDate

fun aValidPerson(
  forename: String = "Bob",
  surname: String = "Smith",
  prisonNumber: String = randomValidPrisonNumber(),
  dateOfBirth: LocalDate = LocalDate.parse("1980-01-01"),
  sentenceType: SentenceType = SentenceType.SENTENCED,
  cellLocation: String? = "A1-1-001",
  releaseDate: LocalDate? = LocalDate.parse("2050-01-01"),
): Person = Person(
  forename = forename,
  surname = surname,
  prisonNumber = prisonNumber,
  dateOfBirth = dateOfBirth,
  sentenceType = sentenceType,
  cellLocation = cellLocation,
  releaseDate = releaseDate,
  hasAdditionalNeed = true,
  inEducation = true,
  planStatus = PlanStatus.NO_PLAN,
)

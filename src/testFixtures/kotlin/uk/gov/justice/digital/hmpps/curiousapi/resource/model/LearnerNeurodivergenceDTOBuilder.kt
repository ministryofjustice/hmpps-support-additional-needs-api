package uk.gov.justice.digital.hmpps.curiousapi.resource.model

import java.time.LocalDate

fun aValidLearnerNeurodivergenceDTO(
  assessmentDate: LocalDate? = LocalDate.parse("2022-02-10"),
  establishmentId: String? = "MDI",
  establishmentName: String? = "MOORLAND (HMP & YOI)",
  neurodivergenceAssessed: List<String>? = listOf("Attention Deficit Hyperactivity Disorder", "Alzheimers"),
  neurodivergenceSelfDeclared: List<String>? = listOf("Dyslexia"),
  neurodivergenceSupport: List<String>? = listOf("Communications", "Visual Support"),
  prn: String? = "A1234BC",
  selfDeclaredDate: LocalDate? = LocalDate.parse("2022-02-01"),
  supportDate: LocalDate? = LocalDate.parse("2022-02-16"),
): LearnerNeurodivergenceDTO = LearnerNeurodivergenceDTO(
  assessmentDate = assessmentDate,
  establishmentId = establishmentId,
  establishmentName = establishmentName,
  neurodivergenceAssessed = neurodivergenceAssessed,
  neurodivergenceSelfDeclared = neurodivergenceSelfDeclared,
  neurodivergenceSupport = neurodivergenceSupport,
  prn = prn,
  selfDeclaredDate = selfDeclaredDate,
  supportDate = supportDate,
)

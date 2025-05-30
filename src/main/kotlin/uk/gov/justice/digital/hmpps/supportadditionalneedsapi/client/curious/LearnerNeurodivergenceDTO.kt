package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

import java.time.LocalDate

/**
 * Verbatim copy of the LearnerNeurodivergenceDTO type from the Curious API swagger spec, including all fields being
 * optional. Unfortunately the Curious API swagger spec does not define the cardinality of the fields, so all fields
 * are essentially nullable even though we know some of them will always contain values.
 */
data class LearnerNeurodivergenceDTO(
  val assessmentDate: LocalDate?,
  val establishmentId: String?,
  val establishmentName: String?,
  val neurodivergenceAssessed: List<String>?,
  val neurodivergenceSelfDeclared: List<String>?,
  val neurodivergenceSupport: List<String>?,
  val prn: String?,
  val selfDeclaredDate: LocalDate?,
  val supportDate: LocalDate?,
)

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

class CuriousApiException(message: String, throwable: Throwable) : RuntimeException(message, throwable)

/**
 * Thrown when a specific prisoner is not returned by Curious API
 */
class CuriousPrisonerRecordNotFoundException(prisonNumber: String) : RuntimeException("Prisoner [$prisonNumber] not returned by Curious API")

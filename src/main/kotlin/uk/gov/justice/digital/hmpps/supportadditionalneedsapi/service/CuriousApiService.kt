package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiClient
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousApiException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious.CuriousPrisonerRecordNotFoundException

/**
 * Service class to provide an abstraction over calling Curious API to get data held about prisoners in Curious 1 or
 * Curious 2.
 */
@Service
class CuriousApiService(private val curiousApiClient: CuriousApiClient) {

  /**
   * Returns `true` if the specified prisoner has any Learning Difficulties and Disability data recorded in Curious (1 or 2)
   *
   * @throws CuriousApiException
   */
  fun hasLearningDifficultiesAndDisabilities(prisonNumber: String): Boolean {
    /* TODO
      Current implementation calls C1 endpoint; when C2 endpoint is available call that to determine if prisoner has any
      LDDs from the C1 and C2 data.
     */
    return try {
      curiousApiClient.getLearningDifficultiesAndDisabilities(prisonNumber).let {
        /* TODO there are nuances to the data within the returned LearnerNeurodivergenceDTOs which will determine whether
        the prisoner has any LDDs. The existence of the LearnerNeurodivergenceDTOs alone is not an indicator of LDDs.
        We need to understand the fields in the returned object to determine true or false.

        Current implementation simply returns true if the returned list of LearnerNeurodivergenceDTOs is not empty.
         */
        it.isNotEmpty()
      }
    } catch (e: CuriousPrisonerRecordNotFoundException) {
      // Prisoner not found in Curious so by definition does not have any LDD data recorded in Curious
      return false
    }
  }
}

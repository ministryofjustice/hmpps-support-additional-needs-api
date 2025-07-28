package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.client.curious

import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException

/**
 * API client class to consume Curious API endpoints.
 */
@Component
class CuriousApiClient(
  @Qualifier("curiousApiWebClient")
  private val curiousApiWebClient: WebClient,
) {

  /**
   * Returns a prisoner's Learning Difficulties and Disabilities (LDD) data from Curious 1
   * This method invokes the Curious 1 API endpoint [Get Learner Neurodivergence Info](https://testservices.sequation.net/sequation-virtual-campus2-api/swagger-ui/index.html#/Learner%20Neurodivergence%20Info/getLearnerNeurodivergenceUsingGET)
   * The Curious API returns a list of [LearnerNeurodivergenceDTO], one for each prison where the prisoner has had an LDD assessment.
   *
   * @throws CuriousPrisonerRecordNotFoundException
   * @throws CuriousApiException
   */
  fun getLearningDifficultiesAndDisabilities(prisonNumber: String): List<LearnerNeurodivergenceDTO> = try {
    curiousApiWebClient.get()
      .uri("/learnerNeurodivergence/{prisonNumber}", prisonNumber)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(object : ParameterizedTypeReference<List<LearnerNeurodivergenceDTO>>() {})
      .block()!!
  } catch (e: WebClientResponseException.NotFound) {
    throw CuriousPrisonerRecordNotFoundException(prisonNumber)
  } catch (e: Exception) {
    throw CuriousApiException("Error retrieving prisoner LDD data by prisonNumber $prisonNumber", e)
  }

  /**
   * Returns a prisoner's education information
   *
   * @throws CuriousPrisonerRecordNotFoundException
   * @throws CuriousApiException
   */
  fun getEducation(prisonNumber: String): EducationDTO = try {
    curiousApiWebClient.get()
      .uri("/learnerEducation/{prisonNumber}", prisonNumber)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(EducationDTO::class.java)
      .block()!!
  } catch (e: WebClientResponseException.NotFound) {
    throw CuriousPrisonerRecordNotFoundException(prisonNumber)
  } catch (e: Exception) {
    throw CuriousApiException("Error retrieving prisoner education data by prisonNumber $prisonNumber", e)
  }

  /**
   * Returns a prisoner's ALN information
   *
   * @throws CuriousPrisonerRecordNotFoundException
   * @throws CuriousApiException
   */
  fun getALNAssessment(prisonNumber: String): ALNAssessmentDTO = try {
    curiousApiWebClient.get()
      .uri("/learnerEducation/{prisonNumber}", prisonNumber)
      .headers {
        it.contentType = MediaType.APPLICATION_JSON
      }
      .retrieve()
      .bodyToMono(ALNAssessmentDTO::class.java)
      .block()!!
  } catch (e: WebClientResponseException.NotFound) {
    throw CuriousPrisonerRecordNotFoundException(prisonNumber)
  } catch (e: Exception) {
    throw CuriousApiException("Error retrieving prisoner ALN Screener data by prisonNumber $prisonNumber", e)
  }
}

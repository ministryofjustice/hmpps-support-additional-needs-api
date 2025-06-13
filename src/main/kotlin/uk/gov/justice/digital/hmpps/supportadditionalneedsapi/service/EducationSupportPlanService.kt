package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PersonAlreadyHasAPlanException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspPlanMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse

@Service
class EducationSupportPlanService(
  private val elspPlanRepository: ElspPlanRepository,
  private val elspPlanMapper: ElspPlanMapper,
) {
  fun getPlan(prisonNumber: String): EducationSupportPlanResponse {
    val entity = elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    return elspPlanMapper.toModel(entity)
  }

  fun create(prisonNumber: String, request: CreateEducationSupportPlanRequest): EducationSupportPlanResponse {
    checkPlanDoesNotExist(prisonNumber)

    val entity = elspPlanMapper.toEntity(prisonNumber, request)
    val savedEntity = elspPlanRepository.saveAndFlush(entity)

    return elspPlanMapper.toModel(savedEntity)

    // TODO
    // Also need to update the plan creation schedule (if it exists)
    // Create the review Schedule - with the deadline date from this request
    // and generate messages for MN
  }

  private fun checkPlanDoesNotExist(prisonNumber: String) {
    elspPlanRepository.findByPrisonNumber(prisonNumber)?.let {
      throw PersonAlreadyHasAPlanException(prisonNumber)
    }
  }
}

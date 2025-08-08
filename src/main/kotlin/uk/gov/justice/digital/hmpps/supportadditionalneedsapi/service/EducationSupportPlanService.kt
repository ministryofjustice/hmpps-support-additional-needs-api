package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.ELSP_CREATED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PersonAlreadyHasAPlanException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspPlanMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent

@Service
class EducationSupportPlanService(
  private val elspPlanRepository: ElspPlanRepository,
  private val elspPlanMapper: ElspPlanMapper,
  private val planCreationScheduleService: PlanCreationScheduleService,
  private val planReviewScheduleService: ReviewScheduleService,
) {
  fun getPlan(prisonNumber: String): EducationSupportPlanResponse {
    val entity = elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    return elspPlanMapper.toModel(entity)
  }

  @Transactional
  @TimelineEvent(
    eventType = ELSP_CREATED,
    additionalInfoPrefix = "Plan created with review deadline ",
  )
  fun create(prisonNumber: String, request: CreateEducationSupportPlanRequest): EducationSupportPlanResponse {
    checkPlanDoesNotExist(prisonNumber)

    val entity = elspPlanMapper.toEntity(prisonNumber, request)
    val savedEntity = elspPlanRepository.saveAndFlush(entity)

    // Update the plan creation schedule (if it exists)
    planCreationScheduleService.completeSchedule(prisonNumber, request.prisonId)
    // Create the review Schedule - with the deadline date from this request
    planReviewScheduleService.createReviewSchedule(prisonNumber, request.reviewDate, request.prisonId)

    return elspPlanMapper.toModel(savedEntity)
  }

  private fun checkPlanDoesNotExist(prisonNumber: String) {
    elspPlanRepository.findByPrisonNumber(prisonNumber)?.let {
      throw PersonAlreadyHasAPlanException(prisonNumber)
    }
  }
}

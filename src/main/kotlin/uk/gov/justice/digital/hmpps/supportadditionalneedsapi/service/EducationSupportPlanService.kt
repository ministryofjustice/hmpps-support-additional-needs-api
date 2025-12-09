package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.TimelineEventType.ELSP_CREATED
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EhcpStatusRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PersonAlreadyHasAPlanException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.EhcpStatusMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ElspPlanMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EducationSupportPlanResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEducationSupportPlanRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service.timeline.TimelineEvent

@Service
class EducationSupportPlanService(
  private val elspPlanRepository: ElspPlanRepository,
  private val ehcpStatusRepository: EhcpStatusRepository,
  private val elspPlanMapper: ElspPlanMapper,
  private val ehcpStatusMapper: EhcpStatusMapper,
  private val planCreationScheduleService: PlanCreationScheduleService,
  private val planReviewScheduleService: ReviewScheduleService,
) {
  fun getPlan(prisonNumber: String): EducationSupportPlanResponse {
    val entity = elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    val ehcpStatusEntity = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    return elspPlanMapper.toModel(entity, ehcpStatusEntity)
  }

  @Transactional
  @TimelineEvent(
    eventType = ELSP_CREATED,
    additionalInfoPrefix = "Plan created with review deadline ",
  )
  fun create(prisonNumber: String, request: CreateEducationSupportPlanRequest): EducationSupportPlanResponse {
    checkPlanDoesNotExist(prisonNumber)

    val elspPlanEntity = elspPlanMapper.toEntity(prisonNumber, request)
    val savedEntity = elspPlanRepository.saveAndFlush(elspPlanEntity)
    val ehcpStatusEntity = ehcpStatusMapper.toEntity(prisonNumber, request)
    val savedEhcpStatusEntity = ehcpStatusRepository.saveAndFlush(ehcpStatusEntity)

    // Update the plan creation schedule (if it exists)
    planCreationScheduleService.completeSchedule(prisonNumber, request.prisonId)
    // Create the review Schedule - with the deadline date from this request
    planReviewScheduleService.createReviewSchedule(prisonNumber, request.reviewDate, request.prisonId)

    return elspPlanMapper.toModel(savedEntity, savedEhcpStatusEntity)
  }

  private fun checkPlanDoesNotExist(prisonNumber: String) {
    elspPlanRepository.findByPrisonNumber(prisonNumber)?.let {
      throw PersonAlreadyHasAPlanException(prisonNumber)
    }
  }

  fun hasPlan(prisonNumber: String): Boolean = elspPlanRepository.existsByPrisonNumber(prisonNumber)

  @Transactional
  fun updatePlan(prisonNumber: String, request: UpdateEducationSupportPlanRequest) {
    // only update if there are any changes
    if (request.anyChanges) {
      val elsp = elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
      with(request) {
        elsp.lnspSupport = lnspSupport
        elsp.lnspSupportHours = lnspSupportHours
        elsp.detail = detail
        elsp.teachingAdjustments = teachingAdjustments
        elsp.examAccessArrangements = examAccessArrangements
        elsp.specificTeachingSkills = specificTeachingSkills
      }
      elspPlanRepository.save(elsp)
    }
  }
}

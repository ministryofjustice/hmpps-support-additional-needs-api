package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.PlanCreationScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.PlanCreationScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.PlanCreationSchedulesResponse

@Service
class PlanCreationScheduleService(
  private val planCreationScheduleHistoryRepository: PlanCreationScheduleHistoryRepository,
  private val planCreationScheduleHistoryMapper: PlanCreationScheduleHistoryMapper,
) {
  fun getSchedules(prisonId: String): PlanCreationSchedulesResponse = PlanCreationSchedulesResponse(
    planCreationScheduleHistoryRepository.findAllByPrisonNumber(prisonId).map { planCreationScheduleHistoryMapper.toModel(it) },
  )
}

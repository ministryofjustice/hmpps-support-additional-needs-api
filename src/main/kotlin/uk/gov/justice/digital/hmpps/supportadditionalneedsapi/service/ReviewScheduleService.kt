package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse

@Service
class ReviewScheduleService(
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleMapper: ReviewScheduleMapper,
) {
  fun getSchedules(prisonId: String): ReviewSchedulesResponse = ReviewSchedulesResponse(
    reviewScheduleRepository.getAllByPrisonNumber(prisonId).map { reviewScheduleMapper.toModel(it) },
  )
}

package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleHistoryEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ReviewScheduleStatus
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ReviewScheduleRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ReviewScheduleHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ReviewSchedulesResponse

@Service
class ReviewScheduleService(
  private val reviewScheduleRepository: ReviewScheduleRepository,
  private val reviewScheduleHistoryRepository: ReviewScheduleHistoryRepository,
  private val reviewScheduleHistoryMapper: ReviewScheduleHistoryMapper,
) {

  fun getSchedules(prisonId: String): ReviewSchedulesResponse {
    val entities = reviewScheduleHistoryRepository.findAllByPrisonNumber(prisonId)

    // Relabel versions per group (by id.id)
    val relabelledEntities: List<Pair<ReviewScheduleHistoryEntity, Int>> = entities
      .groupBy { it.id.id }
      .flatMap { (_, group) ->
        group.sortedBy { it.id.version }
          .mapIndexed { index, entity -> entity to (index + 1) }
      }

    return ReviewSchedulesResponse(
      relabelledEntities.map { (entity, relabelledVersion) ->
        reviewScheduleHistoryMapper.toModel(entity, relabelledVersion)
      },
    )
  }

  fun exemptSchedule(prisonNumber: String, status: ReviewScheduleStatus) {
    reviewScheduleRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)
      ?.takeIf { it.status == ReviewScheduleStatus.SCHEDULED }
      ?.let {
        it.status = status
        reviewScheduleRepository.save(it)
      }
  }
}

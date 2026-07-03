package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EhcpStatusHistoryRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EhcpStatusRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.EhcpStatusHistoryMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.EhcpStatusMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EhcpStatusResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEhcpRequest

@Service
class EhcpStatusService(
  private val elspPlanRepository: ElspPlanRepository,
  private val ehcpStatusRepository: EhcpStatusRepository,
  private val ehcpStatusMapper: EhcpStatusMapper,
  private val ehcpStatusHistoryRepository: EhcpStatusHistoryRepository,
  private val ehcpStatusHistoryMapper: EhcpStatusHistoryMapper,
) {

  fun getEhcpStatus(prisonNumber: String): EhcpStatusResponse {
    elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    val ehcpStatus = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    return ehcpStatusMapper.toModel(ehcpStatus)
  }

  /**
   * Returns every version of the person's EHCP answer, sourced from the EHCP status history, ordered oldest first.
   * Each version is independent of any Plan or Review version.
   */
  fun getAllEhcpStatuses(prisonNumber: String): List<EhcpStatusResponse> = ehcpStatusHistoryRepository
    .findAllByPrisonNumber(prisonNumber)
    .sortedBy { it.id.revisionNumber }
    .map { ehcpStatusHistoryMapper.toModel(it) }

  @Transactional
  fun updateEhcpStatus(prisonNumber: String, request: UpdateEhcpRequest): EhcpStatusResponse {
    elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    val ehcpStatus = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    ehcpStatus.hasCurrentEhcp = request.hasCurrentEhcp
    ehcpStatus.updatedAtPrison = request.prisonId
    return ehcpStatusMapper.toModel(ehcpStatusRepository.save(ehcpStatus))
  }
}

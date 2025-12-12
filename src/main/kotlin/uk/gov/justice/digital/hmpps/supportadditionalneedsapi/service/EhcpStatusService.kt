package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.EhcpStatusRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ElspPlanRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.exceptions.PlanNotFoundException
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.EhcpStatusMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.EhcpStatusResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateEhcpRequest

@Service
class EhcpStatusService(private val elspPlanRepository: ElspPlanRepository, private val ehcpStatusRepository: EhcpStatusRepository, private val ehcpStatusMapper: EhcpStatusMapper) {

  fun getEhcpStatus(prisonNumber: String): EhcpStatusResponse {
    elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    val ehcpStatus = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    return ehcpStatusMapper.toModel(ehcpStatus)
  }

  @Transactional
  fun updateEhcpStatus(prisonNumber: String, request: UpdateEhcpRequest): EhcpStatusResponse {
    elspPlanRepository.findByPrisonNumber(prisonNumber) ?: throw PlanNotFoundException(prisonNumber)
    val ehcpStatus = ehcpStatusRepository.findByPrisonNumber(prisonNumber)
    ehcpStatus.hasCurrentEhcp = request.hasCurrentEhcp
    ehcpStatus.updatedAtPrison = request.prisonId
    return ehcpStatusMapper.toModel(ehcpStatusRepository.save(ehcpStatus))
  }
}

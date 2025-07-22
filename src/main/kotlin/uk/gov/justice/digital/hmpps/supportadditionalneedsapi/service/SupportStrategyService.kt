package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateSupportStrategiesRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.SupportStrategyResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateSupportStrategyRequest
import java.util.*

@Service
class SupportStrategyService {

  fun getSupportStrategies(prisonNumber: String): SupportStrategyListResponse {
    TODO()
  }

  @Transactional
  fun createSupportStrategies(
    prisonNumber: String,
    request: CreateSupportStrategiesRequest,
  ): SupportStrategyListResponse {
    TODO()
  }

  @Transactional
  fun updateSupportStrategy(
    prisonNumber: String,
    supportStrategyReference: UUID,
    request: UpdateSupportStrategyRequest,
  ): SupportStrategyResponse {
    TODO()
  }
}

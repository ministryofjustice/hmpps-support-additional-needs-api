package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.CreateStrengthsRequest
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthListResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.StrengthResponse
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.UpdateStrengthRequest
import java.util.*

@Service
class StrengthsService {
  fun getStrengths(prisonNumber: String): StrengthListResponse {
    TODO()
  }

  @Transactional
  fun createStrengths(prisonNumber: String, request: CreateStrengthsRequest): StrengthListResponse {
    TODO()
  }

  fun updateStrength(
    prisonNumber: String,
    strengthReference: UUID,
    request: UpdateStrengthRequest,
  ): StrengthResponse {
    TODO()
  }
}

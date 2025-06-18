package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository

@Service
class NeedService(
  private val challengeRepository: ChallengeRepository,
  private val conditionRepository: ConditionRepository,
) {

  /**
   * With the most recent ALN screener from Curious determined the person
   * has a need. Only consider the most recent ALN screener.
   * If there is no ALN screener return false
   */
  fun hasALNScreenerNeed(prisonNumber: String): Boolean {
    // TODO method to establish whether a person has an ALN screener need or not
    return false
  }

  fun hasLDDNeed(prisonNumber: String): Boolean {
    // TODO method to establish whether a person has an LDD screener need or not
    return false
  }

  /**
   * Was a challenge or condition created in the SAN database and is it active - these can either be
   * Staff instigated or ALN screener instigated.
   */
  fun hasSANNeed(prisonNumber: String): Boolean = (
    challengeRepository.findAllByPrisonNumber(prisonNumber).any { it.active } ||
      conditionRepository.findAllByPrisonNumber(prisonNumber).any { it.active }
    )

  fun hasNeed(prisonNumber: String): Boolean = hasSANNeed(prisonNumber) || hasALNScreenerNeed(prisonNumber) || hasLDDNeed(prisonNumber)
}

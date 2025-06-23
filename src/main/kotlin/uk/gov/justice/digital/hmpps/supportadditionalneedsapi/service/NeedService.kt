package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.LddAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.LddAssessmentRepository
import java.util.*

@Service
class NeedService(
  private val challengeRepository: ChallengeRepository,
  private val conditionRepository: ConditionRepository,
  private val lddAssessmentRepository: LddAssessmentRepository,
  private val alnAssessmentRepository: AlnAssessmentRepository,
) {

  /**
   * We record this record when we receive an Aln screener message from Curious.
   *
   */
  fun recordAlnScreenerNeed(prisonNumber: String, hasNeed: Boolean, curiousReference: UUID) {
    alnAssessmentRepository.save(
      AlnAssessmentEntity(
        hasNeed = hasNeed,
        prisonNumber = prisonNumber,
        curiousReference = curiousReference,
      ),
    )
  }

  /**
   * May never need this but popped it in while I'm in here.
   *
   * As of 1 October 2025 the LDD (legacy) needs will become readonly.
   * We are hoping that this data will be given to use and loaded directly in
   * to our database.
   */
  fun recordLddScreenerNeed(prisonNumber: String, hasNeed: Boolean, curiousReference: UUID) {
    lddAssessmentRepository.save(
      LddAssessmentEntity(
        hasNeed = hasNeed,
        prisonNumber = prisonNumber,
      ),
    )
  }

  /**
   * With the most recent ALN screener from Curious determined the person
   * has a need. Only consider the most recent ALN screener.
   * If there is no ALN screener return false
   */
  fun hasALNScreenerNeed(prisonNumber: String): Boolean = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed ?: false

  fun hasLDDNeed(prisonNumber: String): Boolean = lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed ?: false

  /**
   * Was a challenge or condition created in the SAN database and is it active - these can either be
   * Staff instigated or ALN screener instigated.
   */
  fun hasActiveSANNeed(prisonNumber: String, includingALN: Boolean = true): Boolean {
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)

    return challenges.any { it.active && it.fromALNScreener == includingALN } ||
      conditions.any { it.active }
  }

  fun hasNeed(prisonNumber: String): Boolean = hasActiveSANNeed(prisonNumber, includingALN = false) ||
    hasALNScreenerNeed(prisonNumber) ||
    hasLDDNeed(prisonNumber)
}

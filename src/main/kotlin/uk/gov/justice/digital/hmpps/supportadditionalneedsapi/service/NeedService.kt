package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import mu.KotlinLogging
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.AlnAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.LddAssessmentEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.NeedSource
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.Source
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnAssessmentRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ChallengeRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.ConditionRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.LddAssessmentRepository
import java.time.LocalDate
import java.util.*
private val log = KotlinLogging.logger {}

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
  @Transactional
  fun recordAlnScreenerNeed(prisonNumber: String, hasNeed: Boolean, curiousReference: UUID, screenerDate: LocalDate) {
    alnAssessmentRepository.save(
      AlnAssessmentEntity(
        hasNeed = hasNeed,
        prisonNumber = prisonNumber,
        curiousReference = curiousReference,
        screeningDate = screenerDate,
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
  @Transactional
  fun recordLddScreenerNeed(prisonNumber: String, hasNeed: Boolean) {
    val existing = lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)

    if (existing != null) {
      existing.hasNeed = hasNeed
      lddAssessmentRepository.save(existing)
    } else {
      lddAssessmentRepository.save(
        LddAssessmentEntity(
          prisonNumber = prisonNumber,
          hasNeed = hasNeed,
        ),
      )
    }
  }

  /**
   * With the most recent ALN screener from Curious determined the person
   * has a need. Only consider the most recent ALN screener.
   * If there is no ALN screener return false
   */
  fun hasALNScreenerNeed(prisonNumber: String): Boolean? = alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed

  fun hasLDDNeed(prisonNumber: String): Boolean? = lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed

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

  /**
   * Has need is only true if the person either has a non ALN SAN need OR
   * has completed an ALN screener and was identified as having a need
   * or has not completed an ALN Screener and the LDD Need is true.
   */
  fun hasNeed(prisonNumber: String): Boolean {
    val alnNeed = hasALNScreenerNeed(prisonNumber)
    return hasActiveSANNeed(prisonNumber, includingALN = false) ||
      (alnNeed ?: hasLDDNeed(prisonNumber)) == true
  }

  fun getNeedSources(prisonNumber: String): Set<NeedSource> {
    log.debug("Getting need sources for $prisonNumber")
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)

    return buildSet {
      val alnNeed = hasALNScreenerNeed(prisonNumber)
      if (alnNeed == true) {
        add(NeedSource.ALN_SCREENER)
      } else {
        if (hasLDDNeed(prisonNumber) == true) {
          add(NeedSource.LDD_SCREENER)
        }
      }

      if (challenges.any { it.active && !it.fromALNScreener }) {
        add(NeedSource.CHALLENGE_NOT_ALN_SCREENER)
      }

      if (conditions.any { it.active && it.source == Source.CONFIRMED_DIAGNOSIS }) {
        add(NeedSource.CONDITION_CONFIRMED_DIAGNOSIS)
      }

      if (conditions.any { it.active && it.source == Source.SELF_DECLARED }) {
        add(NeedSource.CONDITION_SELF_DECLARED)
      }
    }
  }
}

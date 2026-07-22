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
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.mapper.ALNAssessmentMapper
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNAssessmentListResponse
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

@Service
class NeedService(
  private val challengeRepository: ChallengeRepository,
  private val conditionRepository: ConditionRepository,
  private val lddAssessmentRepository: LddAssessmentRepository,
  private val alnAssessmentRepository: AlnAssessmentRepository,
  private val alnAssessmentMapper: ALNAssessmentMapper,
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
   * With the most recent ALN Assessment from Curious determined the person
   * has a need. Only consider the most recent ALN assessment.
   * If there is no ALN assessment return false
   */
  fun hasALNAssessmentNeed(prisonNumber: String): Boolean = (alnAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed == true)
    .also {
      if (it) {
        log.debug { "Prisoner [$prisonNumber]'s most recent ALN Assessment indicates the person has a need" }
      }
    }

  fun hasLDDNeed(prisonNumber: String): Boolean = (lddAssessmentRepository.findFirstByPrisonNumberOrderByUpdatedAtDesc(prisonNumber)?.hasNeed == true)
    .also {
      if (it) {
        log.debug { "Prisoner [$prisonNumber] has an LDD need" }
      }
    }

  /**
   * Was a challenge or condition created in the SAN database and is it active - only the
   * Staff instigated ones are considered.
   */
  fun hasActiveSANNeed(prisonNumber: String): Boolean = (challengeRepository.existsByPrisonNumberAndActiveTrueAndAlnScreenerIdIsNull(prisonNumber)).also {
    if (it) {
      log.debug { "Prisoner [$prisonNumber] has an active Challenge associated to their latest SAN ALN Screener" }
    }
  } ||
    (conditionRepository.existsByPrisonNumberAndActiveTrue(prisonNumber)).also {
      if (it) {
        log.debug { "Prisoner [$prisonNumber] has an active Condition" }
      }
    }

  /**
   * Has need is only true if the person either has a non ALN SAN need OR
   * has completed an ALN Assessment and was identified as having a need
   * or has not completed an ALN Assessment and the LDD Need is true.
   */
  fun hasNeed(prisonNumber: String): Boolean = (hasActiveSANNeed(prisonNumber) || hasALNAssessmentNeed(prisonNumber) || hasLDDNeed(prisonNumber)).also {
    log.debug { "Prison [$prisonNumber] ${if (it) "has" else "does not have"} a need" }
  }

  fun getNeedSources(prisonNumber: String): SortedSet<NeedSource> {
    log.debug("Getting need sources for $prisonNumber")
    val challenges = challengeRepository.findAllByPrisonNumber(prisonNumber)
    val conditions = conditionRepository.findAllByPrisonNumber(prisonNumber)

    return sortedSetOf<NeedSource>().apply {
      if (hasALNAssessmentNeed(prisonNumber)) {
        add(NeedSource.ALN_SCREENER)
      } else if (hasLDDNeed(prisonNumber)) {
        add(NeedSource.LDD_SCREENER)
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
    }.also {
      log.debug { "Prisoner [$prisonNumber] needs sources: $it" }
    }
  }

  fun getAlnScreenerNeeds(prisonNumber: String): ALNAssessmentListResponse = alnAssessmentRepository
    .findAllByPrisonNumber(prisonNumber)
    .sortedWith(
      compareByDescending<AlnAssessmentEntity> { it.screeningDate }
        .thenByDescending { it.updatedAt },
    ).let { assessments ->
      ALNAssessmentListResponse(assessments.map { alnAssessmentMapper.toModel(it) })
    }
}

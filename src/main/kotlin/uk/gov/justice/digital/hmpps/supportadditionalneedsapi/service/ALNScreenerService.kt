package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.service

import jakarta.transaction.Transactional
import org.springframework.stereotype.Service
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ALNScreenerEntity
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository.AlnScreenerRepository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.resource.model.ALNScreenerRequest

@Service
class ALNScreenerService(
  private val challengeService: ChallengeService,
  private val strengthService: StrengthService,
  private val alnScreenerRepository: AlnScreenerRepository,
) {
  @Transactional
  fun createScreener(prisonNumber: String, request: ALNScreenerRequest) {
    with(request) {
      val alnScreener = alnScreenerRepository.saveAndFlush(
        ALNScreenerEntity(
          prisonNumber = prisonNumber,
          createdAtPrison = prisonId,
          updatedAtPrison = prisonId,
          screeningDate = screenerDate,
          hasChallenges = challenges.isNotEmpty(),
          hasStrengths = strengths.isNotEmpty(),
        ),
      )
      challengeService.createAlnChallenges(prisonNumber, challenges, prisonId, alnScreener.id)
      strengthService.createAlnStrengths(prisonNumber, strengths, prisonId, alnScreener.id)
      alnScreenerRepository.saveAndFlush(alnScreener)
    }
  }
}

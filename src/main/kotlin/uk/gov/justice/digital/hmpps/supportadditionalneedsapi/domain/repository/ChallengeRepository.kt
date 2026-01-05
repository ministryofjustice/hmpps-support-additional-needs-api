package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.ChallengeEntity
import java.util.UUID

@Repository
interface ChallengeRepository : JpaRepository<ChallengeEntity, UUID> {

  fun findAllByPrisonNumber(prisonNumber: String): List<ChallengeEntity>
  fun findAllByPrisonNumberAndAlnScreenerIdIsNull(prisonNumber: String): List<ChallengeEntity>
  fun findAllByPrisonNumberAndAlnScreenerIdIsNotNull(prisonNumber: String): List<ChallengeEntity>
  fun getChallengeEntityByPrisonNumberAndReference(prisonNumber: String, reference: UUID): ChallengeEntity?
}

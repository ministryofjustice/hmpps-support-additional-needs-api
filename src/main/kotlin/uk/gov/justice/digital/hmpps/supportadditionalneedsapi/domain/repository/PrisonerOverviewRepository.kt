package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.PrisonerOverviewEntity

@Repository
interface PrisonerOverviewRepository : JpaRepository<PrisonerOverviewEntity, String> {
  @Deprecated("Slow and expensive query", replaceWith = ReplaceWith("findAllByPrisonNumbers"), level = DeprecationLevel.HIDDEN)
  fun findByPrisonNumberIn(prisonNumbers: List<String>): List<PrisonerOverviewEntity>

  @Deprecated("Slow and expensive query", replaceWith = ReplaceWith("findOneByPrisonNumber"), level = DeprecationLevel.HIDDEN)
  fun findByPrisonNumber(prisonNumber: String): PrisonerOverviewEntity?

  @Query(
    "select * from find_prisoner_overviews(cast(:prisonNumbers as text[]))",
    nativeQuery = true,
  )
  fun findAllByPrisonNumbers(
    @Param("prisonNumbers") prisonNumbers: Array<String>,
  ): List<PrisonerOverviewEntity>

  fun findOneByPrisonNumber(prisonNumber: String): PrisonerOverviewEntity? = findAllByPrisonNumbers(arrayOf(prisonNumber))
    .takeIf { it.isNotEmpty() }?.first()
}

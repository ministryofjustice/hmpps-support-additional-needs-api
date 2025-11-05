package uk.gov.justice.digital.hmpps.supportadditionalneedsapi.reporting.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import uk.gov.justice.digital.hmpps.supportadditionalneedsapi.domain.entity.BaseAuditableEntity
import java.time.LocalDate
import java.util.UUID

@Repository
interface PrisonActivitySummaryRepository : JpaRepository<BaseAuditableEntity, UUID> {

  companion object {
    private const val PRISON_ACTIVITY_SUMMARY_QUERY = """
      WITH aln AS (
          SELECT created_at_prison,
                 COUNT(DISTINCT reference) AS aln_screener
          FROM aln_screener
          WHERE created_at is not null
            AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
          GROUP BY created_at_prison
      ),
           elsp AS (
               SELECT created_at_prison,
                      COUNT(DISTINCT reference) AS elsp_plan
               FROM elsp_plan
               WHERE created_at is not null
                 AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
               GROUP BY created_at_prison
           ),
           ch AS (
               SELECT created_at_prison,
                      COUNT (
                              DISTINCT case
                                           when aln_screener_id IS NULL then reference ELSE NULL
                          END
                      ) as challenge
               FROM challenge
               WHERE created_at is not null
                 AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
               GROUP BY created_at_prison
           ),
           cond AS (
               SELECT created_at_prison,
                      COUNT(DISTINCT reference) AS condition
               FROM condition
               WHERE created_at is not null
                 AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
               GROUP BY created_at_prison
           ),
           str AS (
               SELECT created_at_prison,
                      COUNT (
                              DISTINCT case
                                           when aln_screener_id IS NULL then reference ELSE NULL
                          END
                      ) as strength
               FROM strength
               WHERE created_at is not null
                 AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
               GROUP BY created_at_prison
           ),
           supp AS (
               SELECT created_at_prison,
                      COUNT(DISTINCT reference) AS support_strategy
               FROM support_strategy
               WHERE created_at is not null
                 AND CAST(created_at AS date) BETWEEN :fromDate AND :toDate
               GROUP BY created_at_prison
           )
      SELECT COALESCE(
                     aln.created_at_prison,
                     elsp.created_at_prison,
                     ch.created_at_prison,
                     cond.created_at_prison,
                     str.created_at_prison,
                     supp.created_at_prison
             ) AS created_at_prison,
             COALESCE(aln.aln_screener, 0) AS aln_screener,
             COALESCE(elsp.elsp_plan, 0) AS elsp_plan,
             COALESCE(ch.challenge, 0) AS challenge,
             COALESCE(cond.condition, 0) AS condition,
             COALESCE(str.strength, 0) AS strength,
             COALESCE(supp.support_strategy, 0) AS support_strategy
      FROM aln
               FULL OUTER JOIN elsp ON aln.created_at_prison = elsp.created_at_prison
               FULL OUTER JOIN ch ON COALESCE(aln.created_at_prison, elsp.created_at_prison) = ch.created_at_prison
               FULL OUTER JOIN cond ON COALESCE(
                                               aln.created_at_prison,
                                               elsp.created_at_prison,
                                               ch.created_at_prison
                                       ) = cond.created_at_prison
               FULL OUTER JOIN str ON COALESCE(
                                              aln.created_at_prison,
                                              elsp.created_at_prison,
                                              ch.created_at_prison,
                                              cond.created_at_prison
                                      ) = str.created_at_prison
               FULL OUTER JOIN supp ON COALESCE(
                                               aln.created_at_prison,
                                               elsp.created_at_prison,
                                               ch.created_at_prison,
                                               cond.created_at_prison,
                                               str.created_at_prison
                                       ) = supp.created_at_prison
      WHERE COALESCE(aln.created_at_prison, elsp.created_at_prison, ch.created_at_prison, cond.created_at_prison, str.created_at_prison, supp.created_at_prison) IS NOT NULL
        AND COALESCE(
                     aln.created_at_prison,
                     elsp.created_at_prison,
                     ch.created_at_prison,
                     cond.created_at_prison,
                     str.created_at_prison,
                     supp.created_at_prison
             ) NOT IN (
                                      'ACI', 'DGI', 'FWI', 'FBI', 'PBI', 'RHI', 'WNI', 'BWI', 'BZI', 'PRI',
                                      'UKI', 'WYI', 'ASI', 'PFI', 'UPI', 'SWI', 'PYI', 'CFI', 'CKI', 'FYI',
                                      'FEI', 'LGI', 'MKI'
          )
      ORDER BY created_at_prison asc
    """
  }

  @Query(
    nativeQuery = true,
    value = PRISON_ACTIVITY_SUMMARY_QUERY,
  )
  fun findPrisonActivitySummary(
    @Param("fromDate") fromDate: LocalDate,
    @Param("toDate") toDate: LocalDate,
  ): List<PrisonActivitySummaryResult>
}

interface PrisonActivitySummaryResult {
  val createdAtPrison: String
  val alnScreener: Long
  val elspPlan: Long
  val challenge: Long
  val condition: Long
  val strength: Long
  val supportStrategy: Long
}

WITH old_screeners AS (
    SELECT s.id
    FROM aln_screener s
    WHERE EXISTS (
        SELECT 1
        FROM aln_screener moreThan1
        WHERE moreThan1.prison_number = s.prison_number
        GROUP BY moreThan1.prison_number
        HAVING COUNT(*) >= 2
    )
      AND EXISTS (
        SELECT 1
        FROM aln_screener newer
        WHERE newer.prison_number = s.prison_number
          AND (
            newer.screening_date > s.screening_date
                OR (newer.screening_date = s.screening_date AND newer.updated_at > s.updated_at)
            )
    )
),
     updated_challenge AS (
         UPDATE challenge c
             SET active = false,
                 archive_reason = 'Old screener'
             FROM old_screeners os
             WHERE c.aln_screener_id = os.id
                 AND c.active is true
             RETURNING c.id AS record_id, 'challenge' AS source
     ),
     updated_strength AS (
         UPDATE strength s
             SET active = false,
                 archive_reason = 'Old screener'
             FROM old_screeners os
             WHERE s.aln_screener_id = os.id
                 AND s.active is true
             RETURNING s.id AS record_id, 'strength' AS source
     )
SELECT * FROM updated_challenge
UNION ALL
SELECT * FROM updated_strength;
CREATE OR REPLACE VIEW prisoner_overview AS
WITH all_prisoners AS (
    SELECT prison_number FROM aln_assessment
    UNION
    SELECT prison_number FROM ldd_assessment
    UNION
    SELECT prison_number FROM education
    UNION
    SELECT prison_number FROM condition
    UNION
    SELECT prison_number FROM challenge
    UNION
    SELECT prison_number FROM strength
    UNION
    SELECT prison_number FROM plan_creation_schedule
    UNION
    SELECT prison_number FROM review_schedule
    UNION
    SELECT prison_number FROM elsp_plan
)
SELECT
    ap.prison_number,

    -- Latest ALN assessment
    COALESCE(aln.has_need, false) AS has_aln_need,

    -- Latest LDD assessment
    COALESCE(ldd.has_need, false) AS has_ldd_need,

    -- Latest education record
    COALESCE(edu.in_education, false) AS in_education,

    -- Any active condition
    COALESCE(cond.exists IS NOT NULL, false) AS has_condition,

    -- Any active non-screener challenge
    COALESCE(chal.exists IS NOT NULL, false) AS has_non_screener_challenge,

    -- Any active non-screener strength
    COALESCE(str.exists IS NOT NULL, false) AS has_non_screener_strength,

    -- Plan creation deadline
    pcs.deadline_date AS plan_creation_deadline_date,

    -- Latest scheduled review deadline
    rsched.deadline_date AS review_deadline_date,

    -- Combined deadline (review > plan creation > null)
    COALESCE(rsched.deadline_date, pcs.deadline_date) AS deadline_date,

    -- Has a plan in elsp_plan
    COALESCE(plan.exists IS NOT NULL, false) AS has_plan,

    -- Plan declined if status is EXEMPT_PRISONER_NOT_COMPLY
    COALESCE(pcs.status = 'EXEMPT_PRISONER_NOT_COMPLY', false) AS plan_declined,

    -- has need flag
    (
        COALESCE(aln.has_need, false)
            OR COALESCE(ldd.has_need, false)
            OR COALESCE(cond.exists IS NOT NULL, false)
            OR COALESCE(chal.exists IS NOT NULL, false)
        ) AS has_need

FROM all_prisoners ap

-- Latest ALN assessment
         LEFT JOIN LATERAL (
    SELECT has_need
    FROM aln_assessment
    WHERE prison_number = ap.prison_number
    ORDER BY created_at DESC
    LIMIT 1
    ) aln ON true

-- Latest LDD assessment
         LEFT JOIN LATERAL (
    SELECT has_need
    FROM ldd_assessment
    WHERE prison_number = ap.prison_number
    ORDER BY created_at DESC
    LIMIT 1
    ) ldd ON true

-- Latest education record
         LEFT JOIN LATERAL (
    SELECT in_education
    FROM education
    WHERE prison_number = ap.prison_number
    ORDER BY created_at DESC
    LIMIT 1
    ) edu ON true

-- Any active condition
         LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM condition
    WHERE prison_number = ap.prison_number AND active = true
    LIMIT 1
    ) cond ON true

-- Any active non-screener challenge
         LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM challenge
    WHERE prison_number = ap.prison_number AND active = true AND aln_screener_id IS NULL
    LIMIT 1
    ) chal ON true

-- Any active non-screener strength
         LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM strength
    WHERE prison_number = ap.prison_number AND active = true AND aln_screener_id IS NULL
    LIMIT 1
    ) str ON true

-- Plan creation deadline (and status)
         LEFT JOIN plan_creation_schedule pcs
                   ON pcs.prison_number = ap.prison_number

-- Latest scheduled review deadline
         LEFT JOIN LATERAL (
    SELECT deadline_date
    FROM review_schedule
    WHERE prison_number = ap.prison_number AND status = 'SCHEDULED'
    ORDER BY deadline_date DESC
    LIMIT 1
    ) rsched ON true

-- Existence of a plan
         LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM elsp_plan
    WHERE prison_number = ap.prison_number
    LIMIT 1
    ) plan ON true;

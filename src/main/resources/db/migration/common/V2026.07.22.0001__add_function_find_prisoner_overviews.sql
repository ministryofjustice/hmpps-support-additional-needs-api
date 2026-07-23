CREATE OR REPLACE FUNCTION find_prisoner_overviews(
    p_prison_numbers text[]
)
RETURNS TABLE (
    prison_number text,
    has_aln_need boolean,
    has_ldd_need boolean,
    in_education boolean,
    has_condition boolean,
    has_non_screener_challenge boolean,
    has_non_screener_strength boolean,
    plan_creation_deadline_date date,
    review_deadline_date date,
    review_status text,
    deadline_date date,
    has_plan boolean,
    plan_declined boolean,
    has_need boolean
)
AS $$
WITH all_selected_prisoners AS (
	SELECT prison_number FROM unnest(p_prison_numbers) ap(prison_number)
    WHERE EXISTS (
    	SELECT 1 FROM aln_assessment where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM ldd_assessment where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM education_enrolment where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM "condition" where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM challenge where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM strength where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM plan_creation_schedule where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM review_schedule where prison_number = ap.prison_number
	    UNION ALL
	    SELECT 1 FROM elsp_plan WHERE prison_number = ap.prison_number
    )
)
SELECT
    ap.prison_number,

    -- Latest ALN assessment
    aln.has_need AS has_aln_need,

    -- Latest LDD assessment
    ldd.has_need AS has_ldd_need,

    -- Currently enrolled in education: enrolment started today or earlier AND no end date
    COALESCE(edu_enrol.exists IS NOT NULL, false) AS in_education,

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

    -- Latest scheduled review status
    rsched.status AS review_status,

    -- Combined deadline (review > plan creation > null)
    COALESCE(rsched.deadline_date, pcs.deadline_date) AS deadline_date,

    -- Has a plan in elsp_plan
    COALESCE(plan.exists IS NOT NULL, false) AS has_plan,

    -- Plan declined if status is EXEMPT_PRISONER_NOT_COMPLY
    COALESCE(pcs.status = 'EXEMPT_PRISONER_NOT_COMPLY', false) AS plan_declined,

    -- has need flag
    (
        CASE
            WHEN aln.has_need IS NOT NULL THEN aln.has_need
            WHEN ldd.has_need IS NOT NULL THEN ldd.has_need
            ELSE FALSE
            END
            OR COALESCE(cond.exists IS NOT NULL, false)
            OR COALESCE(chal.exists IS NOT NULL, false)
        ) AS has_need

FROM all_selected_prisoners ap

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

-- Currently in education (via education_enrolment)
    LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM education_enrolment
    WHERE prison_number = ap.prison_number
    AND learning_start_date <= CURRENT_DATE
    AND end_date IS NULL
    LIMIT 1
    ) edu_enrol ON true

-- Any active condition
    LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM "condition"
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
    LEFT JOIN LATERAL (
    SELECT deadline_date, status
    FROM plan_creation_schedule
    WHERE prison_number = ap.prison_number
    ORDER BY deadline_date DESC, created_at DESC NULLS LAST
    LIMIT 1
    ) pcs ON true

-- Latest scheduled review deadline
    LEFT JOIN LATERAL (
    SELECT deadline_date, status
    FROM review_schedule
    WHERE prison_number = ap.prison_number AND (status = 'SCHEDULED' OR status = 'EXEMPT_UNKNOWN')
    ORDER BY deadline_date DESC
    LIMIT 1
    ) rsched ON true

-- Existence of a plan
    LEFT JOIN LATERAL (
    SELECT 1 AS exists
    FROM elsp_plan
    WHERE prison_number = ap.prison_number
    LIMIT 1
    ) plan ON true
$$ LANGUAGE sql;

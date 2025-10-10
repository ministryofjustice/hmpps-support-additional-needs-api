CREATE INDEX IF NOT EXISTS aln_assessment_prison_created_idx
    ON aln_assessment (prison_number, created_at DESC);

CREATE INDEX IF NOT EXISTS education_enrolment_active_idx
    ON education_enrolment (prison_number, end_date) WHERE end_date IS NULL;

CREATE INDEX IF NOT EXISTS condition_active_idx
    ON "condition" (prison_number) WHERE active = true;

CREATE INDEX IF NOT EXISTS challenge_active_idx
    ON challenge (prison_number) WHERE active = true AND aln_screener_id IS NULL;

CREATE INDEX IF NOT EXISTS strength_active_idx
    ON strength (prison_number) WHERE active = true AND aln_screener_id IS NULL;

CREATE INDEX IF NOT EXISTS plan_creation_schedule_deadline_idx
    ON plan_creation_schedule (prison_number, deadline_date DESC, created_at DESC);

CREATE INDEX IF NOT EXISTS review_schedule_scheduled_idx
    ON review_schedule (prison_number, status, deadline_date DESC);



DROP VIEW IF EXISTS prisoner_overview;

CREATE OR REPLACE VIEW prisoner_overview AS
WITH all_prisoners AS (
    SELECT DISTINCT prison_number FROM (
    SELECT prison_number FROM aln_assessment
    UNION
    SELECT prison_number FROM ldd_assessment
    UNION
    SELECT prison_number FROM education_enrolment
    UNION
    SELECT prison_number FROM "condition"
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
    ) apu
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

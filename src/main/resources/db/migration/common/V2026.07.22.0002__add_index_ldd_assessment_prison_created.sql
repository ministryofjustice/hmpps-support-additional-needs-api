CREATE INDEX ldd_assessment_prison_created_idx
    ON ldd_assessment (prison_number, created_at DESC);

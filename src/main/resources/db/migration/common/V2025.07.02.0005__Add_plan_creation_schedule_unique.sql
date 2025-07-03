DROP INDEX IF EXISTS plan_creation_schedule_one_scheduled_per_prison_number_idx;

-- UNIQUE constraint on prison_number since a prisoner can only have one plan creation schedule
ALTER TABLE plan_creation_schedule
    ADD CONSTRAINT unique_prison_number
        UNIQUE (prison_number);
ALTER TABLE plan_creation_schedule
    DROP COLUMN need_sources;

ALTER TABLE plan_creation_schedule_history
    DROP COLUMN need_sources;

ALTER TABLE plan_creation_schedule
    ADD COLUMN need_sources VARCHAR[];

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN need_sources VARCHAR[];
ALTER TABLE plan_creation_schedule
    ADD COLUMN need_sources VARCHAR(255);

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN need_sources VARCHAR(255);
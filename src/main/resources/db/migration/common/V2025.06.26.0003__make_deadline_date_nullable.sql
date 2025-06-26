ALTER TABLE plan_creation_schedule
    ALTER COLUMN deadline_date DROP NOT NULL;

ALTER TABLE plan_creation_schedule
    ADD COLUMN exemption_detail TEXT;

ALTER TABLE plan_creation_schedule_history
    ALTER COLUMN deadline_date DROP NOT NULL;

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN exemption_detail TEXT;
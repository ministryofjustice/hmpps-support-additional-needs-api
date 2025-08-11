ALTER TABLE review_schedule
    ALTER COLUMN deadline_date DROP NOT NULL;

ALTER TABLE review_schedule_history
    ALTER COLUMN deadline_date DROP NOT NULL;


ALTER TABLE plan_creation_schedule
    ADD COLUMN earliest_start_date date NULL;

ALTER TABLE plan_creation_schedule_history
    ADD COLUMN earliest_start_date date NULL;